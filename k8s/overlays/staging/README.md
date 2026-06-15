# Staging Overlay

Cet overlay remplace les images locales par les images publiees dans GitHub Container Registry.

```bash
kubectl apply -k k8s/overlays/staging
```

Par defaut, il utilise le tag `latest`. Pour deployer un commit precis, modifier `newTag` avec le SHA pousse par GitHub Actions.

## Deploiement depuis GitHub Actions

Le workflow `Deploy staging` attend un secret GitHub nomme `KUBE_CONFIG_STAGING`.

Encoder le kubeconfig en base64 :

```bash
base64 -w0 ~/.kube/config
```

Puis lancer le workflow avec `image_tag=latest` ou avec le SHA du commit publie par la CI.
