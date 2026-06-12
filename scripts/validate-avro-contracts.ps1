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

foreach ($schemaFile in $schemas) {
    $schema = Get-Content -Raw -Path $schemaFile.FullName | ConvertFrom-Json
    $fieldNames = @($schema.fields | ForEach-Object { $_.name })

    foreach ($requiredField in $requiredMetadataFields) {
        if ($fieldNames -notcontains $requiredField) {
            throw "$($schemaFile.FullName) is missing required metadata field '$requiredField'."
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

Write-Host "Validated $($schemas.Count) Avro schema files across $($schemasByName.Count) event contracts."
