"""
Application web interactive dont le but est d'afficher le tableau de bord de visualisation des courses de taxis jaunes de New York.
Le but est de permettre une exploration dynamique des données nettoyées grâce à des filtres latéraux,
des indicateurs clés (KPI) et plusieurs graphiques interactifs générés avec Plotly.
"""

import streamlit as st
import plotly.express as px
from database import load_trips_data

st.set_page_config(page_title="NYC Taxi - Dataviz", layout="wide")

st.title("Analyse des courses de taxis jaunes - New York")


@st.cache_data(show_spinner="Chargement des données depuis le datamart...")
def get_data():
    """
    Récupère les données préparées via le module database et les conserve en cache dans la mémoire de Streamlit.
    Le but est d'éviter de réinterroger la base de données ou de relire le fichier Parquet à chaque fois
    que l'utilisateur modifie un filtre dans l'interface.

    :return: Tuple contenant le dataframe préparé et le libellé de la source connectée
    """
    return load_trips_data()


df, source_name = get_data()
st.caption(f"Source de données connectée : {source_name}")

# --- Filtres latéraux : permet de restreindre le périmètre d'analyse dynamiquement ---
st.sidebar.header("Filtres d'analyse")

payment_options = sorted(df["payment_label"].unique().tolist())
selected_payments = st.sidebar.multiselect(
    "Mode de paiement",
    options=payment_options,
    default=payment_options,
)

max_dist = float(min(df["trip_distance"].max(), 50.0))
dist_range = st.sidebar.slider(
    "Distance du trajet (miles)",
    min_value=0.0,
    max_value=max_dist,
    value=(0.0, 25.0),
)

hour_range = st.sidebar.slider(
    "Tranche horaire de prise en charge",
    min_value=0,
    max_value=23,
    value=(0, 23),
)

# Application des filtres sélectionnés sur le dataframe principal
filtered_df = df[
    (df["payment_label"].isin(selected_payments))
    & (df["trip_distance"].between(dist_range[0], dist_range[1]))
    & (df["pickup_hour"].between(hour_range[0], hour_range[1]))
]

# --- Indicateurs clés (KPI) : synthèse chiffrée calculée sur le périmètre filtré ---
col1, col2, col3, col4 = st.columns(4)
col1.metric("Nombre de courses", f"{len(filtered_df):,}".replace(",", " "))
col2.metric("Tarif moyen", f"{filtered_df['fare_amount'].mean():.2f} $")
col3.metric("Distance moyenne", f"{filtered_df['trip_distance'].mean():.2f} mi")
col4.metric("Durée moyenne", f"{filtered_df['duration_min'].mean():.1f} min")

st.divider()

# --- Graphiques principaux : analyse horaire et relation distance/tarif ---
left_col, right_col = st.columns(2)

with left_col:
    hourly_stats = (
        filtered_df.groupby("pickup_hour", as_index=False)
        .agg(courses=("fare_amount", "count"), prix_moyen=("fare_amount", "mean"))
    )
    fig_hourly = px.bar(
        hourly_stats,
        x="pickup_hour",
        y="courses",
        color="prix_moyen",
        color_continuous_scale="Blues",
        title="Affluence et tarif moyen par heure",
        labels={"pickup_hour": "Heure", "courses": "Nombre de courses", "prix_moyen": "Tarif moyen ($)"},
    )
    st.plotly_chart(fig_hourly, use_container_width=True)

with right_col:
    # Échantillonnage à 5 000 courses maximum pour garder un affichage fluide dans le navigateur
    sample_df = filtered_df.sample(min(len(filtered_df), 5000), random_state=42)
    fig_scatter = px.scatter(
        sample_df,
        x="trip_distance",
        y="fare_amount",
        color="payment_label",
        opacity=0.5,
        title="Relation Distance vs Tarif (échantillon de 5 000 courses)",
        labels={"trip_distance": "Distance (miles)", "fare_amount": "Tarif ($)", "payment_label": "Paiement"},
    )
    st.plotly_chart(fig_scatter, use_container_width=True)

# --- Graphiques secondaires : répartition des paiements et top des zones de départ ---
col_bottom_left, col_bottom_right = st.columns(2)

with col_bottom_left:
    pay_counts = filtered_df["payment_label"].value_counts().reset_index()
    pay_counts.columns = ["payment_label", "count"]
    fig_pie = px.pie(
        pay_counts,
        names="payment_label",
        values="count",
        hole=0.4,
        title="Répartition des méthodes de paiement",
    )
    st.plotly_chart(fig_pie, use_container_width=True)

with col_bottom_right:
    top_zones = (
        filtered_df.groupby(["PULocationID", "pickup_zone"], as_index=False)
        .size()
        .rename(columns={"size": "Courses"})
        .sort_values("Courses", ascending=False)
        .head(10)
    )
    fig_zones = px.bar(
        top_zones,
        x="Courses",
        y="pickup_zone",
        orientation="h",
        hover_data=["PULocationID"],
        title="Top 10 des zones de départ",
        labels={"Courses": "Nombre de départs", "pickup_zone": "Quartier / Zone TLC", "PULocationID": "ID Zone"},
    )
    fig_zones.update_layout(yaxis={"categoryorder": "total ascending"})
    st.plotly_chart(fig_zones, use_container_width=True)

# --- Tableau d'inspection : aperçu brut des 50 premières lignes filtrées ---
with st.expander("Voir un extrait des données brutes"):
    st.dataframe(filtered_df.head(50), use_container_width=True)