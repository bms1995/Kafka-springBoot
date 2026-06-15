# Staging Overlay

Cet overlay remplace les images locales par les images publiees dans GitHub Container Registry.

```bash
kubectl apply -k overlays/staging
```

Par defaut, il utilise le tag `latest`. Pour deployer un commit precis, modifier `newTag` avec le SHA pousse par GitHub Actions.

## Deploiement depuis GitHub Actions

Le workflow `Deploy staging` attend un secret GitHub nomme `KUBE_CONFIG_STAGING`.

Depuis PowerShell, les scripts du repository font ces operations :

```powershell
.\scripts\setup-staging-secret.ps1
.\scripts\deploy-staging.ps1 -ImageTag latest
```
