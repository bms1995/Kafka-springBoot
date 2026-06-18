param(
    [string]$BootstrapServer = "localhost:29092",
    [string]$KafkaContainer = "kafka1"
)

$ErrorActionPreference = "Stop"

function Invoke-KafkaTool {
    param(
        [string[]]$Args
    )

    docker exec $KafkaContainer @Args
}

Write-Host "Topics:"
Invoke-KafkaTool @("/opt/bitnami/kafka/bin/kafka-topics.sh", "--bootstrap-server", $BootstrapServer, "--list")

Write-Host ""
Write-Host "Topic descriptions:"
Invoke-KafkaTool @("/opt/bitnami/kafka/bin/kafka-topics.sh", "--bootstrap-server", $BootstrapServer, "--describe")

Write-Host ""
Write-Host "Consumer groups:"
Invoke-KafkaTool @("/opt/bitnami/kafka/bin/kafka-consumer-groups.sh", "--bootstrap-server", $BootstrapServer, "--list")

Write-Host ""
Write-Host "Consumer group lag:"
$groups = docker exec $KafkaContainer /opt/bitnami/kafka/bin/kafka-consumer-groups.sh --bootstrap-server $BootstrapServer --list
foreach ($group in $groups) {
    if ($group) {
        Write-Host ""
        Write-Host "Group: $group"
        Invoke-KafkaTool @("/opt/bitnami/kafka/bin/kafka-consumer-groups.sh", "--bootstrap-server", $BootstrapServer, "--describe", "--group", $group)
    }
}
