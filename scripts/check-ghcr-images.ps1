param(
    [string]$Owner = "bms1995",
    [string]$PackagePrefix = "kafka-springboot"
)

$ErrorActionPreference = "Stop"

if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
    throw "GitHub CLI 'gh' is required. Install it, then run: gh auth login"
}

$services = @(
    "api-gateway",
    "order-service",
    "payment-service",
    "inventory-service",
    "notification-service"
)

foreach ($service in $services) {
    $packageName = "$PackagePrefix/$service"
    Write-Host "Checking GHCR package: $packageName"
    gh api "/users/$Owner/packages/container/$([uri]::EscapeDataString($packageName))/versions" `
        --jq '.[0] | {id: .id, tags: .metadata.container.tags, updated_at: .updated_at}'
}
