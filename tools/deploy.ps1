# Build the mod and install the jar into the PCL dev instance's mods folder.
#
# Usage: powershell -NoProfile -ExecutionPolicy Bypass -File tools\deploy.ps1
#
# The PCL instance is the one the user actually tests in, so every build has to land
# there, not just in build\libs.
#
# The jar name follows gradle.properties' version (currently 0.1.0-a, the alpha build),
# so it is read from there instead of being written out twice.

$root = Split-Path -Parent $PSScriptRoot
$mods = 'D:\PCL\.minecraft\versions\26.2-Fabric 0.19.5\mods'

if (-not (Test-Path $mods)) { throw "mods folder not found: $mods" }

# jar name = <project name>-<version>.jar, same as gradle builds it
$version = (Select-String -Path (Join-Path $root 'gradle.properties') -Pattern '^version=' |
    Select-Object -First 1).Line.Split('=')[1].Trim()
$name = 'sephiria-' + $version + '.jar'

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
Get-ChildItem $mods -Filter 'sephiria-*.jar' | Where-Object { $_.Name -ne $name } | ForEach-Object {
    Remove-Item $_.FullName -Force
    Write-Output ("removed stale " + $_.Name)
}

$target = Join-Path $mods $name
Copy-Item $jar $target -Force

$info = Get-Item $target
Write-Output ("deployed " + $info.Length + " B  " + $info.LastWriteTime.ToString('HH:mm:ss') + "  -> " + $target)
