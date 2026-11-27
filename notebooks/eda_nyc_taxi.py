import marimo

__generated_with = "0.25.2"
app = marimo.App(width="medium")


@app.cell
def _():
    import sys
    from pathlib import Path
    import marimo as mo
    import pandas as pd
    import plotly.express as px

    root_path = str(Path(__file__).resolve().parents[1])
    if root_path not in sys.path:
        sys.path.append(root_path)

    from dataviz_exercice4.database import load_trips_data

    return load_trips_data, mo, px


@app.cell
def _(mo):
    mo.md("""
    # Analyse Exploratoire (EDA) - NYC Yellow Taxi
    Exploration des courses nettoyées avant la conception du tableau de bord final.
    """)
    return


@app.cell
def _(load_trips_data, mo):
    df, source = load_trips_data()
    mo.md(f"**Source active :** `{source}` — **{len(df):,}** courses valides chargées.")
    return (df,)


@app.cell
def _(df, mo):
    # Aperçu statistique rapide des variables numériques
    stats = df[["fare_amount", "total_amount", "trip_distance", "duration_min", "tip_amount"]].describe().round(2)
    mo.vstack([mo.md("### Statistiques descriptives"), mo.ui.table(stats)])
    return


@app.cell
def _(df, mo, px):
    # Distribution des tarifs et des distances
    fig_fare = px.histogram(
        df[df["fare_amount"] <= 80],
        x="fare_amount",
        nbins=40,
        title="Répartition du tarif de base (courses <= 80 $)",
        labels={"fare_amount": "Tarif ($)", "count": "Nombre de courses"},
        color_discrete_sequence=["#2563eb"],
    )

    fig_dist = px.histogram(
        df[df["trip_distance"] <= 25],
        x="trip_distance",
        nbins=40,
        title="Répartition des distances parcourues (<= 25 miles)",
        labels={"trip_distance": "Distance (miles)", "count": "Nombre de courses"},
        color_discrete_sequence=["#0d9488"],
    )

    mo.vstack([fig_fare, fig_dist])
    return


@app.cell
def _(df, mo, px):
    # Analyse horaire et moyens de paiement
    hourly = df.groupby("pickup_hour", as_index=False).agg(
        nb_courses=("fare_amount", "count"),
        tarif_moyen=("fare_amount", "mean"),
    )

    fig_hour = px.line(
        hourly,
        x="pickup_hour",
        y="nb_courses",
        markers=True,
        title="Volume de courses par heure de la journée",
        labels={"pickup_hour": "Heure (0-23h)", "nb_courses": "Nombre de courses"},
    )

    fig_pay = px.box(
        df[df["tip_amount"] <= 20],
        x="payment_label",
        y="tip_amount",
        title="Distribution des pourboires selon le mode de paiement",
        labels={"payment_label": "Paiement", "tip_amount": "Pourboire ($)"},
        color="payment_label",
    )

    mo.vstack([fig_hour, fig_pay])
    return


@app.cell
def _(df, px):
    # Matrice de corrélation pour préparer la sélection de features de l'exercice 5
    cols = ["fare_amount", "total_amount", "trip_distance", "duration_min", "tip_amount", "passenger_count"]
    corr = df[cols].corr().round(2)

    fig_corr = px.imshow(
        corr,
        text_auto=True,
        color_continuous_scale="RdBu_r",
        zmin=-1,
        zmax=1,
        title="Corrélations entre les variables numériques",
    )
    fig_corr
    return


if __name__ == "__main__":
    app.run()
