param(
    [Parameter(Mandatory = $true)]
    [string]$Topic,
    [Parameter(Mandatory = $true)]
    [string]$Key,
    [string]$BootstrapServer = "localhost:29092",
    [ValidateRange(1, 100)]
    [int]$MaxMessages = 1,
    [switch]$Execute,
    [string]$Operator = "",
    [string]$Reason = ""
)

$ErrorActionPreference = "Stop"

if ($Topic -notmatch "\.DLQ$") {
    throw "Source topic must end with .DLQ"
}

if ($Execute -and (-not $Operator -or -not $Reason)) {
    throw "-Execute requires -Operator and -Reason"
}

mvn -q -pl common-kafka -am -DskipTests install
if ($LASTEXITCODE -ne 0) {
    throw "Unable to build the DLQ replay tool"
}

$classpathFile = Join-Path $PSScriptRoot "..\common-kafka\target\replay-classpath.txt"
mvn -q -f common-kafka/pom.xml dependency:build-classpath "-Dmdep.outputFile=$classpathFile"
if ($LASTEXITCODE -ne 0) {
    throw "Unable to build the DLQ replay classpath"
}

$classes = Join-Path $PSScriptRoot "..\common-kafka\target\classes"
$classpath = "$classes;$((Get-Content $classpathFile -Raw).Trim())"
$arguments = @(
    "--bootstrap", $BootstrapServer,
    "--source", $Topic,
    "--key", $Key,
    "--max", $MaxMessages
)

if ($Execute) {
    $arguments += @("--execute", "--operator", $Operator, "--reason", $Reason)
}

java -cp $classpath com.example.commonkafka.DlqReplayTool @arguments
if ($LASTEXITCODE -ne 0) {
    throw "DLQ replay tool failed"
}
