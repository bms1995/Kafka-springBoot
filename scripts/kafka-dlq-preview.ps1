param(
    [Parameter(Mandatory = $true)]
    [string]$Topic,
    [int]$MaxMessages = 10,
    [string]$BootstrapServer = "localhost:29092",
    [string]$KafkaContainer = "kafka1"
)

$ErrorActionPreference = "Stop"

if ($Topic -notmatch "\.DLQ$") {
    throw "Refusing to preview non-DLQ topic '$Topic'. Use a topic ending with .DLQ."
}

Write-Host "Previewing up to $MaxMessages messages from $Topic"
docker exec $KafkaContainer /opt/bitnami/kafka/bin/kafka-console-consumer.sh `
    --bootstrap-server $BootstrapServer `
    --topic $Topic `
    --from-beginning `
    --timeout-ms 10000 `
    --max-messages $MaxMessages `
    --property print.key=true `
    --property print.headers=true `
    --property print.timestamp=true
