param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$ApiKey = "local-dev-key",
    [string]$SchemaRegistryUrl = "",
    [int]$SettleSeconds = 10
)

$ErrorActionPreference = "Stop"

function Invoke-JsonPost {
    param(
        [string]$Uri,
        [string]$Body
    )

    Invoke-RestMethod -Method Post -Uri $Uri -Headers @{ "X-API-Key" = $ApiKey } -ContentType "application/json" -Body $Body
}

function Assert-HttpOk {
    param(
        [string]$Uri,
        [switch]$UseApiKey
    )

    $headers = @{}
    if ($UseApiKey) {
        $headers["X-API-Key"] = $ApiKey
    }

    $response = Invoke-WebRequest -Uri $Uri -Headers $headers -UseBasicParsing
    if ($response.StatusCode -lt 200 -or $response.StatusCode -ge 300) {
        throw "Expected HTTP 2xx from $Uri but got $($response.StatusCode)"
    }
}

Write-Host "Checking API Gateway health..."
Assert-HttpOk "$BaseUrl/actuator/health/readiness"

Write-Host "Sending successful order..."
Invoke-JsonPost "$BaseUrl/api/orders" '{"orderId":"smoke-success-1","productName":"MacBook Pro","quantity":1,"amount":250,"customerEmail":"client@test.com"}'

Write-Host "Sending duplicate order..."
Invoke-JsonPost "$BaseUrl/api/orders" '{"orderId":"smoke-success-1","productName":"MacBook Pro","quantity":1,"amount":250,"customerEmail":"client@test.com"}'

Write-Host "Sending payment failure order..."
Invoke-JsonPost "$BaseUrl/api/orders" '{"orderId":"smoke-payment-failed-1","productName":"MacBook Pro","quantity":1,"amount":999,"customerEmail":"client@test.com"}'

Write-Host "Sending inventory compensation order..."
Invoke-JsonPost "$BaseUrl/api/orders" '{"orderId":"fail-inventory-smoke-1","productName":"MacBook Pro","quantity":1,"amount":250,"customerEmail":"client@test.com"}'

Start-Sleep -Seconds $SettleSeconds

Write-Host "Checking materialized order read model..."
Assert-HttpOk "$BaseUrl/api/orders/smoke-success-1" -UseApiKey
Assert-HttpOk "$BaseUrl/api/orders/smoke-success-1/events" -UseApiKey

if ($SchemaRegistryUrl) {
    Write-Host "Checking Schema Registry subjects..."
    $subjects = Invoke-RestMethod -Uri "$SchemaRegistryUrl/subjects"
    $requiredSubjects = @(
        "order-created-value",
        "payment-processed-value",
        "payment-failed-value",
        "payment-refunded-value",
        "inventory-updated-value",
        "inventory-failed-value"
    )

    foreach ($subject in $requiredSubjects) {
        if ($subjects -notcontains $subject) {
            throw "Missing schema subject: $subject"
        }
    }
}

Write-Host "Smoke test completed successfully."
