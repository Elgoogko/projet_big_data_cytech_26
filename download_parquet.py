import argparse
import logging
import os
import sys
from datetime import datetime

import boto3 # SDK AWS pour interagir avec l'API S3 du stockage d'objets (RustFS)
import requests # Bibliothèque de gestion / création de requêtes HTTP
import yaml # Lecture et chargement des fichiers de configuration YAML
from dateutil.relativedelta import relativedelta # Manipulation de date en +- des mois/années
from dotenv import load_dotenv # Chargement des variables d'environnement depuis le .env

# Configuration de base du logger
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - [%(levelname)s] - %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)

def load_environment() -> None:
    """Charge le fichier d'environnement .env et vérifie la présence des clés AWS."""
    load_dotenv()
    if not os.getenv("AWS_ACCESS_KEY") or not os.getenv("AWS_SECRET_KEY"):
        logging.error(
            "\n" + "=" * 60 + "\n"
            "Les variables AWS_ACCESS_KEY ou AWS_SECRET_KEY sont introuvables.\n "
            "Veuillez créer un fichier .env basé sur .env.example."
            "\n" + "=" * 60 + "\n"
        )
        sys.exit(1)

def load_config(config_path: str = "config.yaml") -> dict:
    """Charge le fichier de configuration YAML

    Args:
        config_path (str, optional): Chemin vers le fichier YAML. Par défaut à "config.yaml".

    Returns:
        dict: Dictionnaire contenant la configuration yaml globale.
    """
    if not os.path.exists(config_path):
        logging.error(f"Le fichier de configuration '{config_path}' n'existe pas.")
        sys.exit(1)

    with open(config_path, "r", encoding="utf-8") as f:
        return yaml.safe_load(f)

# Chargement des variables de l'environnement et de la configuration yaml
load_environment()
CONFIG = load_config()

AWS_ACCESS_KEY = os.getenv("AWS_ACCESS_KEY")
AWS_SECRET_KEY = os.getenv("AWS_SECRET_KEY")

RUSTFS_CONFIG = CONFIG["rustfs"]
NYC_TLC_CONFIG = CONFIG["nyc_tlc"]

RUSTFS_ENDPOINT = RUSTFS_CONFIG["endpoint"]
BUCKET_NAME = RUSTFS_CONFIG["bucket_name"]
TARGET_DIR = RUSTFS_CONFIG["target_dir"]

NYC_TLC_BASE_URL = NYC_TLC_CONFIG["base_url"]

def check_rustfs_connection() -> bool:
    """Vérifie la connexion vers RustFS

    Returns:
        bool: True si RustFS répond, False sinon.
    """
    try:
        response = requests.get(RUSTFS_ENDPOINT, timeout=2)
        logging.info("Connexion à RustFS établie avec succès (HTTP 200 OK).")
        return True
    except requests.exceptions.HTTPError as err:
        logging.error(f"Échec de connexion à RustFS - Erreur HTTP {response.status_code} : {err}")
        return False
    except requests.RequestException as err:
        logging.error(f"Impossible de joindre le serveur RustFS ({RUSTFS_ENDPOINT}) : {err}")
        return False

def get_s3_client():
    """Initialise et retourne un client S3 configuré pour RustFS.

    Returns:
        botocore.client.S3: Un client S3 connecté à l'instance local RustFS.
    """
    return boto3.client(
        "s3",
        endpoint_url=RUSTFS_ENDPOINT,
        aws_access_key_id=AWS_ACCESS_KEY,
        aws_secret_access_key=AWS_SECRET_KEY,
    )

def ensure_bucket_exists(s3_client, bucket_name: str):
    """Vérifie l'existence d'un bucket S3 et le crée dans le RustFS s'il n'existe pas.

    Args:
        s3_client (botocore.client.S3): Le client S3 à utiliser.
        bucket_name (str): Nom du bucket S3 à vérifier ou créer.
    """
    try:
        # demande juste les entêtes/métadonnées du bucket pour connaitre existence
        s3_client.head_bucket(Bucket=bucket_name)
    except s3_client.exceptions.ClientError:
        logging.info(f"Création du bucket : '{bucket_name}'")
        s3_client.create_bucket(Bucket=bucket_name)

