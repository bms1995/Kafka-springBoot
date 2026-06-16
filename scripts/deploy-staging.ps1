param(
    [string]$ImageTag = "latest",
    [string]$Repository = "bms1995/Kafka-springBoot",
    [string]$SmokeBaseUrl = ""
)

$ErrorActionPreference = "Stop"

if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
    throw "GitHub CLI 'gh' is required. Install it, then run: gh auth login"
}

Write-Host "Triggering Deploy staging workflow with image_tag=$ImageTag..."
$args = @(
    "workflow",
    "run",
    "Deploy staging",
    "--repo",
    $Repository,
    "-f",
    "image_tag=$ImageTag"
)

if ($SmokeBaseUrl) {
    $args += @("-f", "smoke_base_url=$SmokeBaseUrl")
}

gh @args

Write-Host "Deployment workflow triggered."
Write-Host "Open runs with: gh run list --repo $Repository --workflow `"Deploy staging`""
