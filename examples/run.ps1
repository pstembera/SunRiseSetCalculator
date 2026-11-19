<#
.SYNOPSIS
    Computes sunrise and sunset for a coordinate and date.

.EXAMPLE
    .\run.ps1 -Lat 50.0755 -Lon 14.4378 -Date 2026-06-21 -Tz Europe/Prague

.EXAMPLE
    .\run.ps1 -Lat -33.8688 -Lon 151.2093 -Tz Australia/Sydney
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [double]$Lat,

    [Parameter(Mandatory = $true)]
    [double]$Lon,

    [string]$Date,

    [string]$Tz = "UTC"
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$jar = Join-Path $root "target\sun_rise_set.jar"

if (-not (Test-Path $jar)) {
    throw "Jar not found at $jar. Build it first: .\mvnw.cmd -DskipTests package"
}

$javaArgs = @("-jar", $jar, "--lat", $Lat, "--lon", $Lon, "--tz", $Tz)
if ($Date) {
    $javaArgs += @("--date", $Date)
}

& java @javaArgs
exit $LASTEXITCODE
