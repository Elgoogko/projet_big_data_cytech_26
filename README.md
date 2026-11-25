# Projet Big Data

## Sommaire
<img src="https://upload.wikimedia.org/wikipedia/commons/f/f8/CY_Tech.svg" align="right"
     alt="Logo CY Tech" width="170">

* [Présentation](#présentation) : Présentation du projet et de ses membres
* [Environnement](#environnement) : Installation des dépendances
* [Lancer le projet](#lancer-le-projet) : Instructions pour lancer le projet

## Présentation
Projet Big Data de l'année 2024-2025 de CY Tech réalisé par : 
- Bari-Joris BOICOS
- Baptiste LAMBERT
- Florianne PAN
- Jonathan NGO
- Junjie (Eric) SHAO

## Environnement

- Java 17
- Python 3.10
- Scala 2.13.17
- RustFS

## Lancer le projet
Avant tout, veillez d'abord lancer les conteneurs docker:
```
docker compose up
```

Et ajouter le .env : 
```
cp .env.example .env
```

### Ouvrir RustFS
RustFS peut être accédé sur `http://localhost:9001` en s'authentifiant avec:  
**Account** : `rustfsadmin`  
**Key** : `rustfsadmin`


### Téléchargement des données
Pour télécharger les fichiers parquet des N derniers mois disponibles (par défaut à 3) dans RustFS :
```
cd spark_data_integration
sbt "run --nb-months N"
```

### Data Warehouse PostgreSQL (Modèle en flocon)
#### 1. Démarrer PostgreSQL
Pour démarrer uniquement la base de données :
```
docker compose up -d postgres
```

Attendre que la base soit prête avant de lancer les scripts :
```
until docker compose exec postgres pg_isready -U postgres -d nyc_taxi; do sleep 2; done
```
#### 2. Créer les tables du modèle en flocon et leurs contraintes
```
docker compose exec -T postgres psql -U postgres -d nyc_taxi -v ON_ERROR_STOP=1 < spark_data_integration_exercice3/creation.sql
```
#### 3. Insérer les données de référence (vendeurs, zones, dates, heures).
```
docker compose exec -T postgres psql -U postgres -d nyc_taxi -v ON_ERROR_STOP=1 < spark_data_integration_exercice3/insertion.sql
```
#### Informations de connexion 
- **Base** : `nyc_taxi`
- **Utilisateur** : `postgres`
- **Mot de passe** : `postgres`
- **URL JDBC** : `jdbc:postgresql://postgres:5432/nyc_taxi`


