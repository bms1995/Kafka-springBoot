param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$ApiKey = "local-dev-key",
    [string]$SchemaRegistryUrl = "",
    [int]$SettleSeconds = 10,
    [string]$RunId = ""
)

$ErrorActionPreference = "Stop"

if (-not $RunId) {
    $RunId = Get-Date -Format "yyyyMMddHHmmss"
}

$successOrderId = "smoke-success-$RunId"
$paymentFailedOrderId = "smoke-payment-failed-$RunId"
$inventoryFailedOrderId = "fail-inventory-smoke-$RunId"

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

function Get-Order {
    param(
        [string]$OrderId
    )

    Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/orders/$OrderId" -Headers @{ "X-API-Key" = $ApiKey }
}

function Assert-OrderStatus {
    param(
        [string]$OrderId,
        [string]$ExpectedStatus,
        [int]$TimeoutSeconds = 60,
        [int]$IntervalSeconds = 2
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $lastStatus = "<not read>"

    while ((Get-Date) -lt $deadline) {
        $order = Get-Order $OrderId
        $lastStatus = $order.status
        if ($lastStatus -eq $ExpectedStatus) {
            return
        }

        Start-Sleep -Seconds $IntervalSeconds
    }

    throw "Expected order $OrderId to reach status $ExpectedStatus within $TimeoutSeconds seconds, last status was $lastStatus"
}

function Assert-OrderTimelineContains {
    param(
        [string]$OrderId,
        [string[]]$ExpectedTypes,
        [int]$TimeoutSeconds = 60,
        [int]$IntervalSeconds = 2
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $missing = $ExpectedTypes

    while ((Get-Date) -lt $deadline) {
        $events = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/orders/$OrderId/events" -Headers @{ "X-API-Key" = $ApiKey }
        $eventTypes = @($events | ForEach-Object { $_.eventType })
        $missing = @($ExpectedTypes | Where-Object { $eventTypes -notcontains $_ })

        if ($missing.Count -eq 0) {
            return
        }

        Start-Sleep -Seconds $IntervalSeconds
    }

    throw "Expected order $OrderId timeline to contain [$($ExpectedTypes -join ', ')], missing [$($missing -join ', ')]"
}

Write-Host "Checking API Gateway health..."
Assert-HttpOk "$BaseUrl/actuator/health/readiness"

Write-Host "Sending successful order..."
Invoke-JsonPost "$BaseUrl/api/orders" "{`"orderId`":`"$successOrderId`",`"productName`":`"MacBook Pro`",`"quantity`":1,`"amount`":250,`"customerEmail`":`"client@test.com`"}"

Write-Host "Sending duplicate order..."
Invoke-JsonPost "$BaseUrl/api/orders" "{`"orderId`":`"$successOrderId`",`"productName`":`"MacBook Pro`",`"quantity`":1,`"amount`":250,`"customerEmail`":`"client@test.com`"}"

Write-Host "Sending payment failure order..."
Invoke-JsonPost "$BaseUrl/api/orders" "{`"orderId`":`"$paymentFailedOrderId`",`"productName`":`"MacBook Pro`",`"quantity`":1,`"amount`":999,`"customerEmail`":`"client@test.com`"}"

Write-Host "Sending inventory compensation order..."
Invoke-JsonPost "$BaseUrl/api/orders" "{`"orderId`":`"$inventoryFailedOrderId`",`"productName`":`"MacBook Pro`",`"quantity`":1,`"amount`":250,`"customerEmail`":`"client@test.com`"}"

Start-Sleep -Seconds $SettleSeconds

Write-Host "Checking materialized order read model..."
Assert-OrderStatus $successOrderId "INVENTORY_CONFIRMED"
Assert-OrderStatus $paymentFailedOrderId "PAYMENT_FAILED"
Assert-OrderStatus $inventoryFailedOrderId "REFUNDED"
Assert-HttpOk "$BaseUrl/api/orders/$successOrderId/events" -UseApiKey

Write-Host "Checking Saga timelines..."
Assert-OrderTimelineContains $successOrderId @("PaymentProcessedEvent", "InventoryUpdatedEvent")
Assert-OrderTimelineContains $paymentFailedOrderId @("PaymentFailedEvent")
Assert-OrderTimelineContains $inventoryFailedOrderId @("PaymentProcessedEvent", "InventoryFailedEvent", "PaymentRefundedEvent")

if ($SchemaRegistryUrl) {
    Write-Host "Checking Schema Registry subjects..."
    $subjects = Invoke-RestMethod -Uri "$SchemaRegistryUrl/subjects"

    if ($subjects.Count -gt 0) {
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
    } else {
        Write-Host "Schema Registry is reachable but has no registered subjects; Avro contract validation is covered by scripts/validate-avro-contracts.ps1."
    }
}

Write-Host "Smoke test completed successfully."
