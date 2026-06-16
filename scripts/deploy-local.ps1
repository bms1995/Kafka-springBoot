param(
    [string]$ImageTag = "latest"
)

$ErrorActionPreference = "Stop"

if (-not (Get-Command kubectl -ErrorAction SilentlyContinue)) {
    throw "kubectl is required for local deployment."
}

$context = kubectl config current-context
if ($context -ne "docker-desktop") {
    throw "Refusing local deployment on Kubernetes context '$context'. Switch to docker-desktop or use the staging workflow."
}

Write-Host "Deploying local Kubernetes stack with image_tag=$ImageTag..."
kubectl apply -k overlays/local --validate=false

kubectl -n kafka-spring set image deployment/api-gateway api-gateway=ghcr.io/bms1995/kafka-springboot/api-gateway:$ImageTag
kubectl -n kafka-spring set image deployment/order-service order-service=ghcr.io/bms1995/kafka-springboot/order-service:$ImageTag
kubectl -n kafka-spring set image deployment/payment-service payment-service=ghcr.io/bms1995/kafka-springboot/payment-service:$ImageTag
kubectl -n kafka-spring set image deployment/inventory-service inventory-service=ghcr.io/bms1995/kafka-springboot/inventory-service:$ImageTag
kubectl -n kafka-spring set image deployment/notification-service notification-service=ghcr.io/bms1995/kafka-springboot/notification-service:$ImageTag

kubectl -n kafka-spring rollout status deployment/postgres-order --timeout=180s
kubectl -n kafka-spring rollout status deployment/postgres-payment --timeout=180s
kubectl -n kafka-spring rollout status deployment/postgres-inventory --timeout=180s
kubectl -n kafka-spring rollout status deployment/kafka --timeout=240s
kubectl -n kafka-spring rollout status deployment/order-service --timeout=240s
kubectl -n kafka-spring rollout status deployment/payment-service --timeout=240s
kubectl -n kafka-spring rollout status deployment/inventory-service --timeout=240s
kubectl -n kafka-spring rollout status deployment/notification-service --timeout=240s
kubectl -n kafka-spring rollout status deployment/api-gateway --timeout=240s

Write-Host "Local Kubernetes deployment completed."
