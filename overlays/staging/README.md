# Staging Overlay

Cet overlay remplace les images locales par les images publiees dans GitHub Container Registry.
Il cible un vrai cluster distant accessible depuis GitHub Actions, par exemple K3s sur OVH.

```bash
kubectl apply -k overlays/staging
```

Par defaut, il utilise le tag `latest`. Pour deployer un commit precis, modifier `newTag` avec le SHA pousse par GitHub Actions.

Cet overlay ne deploie pas PostgreSQL, Kafka, Schema Registry ou l'observabilite. Ces dependances doivent etre fournies par l'environnement cible, ou par un overlay dedie au cluster.

## Deploiement depuis GitHub Actions

Le workflow `Deploy staging` attend un vrai cluster accessible depuis GitHub Actions. Un kubeconfig Docker Desktop local ne fonctionne pas dans un runner GitHub, car `kubernetes.docker.internal` existe seulement sur ta machine.

Pour un cluster distant, configurer `KUBE_CONFIG_STAGING`, puis lancer :

```powershell
.\scripts\deploy-staging.ps1 -ImageTag latest
```

## Deploiement local

Pour Docker Desktop, utiliser l'overlay local :

```powershell
.\scripts\deploy-local.ps1 -ImageTag latest
```