def check_remote_parquet_exists(url: str) -> bool:
    """Vérifie l'existence du fichier Parquet distant via une requête HEAD.

    Args:
        url (str): L'URL du fichier Parquet sur le serveur distant.

    Returns:
        bool: True si le fichier existe (HTTP 200), False sinon.
    """
    try:
        response = requests.head(url, timeout=5)
        return response.status_code == 200
    except requests.RequestException:
        return False

def download_to_rustfs(s3_client, url: str, bucket: str, object_name: str) -> bool:
    """Télécharge le fichier distant en RAM et le transfère vers RustFS.

    Args:
        s3_client (botocore.client.S3): Le client S3 connecté à RustFS.
        url (str): L'URL source du fichier distant à télécharger.
        bucket (str): Nom du bucket cible dans RustFS.
        object_name (str): Clé S3 (chemin/nom) sous laquelle stocker l'objet.

    Returns:
        bool: True si le transfert a réussi, False en cas d'échec.
    """
    try:
        with requests.get(url, stream=True, timeout=30) as response:
            response.raise_for_status() # Fait rien si fichier ok, sinon exception
            s3_client.upload_fileobj(response.raw, bucket, object_name)
        logging.info(f"Fichier envoyé vers {bucket}/{object_name} avec succès.")
        return True
    except Exception as e:
        logging.error(f"Échec du transfert pour {url} : {e}")
        return False

def fetch_recent_nyc_data(nb_months: int = 3):
    """Orchestre la recherche et le téléchargement des nb_months derniers mois disponibles sur NYC.

    Args:
        nb_months (int, optional): Nombre de mois récents disponibles à télécharger. Par défaut à 3.
    """
    if not check_rustfs_connection():
        logging.error(
            "\n" + "=" * 60 + "\n"
            "Traitement interrompu car RustFS est inaccessible. \n"
            "Vérifiez que le conteneur Docker est bien démarré (docker compose up) "
            "et que l'endpoint dans config.yaml est correct."
            "\n" + "=" * 60 + "\n"
            )
        sys.exit(1)

    s3_client = get_s3_client()
    ensure_bucket_exists(s3_client, BUCKET_NAME)

    now = datetime.now()
    downloaded_count = 0
    max_search_depth = nb_months + 6 # marge de 6 mois

    logging.info(f"Recherche des {nb_months} derniers mois de données disponibles...")

    for i in range(max_search_depth):
        if downloaded_count >= nb_months:
            break

        year_month = (now - relativedelta(months=i)).strftime("%Y-%m")
        file_url = NYC_TLC_BASE_URL.format(year_month)

        if check_remote_parquet_exists(file_url):
            downloaded_count += 1
            object_name = f"{TARGET_DIR}/yellow_tripdata_{year_month}.parquet"

            logging.info(
                f"[{downloaded_count}/{nb_months}] Mois disponible détecté : {year_month}"
            )

            logging.info(f" Téléchargement en cours : {file_url}")
            download_to_rustfs(s3_client, file_url, BUCKET_NAME, object_name)

    if downloaded_count < nb_months:
        logging.warning(
            f"Seulement {downloaded_count}/{nb_months} des mois les plus récents ont été récupérés."
        )
    else:
        logging.info(f"Terminé : Les {nb_months} mois ont été chargés dans RustFS.")

def parse_args() -> argparse.Namespace:
    """Récupère et vérifie les arguments de la ligne de commande.

    Returns:
        argparse.Namespace: Les arguments validés transmis au script.
    """
    parser = argparse.ArgumentParser(
        description="Télécharge les fichiers parquets des taxi jaunes de NYC vers RustFS"
    )

    parser.add_argument(
        "--nb_months",
        "--nb-months",
        type=int,
        default=3,
        metavar="N",
        help="N derniers mois disponibles à télécharger et envoyer dans RustFS (par défaut: 3).",
    )

    args = parser.parse_args()

    if args.nb_months < 1:
        parser.error("Le nombre de mois doit être supérieur ou égal à 1.")

    return args

if __name__ == "__main__":
    args = parse_args()
    
    fetch_recent_nyc_data(nb_months=args.nb_months)