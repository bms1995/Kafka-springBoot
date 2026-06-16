param(
    [string]$KubeConfigPath = "$env:USERPROFILE\.kube\config",
    [string]$Repository = "bms1995/Kafka-springBoot",
    [switch]$AllowLocalDockerDesktop
)

$ErrorActionPreference = "Stop"

if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
    throw "GitHub CLI 'gh' is required. Install it, then run: gh auth login"
}

if (-not (Test-Path $KubeConfigPath)) {
    throw "Kubeconfig not found: $KubeConfigPath"
}

$kubeConfig = Get-Content -Raw -Path $KubeConfigPath
if (-not $AllowLocalDockerDesktop -and $kubeConfig -match "kubernetes\.docker\.internal") {
    throw @"
This kubeconfig points to Docker Desktop (kubernetes.docker.internal), which only works on your local machine.
GitHub Actions needs a kubeconfig for a cluster reachable from the GitHub-hosted runner.

For local Docker Desktop staging, run:
  .\scripts\deploy-staging.ps1 -ImageTag latest

For GitHub Actions staging, use a remote cluster kubeconfig or a self-hosted runner that can reach your cluster.
"@
}

Write-Host "Encoding kubeconfig from $KubeConfigPath..."
$bytes = [System.IO.File]::ReadAllBytes((Resolve-Path $KubeConfigPath))
$encoded = [Convert]::ToBase64String($bytes)

Write-Host "Saving GitHub secret KUBE_CONFIG_STAGING in $Repository..."
$encoded | gh secret set KUBE_CONFIG_STAGING --repo $Repository

Write-Host "KUBE_CONFIG_STAGING configured successfully."
