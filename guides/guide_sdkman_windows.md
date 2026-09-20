**SDKMAN!** (Software Development Kit Manager) est un outil pour gérer plusieurs versions de kits de développement (Java, Gradle, Maven, Spring Boot, Kotlin, etc.).

Originellement conçu pour les systèmes Unix (Linux, macOS), SDKMAN! s'utilise sur Windows via **WSL (Windows Subsystem for Linux)** ou un environnement Bash comme **Git Bash / MSYS2 / Cygwin**.

Ce guide couvre la méthode recommandée sous Windows : **Git Bash** (ou WSL).

## 1. Prerequisites / Prérequis (Git Bash)

SDKMAN! nécessite `bash`, `zip`, `unzip` et `curl`.

1. **Installez Git pour Windows** (si ce n'est pas déjà fait) : [git-scm.com](https://git-scm.com/?utm_source=gemini)

2. **Installez Zip et Unzip dans Git Bash** :
   Par défaut, Git Bash n'inclut pas `zip` et `unzip`.

   * Vous pouvez exécuter Git Bash en mode administrateur.

   * Ou utiliser le gestionnaire de paquets [Chocolatey](https://chocolatey.org/?utm_source=gemini) depuis PowerShell :

     ```bash
     choco install zip unzip curl
     ```

## 2. Installation de SDKMAN!

### Étape 1 : Ouvrir l'environnement Bash

Ouvrez votre terminal **Git Bash** (ou votre terminal WSL).

### Étape 2 : Exécuter le script d'installation

Lancez la commande suivante :

```bash
curl -s "https://get.sdkman.io" | bash
```

### Étape 3 : Initialiser SDKMAN!

Une fois l'installation terminée, suivez les instructions à l'écran ou exécutez :

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
```

### Étape 4 : Vérifier l'installation

Pour vérifier que tout fonctionne correctement, affichez la version de SDKMAN! :

```bash
sdk version
```

## 3. Commandes les Plus Utilisées

### Recherche et Gestion des SDKs (Java, Gradle, Maven, etc.)

#### 1. Lister les candidats (outils disponibles)

Pour voir tous les outils gérables avec SDKMAN! (Java, Kotlin, Scala, Gradle, Maven, Spring Boot, etc.) :

```bash
sdk list
```

#### 2. Lister les versions d'un outil spécifique

Pour voir toutes les versions disponibles pour un outil (exemple avec Java) :

```bash
sdk list java
```

### Installation de SDKs

#### 1. Installer la dernière version stable d'un SDK

```bash
sdk install java
sdk install gradle
```

#### 2. Installer une version spécifique

```bash
# Exemple : Installer Java OpenJDK 17.0.8-tem (Eclipse Temurin)
sdk install java 17.0.8-tem

# Exemple : Installer Gradle 8.3
sdk install gradle 8.3
```

#### 3. Installer une version locale (déjà présente sur votre PC)

```bash
sdk install java my-custom-11 /c/Java/jdk-11
```

### Gestion des Versions Actives

#### 1. Changer de version pour la session courante uniquement

Permet d'utiliser une version spécifique dans le terminal actuel :

```bash
sdk use java 17.0.8-tem
```

#### 2. Définir la version par défaut (globale)

Définit la version par défaut à chaque ouverture de terminal :

```bash
sdk default java 21-open
```

#### 3. Voir la version actuellement utilisée

```bash
sdk current java
# Ou pour tous les SDKs :
sdk current
```

### ❌ Désinstallation et Mise à jour

#### 1. Désinstaller une version d'un SDK

```bash
sdk uninstall java 11.0.2-open
```

#### 2. Mettre à jour SDKMAN! lui-même

```bash
sdk selfupdate
```

#### 3. Mettre à jour la liste des paquets et versions disponibles

```bash
sdk update
```

## 4. Astuce : Configuration Automatique par Projet (`.sdkmanrc`)

Vous pouvez fixer la version d'un SDK pour un projet spécifique :

1. À la racine de votre projet, créez un fichier de configuration :

   ```bash
   sdk env init
   ```

   *Un fichier `.sdkmanrc` est créé (ex: `java=17.0.8-tem`).*

2. À chaque fois que vous entrez dans le dossier du projet, basculez automatiquement sur la bonne version :

   ```bash
   sdk env
   ```

## 5. Résolution des problèmes fréquents sous Windows

* **Erreur `zip` ou `unzip` manquant** :
  Assurez-vous que `zip` et `unzip` sont accessibles dans votre PATH Windows/Bash.

* **Accès aux variables d'environnement dans l'invite de commande Windows (CMD / PowerShell)** :
  SDKMAN! modifie le `PATH` dans l'environnement Bash (`~/.bashrc` ou `~/.bash_profile`). Si vous souhaitez utiliser les JDK installés depuis PowerShell ou CMD, vous devrez pointer manuellement vos variables d'environnement `JAVA_HOME` vers le dossier :
  `C:\Users\<VotreNom>\.sdkman\candidates\java\current`