# Muscu – suivi de musculation (Android 13+)

Application Android (Kotlin, Jetpack Compose, Room) pour suivre ses séances semaine par semaine (lundi → dimanche).

## Fonctionnalités

- **Ma semaine** : navigation par semaine (n° ISO), sélection du jour, coche verte sur les jours entraînés, liste des exercices du jour avec la performance précédente.
- **Ajouter à la séance** : choix de l'exercice, séries / répétitions / poids (pas de 0,25 kg, appui prolongé sur + pour augmenter le poids en continu jusqu'au relâchement, saisie libre possible), **pré-remplissage automatique avec la dernière valeur saisie pour cet exercice**. Un appui sur une entrée permet de la modifier ou la supprimer.
- **Mes exercices** : bibliothèque avec recherche (insensible aux accents), dernière performance et ancienneté.
- **Nouvel exercice** : nom, couleur, photo depuis la galerie ou l'appareil photo ; modification et suppression.
- Données stockées localement (base Room `muscu.db`), aucune connexion requise.

## Structure

```
app/src/main/java/fr/muscu/app/
├── data/        Entités Room, DAO, base, dépôt, formatage
├── ui/week/     Écran « Ma semaine » + feuille « Nouvelle entrée »
├── ui/exercises/ Bibliothèque + création/édition d'exercice
├── ui/theme/    Couleurs et typographie
└── MainActivity.kt  Navigation + barre du bas
.github/workflows/build-apk.yml  Compilation de l'APK sur GitHub
```

---

## Générer l'APK avec GitHub Actions – pas à pas

### 1. Créer le dépôt GitHub
1. Sur <https://github.com/new>, nommez le dépôt (ex. `MuscuAndroid`), choisissez **Private** si vous le souhaitez, **ne cochez aucune** option d'initialisation (README, .gitignore…), puis **Create repository**.

### 2. Envoyer le code
Dans un terminal ouvert dans le dossier du projet :
```powershell
git init
git add .
git commit -m "Application Muscu"
git branch -M main
git remote add origin https://github.com/<VOTRE_COMPTE>/MuscuAndroid.git
git push -u origin main
```
(Ou dans IntelliJ : *VCS → Share Project on GitHub*.)

### 3. (Recommandé) Ajouter une clé de signature permanente
Sans cette étape, l'APK est signé avec une clé temporaire différente à chaque compilation : pour installer une nouvelle version, il faudrait **désinstaller l'application et perdre ses données**. À faire une seule fois :

1. Générer la clé (gardez précieusement le fichier et les mots de passe) :
   ```powershell
   keytool -genkeypair -v -keystore muscu.jks -alias muscu -keyalg RSA -keysize 2048 -validity 10000
   ```
   (`keytool` se trouve dans le dossier `bin` de votre JDK.)
2. Convertir le fichier en texte Base64 :
   ```powershell
   [Convert]::ToBase64String([IO.File]::ReadAllBytes("muscu.jks")) | Set-Clipboard
   ```
3. Sur GitHub : **Settings → Secrets and variables → Actions → New repository secret**, créer 4 secrets :

   | Nom | Valeur |
   |---|---|
   | `KEYSTORE_BASE64` | le texte copié à l'étape 2 |
   | `SIGNING_STORE_PASSWORD` | mot de passe du keystore |
   | `SIGNING_KEY_ALIAS` | `muscu` |
   | `SIGNING_KEY_PASSWORD` | mot de passe de la clé (souvent le même) |

   Le fichier `muscu.jks` ne doit **pas** être envoyé sur GitHub (il est exclu par `.gitignore`).

### 4. Lancer la compilation
- La compilation démarre automatiquement à chaque `git push` sur `main`.
- Pour la relancer manuellement : onglet **Actions → Build APK → Run workflow**.
- Si Actions est désactivé : onglet **Actions → « I understand my workflows, go ahead and enable them »**.

### 5. Télécharger l'APK
1. Onglet **Actions** → cliquer sur la dernière exécution (coche verte, ~5 min).
2. En bas de la page, section **Artifacts** → **muscu-apk** (fichier `.zip` contenant `muscu-N.apk`).

Variante : créer un tag pour obtenir l'APK dans une *Release* téléchargeable directement depuis le téléphone :
```powershell
git tag v1.0
git push origin v1.0
```
→ onglet **Releases** du dépôt.

### 6. Installer sur le téléphone
1. Transférer `muscu-N.apk` sur le téléphone (câble, Drive, mail…) et l'ouvrir.
2. Autoriser « Installer des applications inconnues » pour l'application utilisée (Fichiers, Chrome…).
3. Si Play Protect affiche un avertissement : **Plus de détails → Installer quand même**.

Les mises à jour suivantes s'installent par-dessus (données conservées) si la clé de l'étape 3 est configurée.

## Compiler localement (optionnel)
Ouvrir le dossier dans Android Studio (JDK 17, SDK 34) puis *Build → Build APK(s)*, ou avec Gradle 8.9 : `gradle assembleRelease`.
