# Comment setup Scala / Hadoop / Spark / RustFS ?

> [!CAUTION]
> Vous allez installer différents programmes et versions de logiciels, ils sont pour la plupart non rétrocompatible et ne s'accordent pas tous entre eux (seuls quelques combos de version Spark + Hadoop + Java fonctionnent correctement)
> J'ai donc décidé (Unilatéralement) qu'on va tous installer les versions qui fonctionnent sur mes deux ordinateurs (Windows 11 et Ubuntu).
> Ainsi, quelques soit les versions recommandées par les tutoriels, prenez toujours ces versions :
> - **Hadoop 3.4.0**
> - **Java 11**
> - **Spark 2.13.15**
> - **Sbt dernière version** (lui, il est cool, il ne change rien)

## 1. Manager de versions Java
### 🪟 Windows
> [!NOTE]
> Sur Windows, je recommande fortement d'installer Scoop au lieu de SDKMAN !
> Même si l'installation de la WSL (Windows subsystem for Linux) sera nécéssaire plus tard quelque soit votre choix

#### 1.1 SDKMAN ! sur Windows

SDKMAN ! est un manager de versions pour les JDKS. Il est utilisé sur Linux, d'où le fait que je propose une solution alternative.
Référé vous au guide d'installation : [Guide SDKMAN! Windows](guide_sdkman_windows.md)

#### 1.2 Scoop

Scoop est un manager de versions de logiciels en général, pas seulement Java et est disponible sur Windows uniquement.
Référé vous au guide d'installation : [Guide Scoop](guide_scoop_windows_jdk.md)

> [!WARNING]
> SDKMAN! et scoop n'utilise pas les mêmes noms pour définir les versions de Java !
> Ainsi, pour java 11 :
> - Scoop : openjdk11
> - SDKMAN! : java 11.0.32-amzn

### 🐧Linux
#### 1.1 Manager de versions Java

SDKMAN! est fait (et est à ma connaissance le seul) manager de versions de Java sur Linux.
Référé vous au guide d'installation : [Guide SDKMAN! Linux](guide_sdkman_linux.md)

## 2. Hadoop et HDFS 

### 🪟Windows
Le plus simple est de suivre le guide d'installation fait par ruslanmv : [How to Install Hadoop on Windows 11](Install_hadoop_windows.md)

> [!CAUTION]
> Même si dans le guide, il utilise des versions antérieures de hadoop et Java, installez la version **Hadoop 3.4.0** (Fichiers Bin disponible dans le github)
> N'installez pas Java via ce guide non plus.

### 🐧Linux

Suivez le guide d'installation [Installation Hadoop Linux](Install_hadoop_linux.md).

> [!CAUTION]
> Par principe, on n'a pas besoin d'installer Spark car "sbt" télécharge tout seul la version de spark demandé dans le fichier de build.

## 3. l'IDE

### 3.1. ♨️ IntelliJ IDEA
Tout est automatique : IntelliJ télécharge automatiquement les versions de SBT et Spark.

Ajoutez les plugins :
- Big Data
- Spark
- Scala

Cependant, pour démarrer le programme avec le bouton "Run", il faut ajouter ces options à la VM.
Run Configuration
```
Les 3 petits points en haut a gauche
│   
└───Edit configuration
    └─── Add VM options
```

Et copier dans la boite de texte : 
```
  "--add-opens=java.base/java.lang=ALL-UNNAMED",
  "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
  "--add-opens=java.base/java.io=ALL-UNNAMED",
  "--add-opens=java.base/java.net=ALL-UNNAMED",
  "--add-opens=java.base/java.nio=ALL-UNNAMED",
  "--add-opens=java.base/java.util=ALL-UNNAMED",
  "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
  "--add-opens=java.base/sun.nio.cs=ALL-UNNAMED",
  "--add-opens=java.base/sun.security.action=ALL-UNNAMED",
  "--add-opens=java.base/sun.util.calendar=ALL-UNNAMED"
```

### 3.2 🆚 Vscode

Ajouter l'extension "Metals" pour Scala et Spark. Le mieux reste quand mêmes les commandes sbt
Commandes: 
```bash
# Pour compilé en avance
sbt compile

# Pour lancer le programme. Faire Run va aussi compilé le programme
# Attention : si il y à plusieurs main (extends app, main ect..) alors sbt va demander lequel il faut exécuté
sbt run

# Télécharger / mettre à jour les dépendances
sbt reload

# Télécharge les mises à jour des dépendances déjà présentent
sbt update

# Nettoie le dossier Target
sbt clean
```

## 4. RustFS

### 4.1 Installer Docker
Pour ça, tout le monde aura la même méthode avec Docker.
Docker c'est un gestionnaire de conteneurs (on l'a vu en première année)

D'abord installer Docker Desktop (L'application, pas le CLI, sinon vous aurez une erreur du type "Back end is not running")
- 🪟 Windows : [Docker Windows](https://docs.docker.com/desktop/setup/install/windows-install/)
- 🐧 Linux :   [Docker Linux](https://docs.docker.com/desktop/setup/install/linux/)

> [!WARNING]
> Pour windows, il va falloir installer WSL cette fois. Pour l'installer: 
> ```bash
> # Terminal en mode administrateur
> wsl --install
> 
> # Installer Ubuntu
> wsl --install -d Ubuntu
> ```
> Les commandes bash seront alors disponible sur windows. Mais attention : ce n'est pas un autre système, c'est un
adaptateur. Éviter donc les commandes du type "rm -r./" car elles s'attaqueront directement à Windows.

### 4.2 RustFS

Une fois Docker desktop installer, vous pouvez l'ouvrir et vous y connecter. Les commandes dockers seront alors disponibles sur le terminal.
Exécuté ces commandes pour installer rustFS
```bash
docker volume create rustfs-data
docker run -d --name rustfs -p 9000:9000 -p 9001:9001 -v rustfs-data:/data rustfs/rustfs:latest /data
docker logs -f rustfs
```

Une fois que c'est fait, aller dans Docker Desktop dans "Containers" → "Actions" → "Start".
RustFS va demander un nom d'utilisateur et un mot de passe. Mettez ceux de base : 
- nom : rustfsadmin
- mdp : rustfsadmin

Quand c'est fait, aller sur le localhost (http://localhost:9001) pour voir si l'interface graphique est présente. Connecter vous à la page avec le nom et le mdp précédent.