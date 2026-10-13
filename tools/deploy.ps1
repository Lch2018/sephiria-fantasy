# Build the mod and install the jar into the PCL dev instance's mods folder.
#
# Usage: powershell -NoProfile -ExecutionPolicy Bypass -File tools\deploy.ps1
#
# The PCL instance is the one the user actually tests in, so every build has to land
# there, not just in build\libs.
#
# The jar name follows gradle.properties' version (date-based, e.g. 0.1.10.9-a, the alpha build),
# so it is read from there instead of being written out twice.

param(
    [switch]$Force
)

$root = Split-Path -Parent $PSScriptRoot
$instance = '26.3-Fabric 0.19.5'
$mods = 'D:\PCL\.minecraft\versions\' + $instance + '\mods'

if (-not (Test-Path $mods)) { throw "mods folder not found: $mods" }

# Do not deploy while the game is running. Copy-Item -Force overwrites the jar in place, and the
# loader reads classes lazily, so a game that is already up reads its next class out of the
# replaced file and dies with "ZipFile invalid LOC header (bad signature)" /
# "Failed to load class file for ...". (2026-10-13: that is exactly what crashed the user's
# session - launched 20:58, deployed 21:01, crash 21:02.)
# A file-lock test is not enough: the game keeps no handle open between class loads, so the copy
# would succeed and the corruption would only show up later. Look for the game process instead.
$game = @(Get-CimInstance Win32_Process -Filter "Name = 'javaw.exe' OR Name = 'java.exe'" `
        -ErrorAction SilentlyContinue |
    Where-Object {
        $_.CommandLine -and ($_.CommandLine -like ('*' + $instance + '*') -or
            $_.CommandLine -like '*-Dminecraft.launcher.brand=PCL*')
    })

if ($game.Count -gt 0 -and -not $Force) {
    throw ("Minecraft is still running (" + $game.Count + " java process(es) for '" + $instance +
        "') - close the game first: replacing the jar underneath it makes the running game die " +
        "with 'ZipFile invalid LOC header'. Pass -Force only if that instance truly is not in use.")
}

# jar name = <project name>-<version>.jar, same as gradle builds it
$version = (Select-String -Path (Join-Path $root 'gradle.properties') -Pattern '^version=' |
    Select-Object -First 1).Line.Split('=')[1].Trim()
$name = 'sephiria-fantasy-' + $version + '.jar'

Push-Location $root
try {
    # javac warnings (deprecated API) go to stderr and are not build failures, so don't
    # let PowerShell's error preference abort on them -- check the exit code instead.
    & .\gradlew.bat build --console=plain 2>&1 | ForEach-Object { Write-Output $_ }
    if ($LASTEXITCODE -ne 0) { throw "gradle build failed (exit $LASTEXITCODE)" }
}
finally {
    Pop-Location
}

$jar = Join-Path $root ('build\libs\' + $name)
if (-not (Test-Path $jar)) { throw "built jar not found: $jar" }

# 旧名字的 jar 要清掉：mods 目录里同时存在两个同 mod id 的 jar，进游戏会报重复 mod
Get-ChildItem $mods -Filter 'sephiria*.jar' | Where-Object { $_.Name -ne $name } | ForEach-Object {
    Remove-Item $_.FullName -Force
    Write-Output ("removed stale " + $_.Name)
}

$target = Join-Path $mods $name
Copy-Item $jar $target -Force

# A truncated copy would look "deployed" but crash the game at class-load time (same symptom as
# above), so check the byte count landed.
$copied = Get-Item $target
if ($copied.Length -ne (Get-Item $jar).Length) {
    throw ("copy is incomplete: " + $copied.Length + " B at " + $target + " vs " +
        (Get-Item $jar).Length + " B built - deploy again")
}

Write-Output ("deployed " + $copied.Length + " B  " + $copied.LastWriteTime.ToString('HH:mm:ss') + "  -> " + $target)
