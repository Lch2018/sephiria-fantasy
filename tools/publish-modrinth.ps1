# 把当前版本发到 Modrinth（新建项目 + 上传版本），供「发布」时使用。
#
# Usage: powershell -NoProfile -ExecutionPolicy Bypass -File tools\publish-modrinth.ps1
#
# 需要一份 Modrinth 个人访问令牌（PAT）——网页 https://modrinth.com/settings/pats 新建，
# 勾上 PROJECT_CREATE 与 VERSION_CREATE 两个权限。令牌按下面的顺序找：
#   1) -Token 参数；2) 环境变量 MODRINTH_TOKEN；3) %USERPROFILE%\.sephiria-modrinth-token（首行）。
# 令牌只在本地使用，绝不要写进仓库（那个文件也刻意放在仓库外）。
#
# 走本地代理：api.modrinth.com 直连不通，和 git 一样用 127.0.0.1:7897。

param(
    [string]$Token = '',
    [string]$Proxy = 'http://127.0.0.1:7897',
    [string]$Slug = 'sephiria',
    [switch]$SkipProjectCheck
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot

# ---- 令牌 ----
if (-not $Token) { $Token = $env:MODRINTH_TOKEN }

if (-not $Token) {
    $tokenPath = Join-Path $env:USERPROFILE '.sephiria-modrinth-token'

    if (Test-Path $tokenPath) {
        $Token = (Get-Content -Path $tokenPath -Encoding UTF8 | Select-Object -First 1).Trim()
    }
}

if (-not $Token) {
    throw "找不到 Modrinth 令牌。请在 https://modrinth.com/settings/pats 建一个（勾 PROJECT_CREATE + VERSION_CREATE），把令牌粘进 $env:USERPROFILE\.sephiria-modrinth-token，或设环境变量 MODRINTH_TOKEN。"
}

# ---- 版本号与产物（与 deploy.ps1 同一处来源）----
$version = (Select-String -Path (Join-Path $root 'gradle.properties') -Pattern '^version=' |
    Select-Object -First 1).Line.Split('=')[1].Trim()
$jar = Join-Path $root ("build\libs\sephiria-$version.jar")

if (-not (Test-Path $jar)) {
    throw "没找到构建产物 $jar —— 先跑 tools\deploy.ps1。"
}

$description = Join-Path $PSScriptRoot 'modrinth\description.md'
$changelogPath = Join-Path $PSScriptRoot 'modrinth\changelog.md'

foreach ($file in @($description, $changelogPath)) {
    if (-not (Test-Path $file)) { throw "缺文件：$file" }
}

# curl.exe 自带在 Windows 10+ 上；PowerShell 5.1 的 Invoke-RestMethod 做 multipart 太别扭。
curl.exe --version | Out-Null
if ($LASTEXITCODE -ne 0) { throw '没找到 curl.exe' }

$api = 'https://api.modrinth.com/v2'
$userAgent = 'SEPHIRIA-Mod-Publisher/1.0 (github.com/Lch2018/sephiria)'
$tempDir = Join-Path ([System.IO.Path]::GetTempPath()) ("sephiria-modrinth-" + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $tempDir | Out-Null

# 拼 JSON 用自写的小序列化器，**不要**改成 ConvertTo-Json：本机 PowerShell 5.1 碰到
# description.md 那份中文正文会卡死（实测进程内存涨到 7 GB 也不返回），换成自写的就没这问题。
# 只需要转义反斜杠、引号与控制字符，其余（含中文）原样输出——curl 按 UTF-8 发，API 也按 UTF-8 收。
function ConvertTo-JsonText([string]$text) {
    $builder = New-Object System.Text.StringBuilder

    foreach ($ch in $text.ToCharArray()) {
        switch ($ch) {
            '"' { [void]$builder.Append('\"') }
            '\' { [void]$builder.Append('\\') }
            "`n" { [void]$builder.Append('\n') }
            "`r" { [void]$builder.Append('\r') }
            "`t" { [void]$builder.Append('\t') }
            default {
                if ([int]$ch -lt 0x20) { [void]$builder.Append('\u' + ([int]$ch).ToString('x4')) }
                else { [void]$builder.Append($ch) }
            }
        }
    }

    return $builder.ToString()
}

# 够用即可：哈希表 / 数组 / 字符串 / 布尔 / 数字。注意 [string] 也算 IEnumerable，
# 所以字符串那一支必须排在数组前面。
function ConvertTo-JsonValue($value) {
    if ($null -eq $value) { return 'null' }
    if ($value -is [string]) { return '"' + (ConvertTo-JsonText $value) + '"' }
    if ($value -is [bool]) { return $value.ToString().ToLowerInvariant() }
    if ($value -is [int] -or $value -is [long] -or $value -is [double]) {
        return $value.ToString([System.Globalization.CultureInfo]::InvariantCulture)
    }
    if ($value -is [System.Collections.IDictionary]) {
        $parts = @()
        foreach ($key in $value.Keys) { $parts += ('"' + $key + '": ' + (ConvertTo-JsonValue $value[$key])) }
        return '{' + ($parts -join ', ') + '}'
    }
    if ($value -is [System.Collections.IEnumerable]) {
        $parts = @()
        foreach ($item in $value) { $parts += (ConvertTo-JsonValue $item) }
        return '[' + ($parts -join ', ') + ']'
    }

    return '"' + (ConvertTo-JsonText $value.ToString()) + '"'
}

# 中文写成不带 BOM 的 UTF-8：带 BOM 的 JSON 会被 API 拒掉。
function Write-JsonFile([string]$path, $payload) {
    $utf8 = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($path, (ConvertTo-JsonValue $payload), $utf8)
}

# 统一发请求：返回 @{ status = <码>; body = <文本> }
function Invoke-Api {
    param(
        [string]$Method,
        [string]$Path,
        [string[]]$ExtraArgs = @(),
        [string]$BodyFile = ''
    )

    $out = Join-Path $tempDir 'response.json'
    # 注意别用 $args：那是 PowerShell 的自动变量
    $curlArgs = @('-sS', '-x', $Proxy, '-A', $userAgent, '-X', $Method, '-H', "Authorization: Bearer $Token",
        '-o', $out, '-w', '%{http_code}', "$api$Path")

    if ($BodyFile) { $curlArgs += @('--data-binary', "@$BodyFile") }
    if ($ExtraArgs.Count -gt 0) { $curlArgs += $ExtraArgs }

    $status = & curl.exe @curlArgs
    $body = if (Test-Path $out) { Get-Content -Path $out -Raw -Encoding UTF8 } else { '' }

    if (Test-Path $out) { Remove-Item $out -Force }

    return @{ status = [int]$status; body = $body }
}

try {
    $project = $null

    if (-not $SkipProjectCheck) {
        Write-Output "查询项目 $Slug …"
        $existing = Invoke-Api -Method 'GET' -Path "/project/$Slug"

        if ($existing.status -eq 200) {
            $project = $existing.body | ConvertFrom-Json
            Write-Output ("项目已存在：" + $project.slug + "（id " + $project.id + "）")
        } elseif ($existing.status -ne 404) {
            throw "查询项目失败：HTTP $($existing.status) $($existing.body)"
        }
    }

    # 项目资料（正文 + 摘要 + 分类 + 环境 + 许可证 + 链接）：新建时用一次，之后每次发布再刷新一遍
    $projectPayload = @{
        title         = 'SEPHIRIA'
        # 摘要中英双语（Modrinth 上限 256 字符，这段 254）
        description   = '把《SEPHIRIA》的武器系统带进 Minecraft 的同人模组：六把武器、十套连击、55 件神器、减益与神器技能。大量使用 AI 生成代码，仅供学习交流，禁止商业行为。 Unofficial fan mod: SEPHIRIA''s weapons in Minecraft — 6 weapons, 10 combos, 55 artifacts, debuffs and artifact skills. Heavy use of AI-generated code; non-commercial.'
        body          = (Get-Content -Path $description -Raw -Encoding UTF8)
        # 分类上限就是 3 个：给 4 个 API 会回 `field categories failed validation with error: length`
        categories    = @('adventure', 'equipment', 'game-mechanics')
        client_side   = 'required'
        server_side   = 'required'
        license_id    = 'CC-BY-NC-4.0'   # 字段名就是 license_id（字符串），不是嵌套的 license 对象
        source_url    = 'https://github.com/Lch2018/sephiria'
        issues_url    = 'https://github.com/Lch2018/sephiria/issues'
    }

    if (-not $project) {
        Write-Output '新建项目 …'

        $payload = $projectPayload + @{
            slug             = $Slug
            project_type     = 'mod'
            game_versions    = @('26.3')
            loaders          = @('fabric')
            is_draft         = $false
            initial_versions = @()   # 必填字段：建项目时本可顺手带一个版本，这里留空、随后单独传
        }

        $bodyFile = Join-Path $tempDir 'project.json'
        Write-JsonFile -path $bodyFile -payload $payload

        # 建项目这条是 multipart + data 字段（和上传版本同一套）——发成 application/json
        # 会被 API 回一句 "Error while parsing multipart payload: ContentTypeIncompatible"。
        $created = Invoke-Api -Method 'POST' -Path '/project' -ExtraArgs @('-F', "data=<$bodyFile")

        if ($created.status -ne 200 -and $created.status -ne 201) {
            throw "建项目失败：HTTP $($created.status) $($created.body)"
        }

        $project = $created.body | ConvertFrom-Json
        Write-Output ("项目已建立：" + $project.slug + "（id " + $project.id + "）")
    } else {
        # 项目已经在了：把正文与资料按本地文件刷新一遍 —— 改了 tools/modrinth/description.md，
        # 下次发布就会同步到项目页。注意 PATCH 收 application/json，POST 却要 multipart，两个口径不一样。
        Write-Output '刷新项目资料 …'

        $patchFile = Join-Path $tempDir 'project-patch.json'
        Write-JsonFile -path $patchFile -payload $projectPayload

        $patched = Invoke-Api -Method 'PATCH' -Path "/project/$($project.id)" -BodyFile $patchFile `
            -ExtraArgs @('-H', 'Content-Type: application/json')

        if ($patched.status -ne 204 -and $patched.status -ne 200) {
            throw "刷新项目资料失败：HTTP $($patched.status) $($patched.body)"
        }

        Write-Output '项目资料已刷新'
    }

    # 同一个版本号已经传过就跳过：Modrinth **不拦**重复的 version_number，重复上传会在项目页多出
    # 一个一模一样的版本（得按 id 删掉）。要重传同一个版本号，先去后台把旧的那条删了再跑。
    $existingVersions = Invoke-Api -Method 'GET' -Path "/project/$($project.id)/version"

    if ($existingVersions.status -eq 200) {
        $already = ($existingVersions.body | ConvertFrom-Json) |
            Where-Object { $_.version_number -eq $version } | Select-Object -First 1

        if ($already) {
            Write-Output "项目里已经有 $version 这个版本（id $($already.id)），跳过上传。"

            # 顺手把更新说明刷新成 tools/modrinth/changelog.md 的内容：改了说明重跑一次就同步了
            $changelogFile = Join-Path $tempDir 'version-patch.json'
            Write-JsonFile -path $changelogFile -payload @{
                changelog = (Get-Content -Path $changelogPath -Raw -Encoding UTF8)
            }

            $refreshed = Invoke-Api -Method 'PATCH' -Path "/version/$($already.id)" -BodyFile $changelogFile `
                -ExtraArgs @('-H', 'Content-Type: application/json')

            if ($refreshed.status -eq 204 -or $refreshed.status -eq 200) {
                Write-Output '更新说明已刷新'
            } else {
                Write-Output "更新说明刷新失败（HTTP $($refreshed.status)）：$($refreshed.body)"
            }

            Write-Output ('项目页： https://modrinth.com/mod/' + $project.slug)
            return
        }
    }

    Write-Output "上传版本 $version …"

    $versionPayload = @{
        project_id     = $project.id
        name           = "$version（A 测）"
        version_number = $version
        changelog      = (Get-Content -Path $changelogPath -Raw -Encoding UTF8)
        dependencies   = @(
            @{ project_id = 'P7dR8mSH'; dependency_type = 'required' }   # Fabric API
            @{ project_id = '8BmcQJ2H'; dependency_type = 'required' }   # GeckoLib
        )
        game_versions  = @('26.3')
        version_type   = 'alpha'
        loaders        = @('fabric')
        featured       = $true
        status         = 'listed'
        # 必填：列出 multipart 里装文件的那几个字段名，要和下面的 -F 名字对上
        file_parts     = @('file')
    }

    $versionFile = Join-Path $tempDir 'version.json'
    Write-JsonFile -path $versionFile -payload $versionPayload

    # multipart：data 字段用「文件内容当值」的 curl 语法（<），file 字段才是真的上传文件
    $uploaded = Invoke-Api -Method 'POST' -Path '/version' -ExtraArgs @(
        '-F', "data=<$versionFile",
        '-F', "file=@$jar;type=application/java-archive"
    )

    if ($uploaded.status -ne 200 -and $uploaded.status -ne 201) {
        throw "上传版本失败：HTTP $($uploaded.status) $($uploaded.body)"
    }

    $result = $uploaded.body | ConvertFrom-Json
    Write-Output ''
    Write-Output ('完成：' + $project.slug + ' → ' + $version)
    Write-Output ('项目页： https://modrinth.com/mod/' + $project.slug)
    Write-Output ('版本页： https://modrinth.com/mod/' + $project.slug + '/version/' + $result.version_number)
} finally {
    Remove-Item -Path $tempDir -Recurse -Force -ErrorAction SilentlyContinue
}
