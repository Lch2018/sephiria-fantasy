# Generate the SEPHIRIA "electric spark" sound: a short crackling zap ("crackle"), used by the
# Shock debuff instead of the long BEE_LOOP buzz (which overlapped into a constant drone).
#
# The sound is synthesized straight to WAV (PCM16 mono) and then encoded to .ogg with the
# ffmpeg binary that @ffmpeg-installer/ffmpeg ships inside the npm tarball (no github download):
#   cd /tmp/audio-tools && npm install @ffmpeg-installer/ffmpeg
#
# Design (what makes it read as electricity rather than noise):
#   * a dozen random short bursts of white noise with a sharp attack and an exponential decay -
#     that is the "crackle-spark" cadence (gaps between bursts matter as much as the bursts);
#   * each burst is one-pole high-passed (sample - 0.5 * previous), which tilts it toward the
#     bright sizzle end and away from a "wind/steam" rumble;
#   * a very quiet high-passed sizzle floor underneath keeps the channel hissing instead of
#     dropping to digital silence between sparks;
#   * three variants so repeated hits never sound sample-identical (sounds.json picks randomly).
#
# NOTE: keep this file ASCII - Windows PowerShell 5.1 reads .ps1 as GBK otherwise.

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$soundDir = Join-Path $root 'src\main\resources\assets\sephiria_fantasy\sounds'
New-Item -ItemType Directory -Force -Path $soundDir | Out-Null

function Write-Wav([string]$path, [double[]]$samples, [int]$rate) {
    $count = $samples.Count
    $bytes = New-Object 'System.IO.MemoryStream'
    $writer = New-Object System.IO.BinaryWriter $bytes

    # 44-byte canonical PCM WAV header, mono, 16-bit
    $writer.Write([byte[]](0x52, 0x49, 0x46, 0x46))                     # "RIFF"
    $writer.Write([int](36 + $count * 2))
    $writer.Write([byte[]](0x57, 0x41, 0x56, 0x45))                     # "WAVE"
    $writer.Write([byte[]](0x66, 0x6D, 0x74, 0x20))                     # "fmt "
    $writer.Write([int]16)                                              # fmt chunk size
    $writer.Write([int16]1)                                             # PCM
    $writer.Write([int16]1)                                             # mono
    $writer.Write([int]$rate)
    $writer.Write([int]($rate * 2))
    $writer.Write([int16]2)                                             # block align
    $writer.Write([int16]16)                                            # bits per sample
    $writer.Write([byte[]](0x64, 0x61, 0x74, 0x61))                     # "data"
    $writer.Write([int]($count * 2))

    foreach ($v in $samples) {
        $clamped = [Math]::Max(-1.0, [Math]::Min(1.0, $v))
        $writer.Write([int16][Math]::Round($clamped * 32767.0))
    }

    $writer.Flush()
    [System.IO.File]::WriteAllBytes($path, $bytes.ToArray())
    $writer.Dispose()
    $bytes.Dispose()
}

function Make-Spark([int]$seed, [double]$seedShift) {
    $rate = 32000
    $count = [int]($rate * (0.30 + $seedShift * 0.10))
    $samples = New-Object 'double[]' $count
    $rng = New-Object System.Random $seed

    # two loud "snaps" so the crackle has something to lead with
    $sparks = @()
    for ($i = 0; $i -lt 12; $i++) {
        $loud = ($i -eq 0 -or $i -eq 5 -or $rng.NextDouble() -lt 0.25)
        $sparks += ,@{
            at = [int]($rng.NextDouble() * ($count - 200) * 0.95)
            len = [int](($rate * (0.006 + $rng.NextDouble() * 0.022)) + $seedShift)
            amp = if ($loud) { 1.0 } else { 0.3 + $rng.NextDouble() * 0.5 }
            decay = 3.0 + $rng.NextDouble() * 5.0
        }
    }

    foreach ($s in $sparks) {
        $prev = 0.0
        for ($i = 0; $i -lt $s.len; $i++) {
            $idx = $s.at + $i
            if ($idx -ge $count) { break }
            $noise = $rng.NextDouble() * 2.0 - 1.0
            $hiss = $noise - 0.5 * $prev   # one-pole high-pass: the sizzle lives up top
            $prev = $noise
            $env = [Math]::Exp(-$s.decay * $i / [Math]::Max(1, $s.len))
            $samples[$idx] += $s.amp * $hiss * $env
        }
    }

    # quiet hiss bed so the gaps between sparks still sound "live"
    $prev = 0.0
    for ($i = 0; $i -lt $count; $i++) {
        $noise = $rng.NextDouble() * 2.0 - 1.0
        $samples[$i] += 0.06 * ($noise - 0.5 * $prev)
        $prev = $noise
    }

    # normalise to a fixed peak so the three variants play at a comparable loudness
    $peak = 0.0
    foreach ($v in $samples) { $abs = [Math]::Abs($v); if ($abs -gt $peak) { $peak = $abs } }
    if ($peak -gt 0.0) {
        $gain = 0.85 / $peak
        for ($i = 0; $i -lt $count; $i++) { $samples[$i] = $samples[$i] * $gain }
    }

    return ,$samples
}

$ffmpeg = 'C:\Users\28237\AppData\Local\Temp\audio-tools\node_modules\@ffmpeg-installer\win32-x64\ffmpeg.exe'
if (-not (Test-Path $ffmpeg)) { throw "ffmpeg not found: $ffmpeg (npm install @ffmpeg-installer/ffmpeg in the temp dir first)" }

$variants = @('electric_spark_1', 'electric_spark_2', 'electric_spark_3')
$shift = 0.0
$seed = 7919
$rate = 32000

foreach ($name in $variants) {
    $wavTmp = Join-Path $env:TEMP ($name + '.wav')
    $samples = Make-Spark $seed $shift
    Write-Wav $wavTmp $samples $rate
    & $ffmpeg -y -loglevel error -i $wavTmp `
        -codec:a libvorbis -qscale:a 5 (Join-Path $soundDir ($name + '.ogg'))
    if ($LASTEXITCODE -ne 0) { throw "ffmpeg failed for $name" }
    Remove-Item $wavTmp
    $shift += 0.5
    $seed += 104729
    Write-Output "$name encoded"
}

Write-Output 'done -> assets/sephiria_fantasy/sounds/'
