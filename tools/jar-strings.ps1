# Dump printable ASCII strings from class entries inside a jar, filtered by a pattern.
#
# Used to inspect GeckoLib internals (the jar ships no sources) when figuring out
# resource-path conventions.
#
# Usage: powershell -NoProfile -ExecutionPolicy Bypass -File tools\jar-strings.ps1 -Jar libs\geckolib-fabric-26.2-5.5.5.jar -Entry com/geckolib/renderer -Filter textures

param(
    [Parameter(Mandatory = $true)][string]$Jar,
    [Parameter(Mandatory = $true)][string]$Entry,
    [string]$Filter = ''
)

Add-Type -AssemblyName System.IO.Compression.FileSystem

$zip = [System.IO.Compression.ZipFile]::OpenRead((Resolve-Path $Jar).Path)
try {
    $classes = $zip.Entries | Where-Object { $_.FullName -like ($Entry + '*') -and $_.FullName -like '*.class' }
    Write-Output ("entries: " + $classes.Count)
    foreach ($c in $classes) {
        $stream = $c.Open()
        $ms = New-Object System.IO.MemoryStream
        $stream.CopyTo($ms)
        $stream.Dispose()
        $bytes = $ms.ToArray()
        $ms.Dispose()

        $sb = New-Object System.Text.StringBuilder
        $found = New-Object System.Collections.Generic.List[string]
        foreach ($b in $bytes) {
            if ($b -ge 32 -and $b -lt 127) {
                [void]$sb.Append([char]$b)
            }
            else {
                if ($sb.Length -ge 5) {
                    $s = $sb.ToString()
                    if ($Filter -eq '' -or $s -like ('*' + $Filter + '*')) { $found.Add($s) }
                }
                [void]$sb.Clear()
            }
        }

        if ($found.Count -gt 0) {
            Write-Output ('--- ' + $c.FullName)
            foreach ($f in $found) { Write-Output ('    ' + $f) }
        }
    }
}
finally {
    $zip.Dispose()
}
