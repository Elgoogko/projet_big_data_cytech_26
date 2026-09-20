### 1. Prérequis & Java

Hadoop et Spark nécessitent OpenJDK 11.

```bash
sudo apt update && sudo apt install openjdk-11-jdk open-ssh-server wget tar -y
java -version
```

---

### 2. Configuration SSH sans mot de passe

Hadoop utilise SSH pour gérer la communication des nœuds (même en local).

```bash
ssh-keygen -t rsa -P '' -f ~/.ssh/id_rsa
cat ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys
chmod 0600 ~/.ssh/authorized_keys
ssh localhost
```

*(Répondez `yes` à la première connexion, puis déconnectez-vous avec `exit`)*.

---

### 3. Installation et configuration de Hadoop (HDFS)

#### A. Téléchargement et extraction

```bash
wget https://dlcdn.apache.org/hadoop/common/hadoop-3.4.3/hadoop-3.4.3.tar.gz
tar -xvzf hadoop-3.4.3.tar.gz
sudo mv hadoop-3.4.3 /usr/local/hadoop
```

#### B. Variables d'environnement

Ajoutez ces lignes à la fin de votre fichier `~/.bashrc` :

```bash
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
export HADOOP_HOME=/usr/local/hadoop
export HADOOP_INSTALL=$HADOOP_HOME
export HADOOP_MAPRED_HOME=$HADOOP_HOME
export HADOOP_COMMON_HOME=$HADOOP_HOME
export HADOOP_HDFS_HOME=$HADOOP_HOME
export YARN_HOME=$HADOOP_HOME
export PATH=$PATH:$HADOOP_HOME/bin:$HADOOP_HOME/sbin
```

Appliquez les changements : `source ~/.bashrc`.

#### C. Configuration des fichiers Hadoop

1. Définir Java dans `/usr/local/hadoop/etc/hadoop/hadoop-env.sh` :
```bash
echo "export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64" >> /usr/local/hadoop/etc/hadoop/hadoop-env.sh
```


2. Éditer `/usr/local/hadoop/etc/hadoop/core-site.xml` :
```xml
<configuration>
    <property>
        <name>fs.defaultFS</name>
        <value>hdfs://localhost:9000</value>
    </property>
</configuration>

```


3. Éditer `/usr/local/hadoop/etc/hadoop/hdfs-site.xml` :
```xml
<configuration>
    <property>
        <name>dfs.replication</name>
        <value>1</value>
    </property>
    <property>
        <name>dfs.namenode.name.dir</name>
        <value>file:///usr/local/hadoop/data/namenode</value>
    </property>
    <property>
        <name>dfs.datanode.data.dir</name>
        <value>file:///usr/local/hadoop/data/datanode</value>
    </property>
</configuration>

```



#### D. Formater et démarrer HDFS

```bash
# Formatage du NameNode
hdfs namenode -format

# Démarrage des services HDFS et YARN
start-dfs.sh
start-yarn.sh

# Vérification des services actifs
jps

```

*Pour vérifier la réussite :* La commande `jps` doit afficher `NameNode`, `DataNode`, `SecondaryNameNode`, `ResourceManager` et `NodeManager`. Interface web accessible sur `http://localhost:9870`.

---

### 4. Installation de Scala

```bash
sudo apt install scala -y
scala -version

```

---

### 5. Installation de Spark

#### A. Téléchargement et extraction

```bash
wget https://dlcdn.apache.org/spark/spark-3.5.9/spark-3.5.9-bin-hadoop3.tgz
tar -xvzf spark-3.5.9-bin-hadoop3.tgz
sudo mv spark-3.5.9-bin-hadoop3 /usr/local/spark

```

#### B. Variables d'environnement

Ajoutez ces lignes à la fin de `~/.bashrc` :

```bash
export SPARK_HOME=/usr/local/spark
export PATH=$PATH:$SPARK_HOME/bin:$SPARK_HOME/sbin

```

Appliquez les changements : `source ~/.bashrc`.

#### C. Lancement de Spark

Pour exécuter Spark avec Scala :

```bash
spark-shell

```

*Pour vérifier la réussite :* La console Scala/Spark s'ouvre. Testez en exécutant `sc.parallelize(1 to 10).sum()`, cela doit renvoyer `Double = 55.0`. Tapez `:quit` pour sortir. Interface web accessible sur `http://localhost:4040` pendant le fonctionnement.