# Guide Jour J — Passer du runner local au vrai VPS

Ce guide part de ton état actuel : le pipeline `deploy.yml` (self-hosted) fonctionne déjà sur ta machine, et `deploy_vps.yml` est déjà commité mais inactif (secrets manquants). Suis les étapes dans l'ordre le jour où tu as ton VPS.

---

## 0. Avant de commencer

⚠️ **Note importante sur l'état actuel du repo** : `deploy_vps.yml` se déclenche déjà sur chaque `push` vers `master`, en parallèle de `deploy.yml`, et échoue tant que les secrets ne sont pas configurés. C'est sans danger (ça ne casse rien) mais ça pollue l'onglet Actions. Si ça te gêne en attendant le Jour J, deux options :
- Ignorer les échecs (le plus simple, aucun impact réel)
- Ou renommer temporairement `deploy_vps.yml` en `deploy_vps.yml.disabled` et le remettre le Jour J

---

## 1. Créer le VPS

Suis la procédure Oracle Cloud Free Tier (ou l'équivalent chez ton fournisseur) :
1. Créer le compte / se connecter
2. Créer une instance de calcul (shape `VM.Standard.E2.1.Micro` ou ARM Ampere si disponible)
3. Choisir l'image **Ubuntu 22.04 ou 24.04**
4. **Générer une nouvelle paire de clés SSH lors de la création** (ne pas réutiliser une clé existante) — télécharge la clé privée immédiatement, elle ne sera plus récupérable après
5. Ouvrir les ports nécessaires dans la Security List / Network Security Group : au minimum le port **22** (SSH), et **80/443** si l'app doit être accessible depuis l'extérieur

**Note tes infos ici une fois le VPS créé :**
- IP publique : `_______________`
- Nom d'utilisateur SSH par défaut : `_______________` (souvent `ubuntu` pour une image Ubuntu, à vérifier selon le fournisseur)

---

## 2. Se connecter et préparer le VPS

```bash
ssh -i chemin/vers/ta_cle.pem NOM_UTILISATEUR@IP_DU_VPS
```

Une fois connecté, installe Docker :

```bash
sudo apt update
sudo apt install -y docker.io docker-compose-plugin git
sudo systemctl enable docker
sudo systemctl start docker
sudo usermod -aG docker $USER
```

⚠️ Après `usermod`, déconnecte-toi et reconnecte-toi en SSH pour que le changement de groupe prenne effet (sinon il faudra `sudo` devant chaque commande docker).

---

## 3. Cloner le dépôt une première fois sur le VPS

```bash
cd ~
git clone https://github.com/KudzoNelsam/vps_deploy.git
cd vps_deploy
```

Note le chemin absolu complet, tu en auras besoin pour le secret `DEPLOY_PATH` :

```bash
pwd
```

→ Résultat attendu du type `/home/ubuntu/vps_deploy`

---

## 4. Générer une clé SSH dédiée au déploiement

Cette clé est **différente** de celle utilisée pour te connecter manuellement — elle sera utilisée uniquement par GitHub Actions.

Sur ta machine locale (pas sur le VPS) :

```bash
ssh-keygen -t ed25519 -f ~/deploy_key -N ""
```

Ça génère deux fichiers : `deploy_key` (privée) et `deploy_key.pub` (publique).

Copie la clé publique sur le VPS :

```bash
ssh-copy-id -i ~/deploy_key.pub NOM_UTILISATEUR@IP_DU_VPS
```

Si `ssh-copy-id` n'est pas disponible, fais-le manuellement :

```bash
cat ~/deploy_key.pub | ssh NOM_UTILISATEUR@IP_DU_VPS "mkdir -p ~/.ssh && cat >> ~/.ssh/authorized_keys"
```

Vérifie que la connexion fonctionne avec cette nouvelle clé :

```bash
ssh -i ~/deploy_key NOM_UTILISATEUR@IP_DU_VPS
```

---

## 5. Ajouter les secrets dans GitHub

Va sur : `github.com/KudzoNelsam/vps_deploy/settings/secrets/actions` → **New repository secret**

Crée ces 5 secrets un par un :

| Nom du secret | Valeur |
|---|---|
| `VPS_HOST` | L'IP publique de ton VPS |
| `VPS_USER` | Le nom d'utilisateur SSH (ex. `ubuntu`) |
| `SSH_PRIVATE_KEY` | Le contenu **complet** de `~/deploy_key` (la clé privée, y compris les lignes `-----BEGIN...` et `-----END...`) — utilise `cat ~/deploy_key` pour l'afficher et copie tout |
| `VPS_PORT` | `22` (sauf si tu as changé le port SSH) |
| `DEPLOY_PATH` | Le chemin absolu noté à l'étape 3 (ex. `/home/ubuntu/vps_deploy`) |

⚠️ Ne partage jamais le contenu de `SSH_PRIVATE_KEY` ailleurs que dans ce champ secret GitHub.

---

## 6. Basculer sur le pipeline VPS

Deux workflows existent dans `.github/workflows/` :
- `deploy.yml` → déploiement local (self-hosted runner sur ton PC)
- `deploy_vps.yml` → déploiement sur le VPS distant (celui qu'on utilise maintenant)

Pour éviter que les deux se déclenchent en même temps à chaque push, supprime ou renomme `deploy.yml` :

```bash
git rm .github/workflows/deploy.yml
git commit -m "Bascule vers le déploiement VPS"
git push origin master
```

(Tu peux aussi arrêter le runner local avec Ctrl+C dans le terminal `./run.sh` puisqu'il ne sera plus sollicité.)

---

## 7. Tester le pipeline complet

Fais un petit changement dans le code (ex. modifie le message dans `HelloController.java`), puis :

```bash
git add .
git commit -m "Test déploiement VPS"
git push origin master
```

Va dans l'onglet **Actions** du dépôt GitHub et suis l'exécution du workflow `Deploy to VPS`. Une fois terminé (coche verte), vérifie sur le VPS :

```bash
ssh NOM_UTILISATEUR@IP_DU_VPS
docker ps
curl http://localhost:8080
```

Et depuis l'extérieur (si le port 80/443 est ouvert et configuré) :

```bash
curl http://IP_DU_VPS:8080
```

---

## Dépannage rapide

| Problème | Cause probable | Solution |
|---|---|---|
| `Permission denied (publickey)` dans les logs GitHub Actions | Mauvaise clé privée dans le secret, ou clé publique pas bien copiée sur le VPS | Revérifie l'étape 4, régénère si besoin |
| `docker: command not found` sur le VPS | Docker pas installé ou session SSH pas relancée après `usermod` | Revoir étape 2 |
| Le job reste bloqué en `queued` | Normal pour `runs-on: ubuntu-latest` — GitHub attend un runner disponible, patiente quelques secondes | — |
| `cd: no such file or directory` dans les logs | `DEPLOY_PATH` incorrect | Vérifie avec `pwd` sur le VPS, corrige le secret |
| Connexion SSH refusée depuis GitHub Actions mais fonctionne en manuel | Port SSH bloqué dans la Security List/Firewall du VPS pour le trafic entrant | Vérifie les règles réseau du fournisseur |