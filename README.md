# Projet Big Data

# Sommaire
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

- Java 11
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
uv run download_parquet.py --nb-months N
```