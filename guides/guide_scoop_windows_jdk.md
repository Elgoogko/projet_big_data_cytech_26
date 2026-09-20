# Manuel d'Installation et d'Utilisation de Scoop (Windows) - Gestion des JDK

**Scoop** est un gestionnaire de paquets en ligne de commande pour Windows, idéal pour les développeurs. Contrairement aux installeurs traditionnels, il installe les applications dans votre répertoire utilisateur sans afficher de fenêtres contextuelles d'élévation de privilèges (UAC) et permet de basculer très facilement entre différentes versions d'un même outil (comme le JDK Java).

---

## 1. Prérequis et Installation de Scoop

Scoop s'installe via **PowerShell**.

### Étape 1 : Ouvrir PowerShell
Ouvrez PowerShell (pas besoin d'être en mode administrateur).

### Étape 2 : Autoriser l'exécution des scripts
Par défaut, PowerShell bloque l'exécution des scripts. Autorisez l'exécution pour l'utilisateur actuel :

```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

### Étape 3 : Installer Scoop
Exécutez la commande d'installation standard :

```powershell
Invoke-RestMethod -Uri https://get.scoop.sh | Invoke-Expression
```

*(Optionnel)* Pour vérifier que Scoop est bien installé :
```powershell
scoop --version
```

---

## 2. Configuration pour les JDK (Ajout des Buckets)

Par défaut, Scoop inclut un catalogue restreint d'applications (le bucket `main`). Les versions de Java se trouvent principalement dans le dépôt officiel **`java`** et les utilitaires supplémentaires dans **`extras`**.

### Étape 1 : Installer Git (Requis pour ajouter des buckets)
```powershell
scoop install git
```

### Étape 2 : Ajouter les buckets de dépôts Java
```powershell
scoop bucket add java
scoop bucket add extras
```

---

## 3. Recherche et Installation de JDK avec Scoop

### 1. Rechercher des versions de JDK disponibles
Pour rechercher une version spécifique ou une distribution (Temurin, Oracle, OpenJDK, Corretto, Zulu, etc.) :

```powershell
scoop search jdk
```
*Ou pour une version spécifique (ex. Java 17) :*
```powershell
scoop search openjdk17
```

### 2. Installer des JDKs
Installez les versions dont vous avez besoin pour vos projets :

```powershell
# Installer OpenJDK 21 (par défaut la version courante)
scoop install openjdk

# Installer des versions spécifiques / distributions
scoop install openjdk17
scoop install openjdk11
scoop install temurin17-jdk
scoop install zulu17
```

> **Note :** Scoop gère automatiquement les variables d'environnement (`PATH` et `JAVA_HOME`) lors de la première installation.

---

## 4. Changer de version de JDK (Commandes Clés)

C'est ici que Scoop brille : vous pouvez basculer d'une version de Java à une autre en une seule commande.

### 1. Voir la version de Java actuellement active
```powershell
java -version
```

### 2. Lister les JDKs installés sur votre machine
```powershell
scoop list
```

### 3. Basculer vers une autre version de JDK (`scoop reset`)
Pour changer la version active dans votre système, utilisez la commande `reset` en spécifiant le nom du paquet souhaité :

```powershell
# Basculer vers Java 17
scoop reset openjdk17

# Basculer vers Java 11
scoop reset openjdk11

# Basculer vers Temurin 17
scoop reset temurin17-jdk
```

**⚠️ Important après un changement :** 
Fermez et rouvrez votre fenêtre PowerShell (ou invite de commande) pour que les variables d'environnement (`JAVA_HOME` et `PATH`) prennent pleinement effet dans votre terminal.

Pour vérifier le changement :
```powershell
java -version
echo $env:JAVA_HOME
```

---

## 5. Astuce Avancée : Utiliser `scoop-search` et `scoop-checkup`

### 1. Diagnostic de l'environnement Scoop
Si vous rencontrez des problèmes de PATH ou de variables d'environnement :
```powershell
scoop checkup
```

### 2. Désinstaller une version de JDK inutilisée
```powershell
scoop uninstall openjdk11
```

### 3. Mettre à jour les JDK et Scoop
```powershell
# Mettre à jour la liste des paquets et Scoop
scoop update

# Mettre à jour un JDK spécifique vers sa dernière révision
scoop update openjdk17

# Mettre à jour TOUTES les applications installées via Scoop
scoop update *
```

---

## 6. Résumé Rapide des Commandes (Cheatsheet)

| Action | Commande |
| :--- | :--- |
| **Rechercher un JDK** | `scoop search jdk` |
| **Installer un JDK** | `scoop install openjdk17` |
| **Changer de JDK actif** | `scoop reset openjdk17` |
| **Vérifier le JDK actif** | `java -version` |
| **Lister les versions installées** | `scoop list` |
| **Désinstaller un JDK** | `scoop uninstall openjdk17` |