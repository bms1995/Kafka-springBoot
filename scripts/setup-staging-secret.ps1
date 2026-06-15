param(
    [string]$KubeConfigPath = "$env:USERPROFILE\.kube\config",
    [string]$Repository = "bms1995/Kafka-springBoot"
)

$ErrorActionPreference = "Stop"

if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
    throw "GitHub CLI 'gh' is required. Install it, then run: gh auth login"
}

if (-not (Test-Path $KubeConfigPath)) {
    throw "Kubeconfig not found: $KubeConfigPath"
}

Write-Host "Encoding kubeconfig from $KubeConfigPath..."
$bytes = [System.IO.File]::ReadAllBytes((Resolve-Path $KubeConfigPath))
$encoded = [Convert]::ToBase64String($bytes)

Write-Host "Saving GitHub secret KUBE_CONFIG_STAGING in $Repository..."
$encoded | gh secret set KUBE_CONFIG_STAGING --repo $Repository

Write-Host "KUBE_CONFIG_STAGING configured successfully."
