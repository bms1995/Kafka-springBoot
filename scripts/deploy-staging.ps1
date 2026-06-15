param(
    [string]$ImageTag = "latest",
    [string]$Repository = "bms1995/Kafka-springBoot",
    [switch]$GitHubActions
)

$ErrorActionPreference = "Stop"

if ($GitHubActions) {
    if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
        throw "GitHub CLI 'gh' is required. Install it, then run: gh auth login"
    }

    Write-Host "Triggering Deploy staging workflow with image_tag=$ImageTag..."
    gh workflow run "Deploy staging" --repo $Repository -f image_tag=$ImageTag

    Write-Host "Deployment workflow triggered."
    Write-Host "Open runs with: gh run list --repo $Repository --workflow `"Deploy staging`""
    return
}

if (-not (Get-Command kubectl -ErrorAction SilentlyContinue)) {
    throw "kubectl is required for local staging deployment."
}

Write-Host "Deploying staging locally with image_tag=$ImageTag..."
kubectl apply -k overlays/staging

kubectl -n kafka-spring set image deployment/api-gateway api-gateway=ghcr.io/bms1995/kafka-springboot/api-gateway:$ImageTag
kubectl -n kafka-spring set image deployment/order-service order-service=ghcr.io/bms1995/kafka-springboot/order-service:$ImageTag
kubectl -n kafka-spring set image deployment/payment-service payment-service=ghcr.io/bms1995/kafka-springboot/payment-service:$ImageTag
kubectl -n kafka-spring set image deployment/inventory-service inventory-service=ghcr.io/bms1995/kafka-springboot/inventory-service:$ImageTag
kubectl -n kafka-spring set image deployment/notification-service notification-service=ghcr.io/bms1995/kafka-springboot/notification-service:$ImageTag

kubectl -n kafka-spring rollout status deployment/api-gateway --timeout=180s
kubectl -n kafka-spring rollout status deployment/order-service --timeout=180s
kubectl -n kafka-spring rollout status deployment/payment-service --timeout=180s
kubectl -n kafka-spring rollout status deployment/inventory-service --timeout=180s
kubectl -n kafka-spring rollout status deployment/notification-service --timeout=180s

Write-Host "Local staging deployment completed."
