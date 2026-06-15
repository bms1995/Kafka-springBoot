# Staging Overlay

Cet overlay remplace les images locales par les images publiees dans GitHub Container Registry.

```bash
kubectl apply -k overlays/staging
```

Par defaut, il utilise le tag `latest`. Pour deployer un commit precis, modifier `newTag` avec le SHA pousse par GitHub Actions.

## Deploiement local Docker Desktop

Pour Docker Desktop Kubernetes, deployer depuis ta machine, pas depuis GitHub Actions :

```powershell
.\scripts\deploy-staging.ps1 -ImageTag latest
```

## Deploiement depuis GitHub Actions

Le workflow `Deploy staging` attend un vrai cluster accessible depuis GitHub Actions. Un kubeconfig Docker Desktop local ne fonctionne pas dans un runner GitHub, car `kubernetes.docker.internal` existe seulement sur ta machine.

Pour un cluster distant, configurer `KUBE_CONFIG_STAGING`, puis lancer :

```powershell
.\scripts\deploy-staging.ps1 -ImageTag latest -GitHubActions
```
