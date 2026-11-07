import boto3
from datetime import datetime
from dateutil.relativedelta import relativedelta
import requests
import sys

RUSTFS_ENDPOINT = "http://localhost:9000"
AWS_ACCESS_KEY = "rustfsadmin"
AWS_SECRET_KEY = "rustfsadmin"

BUCKET_NAME = "nyc-bucket"
TARGET_DIR = "nyc_raw"
NYC_TLC_BASE_URL = (
    "https://d37ci6vzurychx.cloudfront.net/trip-data/yellow_tripdata_{}.parquet"
)

def check_rustfs_connection() -> bool:
    """Vérifie si le cluster RustFS tourne bien

    Returns:
        bool: True si RustFS répond, False sinon.
    """
    try:
        response = requests.get(RUSTFS_ENDPOINT, timeout=2)
        return True
    except requests.RequestException:
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
        print(f"Création du bucket : '{bucket_name}'")
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
        print(f"Fichier envoyé vers {bucket}/{object_name} avec succès\n")
        return True
    except Exception as e:
        print(f" Échec du transfert pour {url} : {e}\n")
        return False

def fetch_recent_nyc_data(nb_months: int = 3):
    """Orchestre la recherche et le téléchargement des nb_months derniers mois disponibles sur NYC.

    Args:
        nb_months (int, optional): Nombre de mois récents disponibles à télécharger. Par défaut à 3.
    """
    if not check_rustfs_connection():
        print("\n" + "=" * 60)
        print(" Le cluster RustFS n'est pas accessible !")
        print("=" * 60)
        print(" Veuillez d'abord démarrer Docker avec la commande :")
        print("\n    docker compose up\n")
        print("=" * 60 + "\n")
        sys.exit(1)

    s3_client = get_s3_client()
    ensure_bucket_exists(s3_client, BUCKET_NAME)

    now = datetime.now()
    downloaded_count = 0
    max_search_depth = 12

    print(f"Recherche des {nb_months} derniers mois de données disponibles...")

    for i in range(max_search_depth):
        if downloaded_count >= nb_months:
            break

        year_month = (now - relativedelta(months=i)).strftime("%Y-%m")
        file_url = NYC_TLC_BASE_URL.format(year_month)

        if check_remote_parquet_exists(file_url):
            downloaded_count += 1
            object_name = f"{TARGET_DIR}/yellow_tripdata_{year_month}.parquet"

            print(
                f"[{downloaded_count}/{nb_months}] Mois disponible détecté : {year_month}"
            )
            print(f" Téléchargement en cours : {file_url}")
            download_to_rustfs(s3_client, file_url, BUCKET_NAME, object_name)

    if downloaded_count < nb_months:
        print(
            f"Attention : Seulement {downloaded_count}/{nb_months} mois récents ont été récupérés."
        )
    else:
        print(f"Terminé : Les {nb_months} mois ont été chargés dans RustFS.")

if __name__ == "__main__":
    fetch_recent_nyc_data(nb_months=3)