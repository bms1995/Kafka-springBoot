param(
    [string]$BaseUrl = "http://localhost:8081"
)

$ErrorActionPreference = "Stop"

function Invoke-JsonPost {
    param(
        [string]$Uri,
        [string]$Body
    )

    Invoke-RestMethod -Method Post -Uri $Uri -ContentType "application/json" -Body $Body
}

function Assert-HttpOk {
    param([string]$Uri)

    $response = Invoke-WebRequest -Uri $Uri -UseBasicParsing
    if ($response.StatusCode -lt 200 -or $response.StatusCode -ge 300) {
        throw "Expected HTTP 2xx from $Uri but got $($response.StatusCode)"
    }
}

Write-Host "Checking service health endpoints..."
Assert-HttpOk "http://localhost:8081/actuator/health"
Assert-HttpOk "http://localhost:8082/actuator/health"
Assert-HttpOk "http://localhost:8083/actuator/health"
Assert-HttpOk "http://localhost:8084/actuator/health"

Write-Host "Sending successful order..."
Invoke-JsonPost "$BaseUrl/api/orders" '{"orderId":"smoke-success-1","productName":"MacBook Pro","quantity":1,"amount":250,"customerEmail":"client@test.com"}'

Write-Host "Sending duplicate order..."
Invoke-JsonPost "$BaseUrl/api/orders" '{"orderId":"smoke-success-1","productName":"MacBook Pro","quantity":1,"amount":250,"customerEmail":"client@test.com"}'

Write-Host "Sending payment failure order..."
Invoke-JsonPost "$BaseUrl/api/orders" '{"orderId":"smoke-payment-failed-1","productName":"MacBook Pro","quantity":1,"amount":999,"customerEmail":"client@test.com"}'

Write-Host "Sending inventory compensation order..."
Invoke-JsonPost "$BaseUrl/api/orders" '{"orderId":"fail-inventory-smoke-1","productName":"MacBook Pro","quantity":1,"amount":250,"customerEmail":"client@test.com"}'

Start-Sleep -Seconds 5

Write-Host "Checking Schema Registry subjects..."
$subjects = Invoke-RestMethod -Uri "http://localhost:8086/subjects"
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

Write-Host "Smoke test completed successfully."
