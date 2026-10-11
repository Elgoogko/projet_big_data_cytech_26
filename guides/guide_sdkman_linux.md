**SDKMAN!** (Software Development Kit Manager) est un outil pour gérer plusieurs versions de kits de développement (Java, Gradle, Maven, Spring Boot, Kotlin, etc.).

Sur Ubuntu, l'installation est native et se fait directement depuis le terminal.

#### Prérequis
Mettez à jour vos paquets et installez les outils de compression nécessaires (`zip` et `unzip`) ainsi que `curl`:
```bash
sudo apt update
sudo apt install -y curl zip unzip
```

#### Étapes d'installation
1. **Lancer l'installation** : Exécutez la commande officielle de téléchargement et d'installation :
   ```bash
   curl -s "https://sdkman.io" | bash
   ```

2. **Initialiser SDKMAN!** : Rechargez la configuration de votre terminal actuel (ou fermez-le et rouvrez-le) :
   ```bash
   source "$HOME/.sdkman/bin/sdkman-init.sh"
   ```

3. **Vérifier** : Validez que l'installation s'est bien déroulée en tapant :
   ```bash
   sdk version
   ```

---

### Télécharger des JDK (Java)

1. **Lister les versions disponibles** :
   ```bash
   sdk list java
   ```

2. **Installer une version spécifique** (ex: Eclipse Temurin 21 et 17) :
   ```bash
   sdk install java 21.0.2-tem
   sdk install java 17.0.10-tem
   ```

---

### Appliquer un JDK uniquement à un projet spécifique

SDKMAN! permet de lier une version de Java à un dossier de projet grâce au fichier `.sdkmanrc`.

### Configuration manuelle
1. Naviguez dans le dossier de votre projet :
   ```bash
   cd /chemin/vers/mon-projet
   ```

2. Activez temporairement la version de Java souhaitée pour ce projet :
   ```bash
   sdk use java 17.0.10-tem
   ```

3. Générez le fichier de configuration local :
   ```bash
   sdk env init
   ```
   *Un fichier `.sdkmanrc` contenant la version choisie est créé à la racine du projet.*

4. **Activer l'environnement** : Désormais, lorsque vous ouvrez votre terminal dans ce dossier, tapez simplement la commande suivante pour charger la bonne version :
   ```bash
   sdk env
   ```

#### Automatisation (Changement automatique lors du `cd`)
Pour éviter d'avoir à taper `sdk env` manuellement à chaque fois que vous entrez dans le dossier :

1. Ouvrez le fichier de configuration global de SDKMAN! :
   ```bash
   nano ~/.sdkman/etc/config
   ```
2. Trouvez la ligne `sdk_auto_env` et passez-la à `true` :
   ```text
   sdk_auto_env=true
   ```
3. Sauvegardez le fichier (avec `Ctrl+O` puis `Entrée` dans nano) et quittez (`Ctrl+X`).

Le terminal basculera automatiquement sur la bonne version de Java dès que vous entrerez dans le dossier de votre projet avec la commande `cd`.

