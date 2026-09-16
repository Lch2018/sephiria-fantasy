# Build the mod and install the jar into the PCL dev instance's mods folder.
#
# Usage: powershell -NoProfile -ExecutionPolicy Bypass -File tools\deploy.ps1
#
# The PCL instance is the one the user actually tests in, so every build has to land
# there, not just in build\libs.

$root = Split-Path -Parent $PSScriptRoot
$mods = 'D:\PCL\.minecraft\versions\26.2-Fabric 0.19.5\mods'

if (-not (Test-Path $mods)) { throw "mods folder not found: $mods" }

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

$jar = Join-Path $root 'build\libs\sephiria-0.1.0.jar'
if (-not (Test-Path $jar)) { throw "built jar not found: $jar" }

$target = Join-Path $mods 'sephiria-0.1.0.jar'
Copy-Item $jar $target -Force

$info = Get-Item $target
Write-Output ("deployed " + $info.Length + " B  " + $info.LastWriteTime.ToString('HH:mm:ss') + "  -> " + $target)
