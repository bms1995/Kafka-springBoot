$ErrorActionPreference = "Stop"

$requiredMetadataFields = @(
    "eventId",
    "correlationId",
    "causationId",
    "occurredAt",
    "producer",
    "schemaVersion"
)

$schemas = Get-ChildItem -Path . -Recurse -Filter *.avsc |
    Where-Object { $_.FullName -notmatch "\\target\\" }

if (-not $schemas) {
    throw "No Avro schemas found."
}

$schemasByName = @{}
$topicSubjects = @{
    "OrderCreatedEvent" = "order-created-value"
    "PaymentProcessedEvent" = "payment-processed-value"
    "PaymentFailedEvent" = "payment-failed-value"
    "PaymentRefundedEvent" = "payment-refunded-value"
    "InventoryUpdatedEvent" = "inventory-updated-value"
    "InventoryFailedEvent" = "inventory-failed-value"
}

foreach ($schemaFile in $schemas) {
    $schema = Get-Content -Raw -Path $schemaFile.FullName | ConvertFrom-Json
    $fieldNames = @($schema.fields | ForEach-Object { $_.name })

    foreach ($requiredField in $requiredMetadataFields) {
        if ($fieldNames -notcontains $requiredField) {
            throw "$($schemaFile.FullName) is missing required metadata field '$requiredField'."
        }
    }

    if (-not $topicSubjects.ContainsKey($schema.name)) {
        throw "$($schemaFile.FullName) has no declared topic subject mapping for schema '$($schema.name)'."
    }

    foreach ($field in $schema.fields) {
        $fieldTypeJson = $field.type | ConvertTo-Json -Depth 20 -Compress
        $isNullableUnion = $fieldTypeJson -match '"null"'
        if ($isNullableUnion -and -not ($field.PSObject.Properties.Name -contains "default")) {
            throw "$($schemaFile.FullName) nullable field '$($field.name)' must declare an Avro default for compatibility."
        }
    }

    $canonicalSchema = $schema | ConvertTo-Json -Depth 20 -Compress

    if (-not $schemasByName.ContainsKey($schema.name)) {
        $schemasByName[$schema.name] = @{
            Path = $schemaFile.FullName
            Canonical = $canonicalSchema
        }
        continue
    }

    if ($schemasByName[$schema.name].Canonical -ne $canonicalSchema) {
        throw "Schema '$($schema.name)' differs between '$($schemasByName[$schema.name].Path)' and '$($schemaFile.FullName)'."
    }
}

Write-Host "Validated $($schemas.Count) Avro schema files across $($schemasByName.Count) event contracts and $($topicSubjects.Count) topic subjects."
