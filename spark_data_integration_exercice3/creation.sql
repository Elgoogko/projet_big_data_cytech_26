-- creation.sql (PostgreSQL) : Data Warehouse NYC Yellow Taxi (modèle en flocon) avec Data Dictionary Yellow Taxi Trip Records (18 march 2025) et taxi_zone_lookup.csv 

DROP TABLE IF EXISTS fact_trip         CASCADE;
DROP TABLE IF EXISTS dim_location      CASCADE;
DROP TABLE IF EXISTS dim_borough       CASCADE;
DROP TABLE IF EXISTS dim_service_zone  CASCADE;
DROP TABLE IF EXISTS dim_date          CASCADE;
DROP TABLE IF EXISTS dim_month         CASCADE;
DROP TABLE IF EXISTS dim_time          CASCADE;
DROP TABLE IF EXISTS dim_vendor        CASCADE;
DROP TABLE IF EXISTS dim_ratecode      CASCADE;
DROP TABLE IF EXISTS dim_payment_type  CASCADE;

-- Dimensions simples

CREATE TABLE dim_vendor (
    vendor_id   SMALLINT     PRIMARY KEY,
    vendor_name VARCHAR(100) NOT NULL
);

CREATE TABLE dim_ratecode (
    ratecode_id SMALLINT     PRIMARY KEY,
    description VARCHAR(100) NOT NULL
);

CREATE TABLE dim_payment_type (
    payment_type_id SMALLINT     PRIMARY KEY,
    description     VARCHAR(100) NOT NULL
);


-- Dimension lieu (flocon : location -> borough = arrondissement, location -> service_zone)

CREATE TABLE dim_borough (
    borough_id   SMALLINT    PRIMARY KEY,
    borough_name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE dim_service_zone (
    service_zone_id   SMALLINT    PRIMARY KEY,
    service_zone_name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE dim_location (
    location_id     SMALLINT     PRIMARY KEY,   
    zone_name       VARCHAR(100) NOT NULL,
    borough_id      SMALLINT     NOT NULL REFERENCES dim_borough(borough_id),
    service_zone_id SMALLINT     NOT NULL REFERENCES dim_service_zone(service_zone_id)
);

-- Dimension temps (flocon : date -> mois et heure/minute à part)

CREATE TABLE dim_month (
    month_id INT      PRIMARY KEY,              -- ex : 202501
    year     SMALLINT NOT NULL,
    month    SMALLINT NOT NULL CHECK (month BETWEEN 1 AND 12),
    UNIQUE (year, month)
);

CREATE TABLE dim_date (
    date_id     INT      PRIMARY KEY,           -- ex : 20250115
    full_date   DATE     NOT NULL UNIQUE,
    day         SMALLINT NOT NULL CHECK (day BETWEEN 1 AND 31),
    day_of_week SMALLINT NOT NULL CHECK (day_of_week BETWEEN 1 AND 7), -- 1 = lundi
    is_weekend  BOOLEAN  NOT NULL,
    month_id    INT      NOT NULL REFERENCES dim_month(month_id)
);

CREATE TABLE dim_time (
    time_id SMALLINT PRIMARY KEY,               -- ex : 1435 pour 14h35
    hour    SMALLINT NOT NULL CHECK (hour   BETWEEN 0 AND 23),
    minute  SMALLINT NOT NULL CHECK (minute BETWEEN 0 AND 59),
    UNIQUE (hour, minute)
);

-- Table de faits

CREATE TABLE fact_trip (
    trip_id               BIGSERIAL     PRIMARY KEY,

    -- clés étrangères vers les dimensions
    vendor_id             SMALLINT      REFERENCES dim_vendor(vendor_id),
    ratecode_id           SMALLINT      REFERENCES dim_ratecode(ratecode_id),
    payment_type_id       SMALLINT      REFERENCES dim_payment_type(payment_type_id),
    pu_location_id        SMALLINT      NOT NULL REFERENCES dim_location(location_id),
    do_location_id        SMALLINT      NOT NULL REFERENCES dim_location(location_id),
    pickup_date_id        INT           NOT NULL REFERENCES dim_date(date_id),
    pickup_time_id        SMALLINT      NOT NULL REFERENCES dim_time(time_id),
    dropoff_date_id       INT           NOT NULL REFERENCES dim_date(date_id),
    dropoff_time_id       SMALLINT      NOT NULL REFERENCES dim_time(time_id),

    -- mesures
    passenger_count       SMALLINT      CHECK (passenger_count >= 0),
    trip_distance         NUMERIC(10,2) CHECK (trip_distance >= 0),
    fare_amount           NUMERIC(10,2),
    extra                 NUMERIC(10,2),
    mta_tax               NUMERIC(10,2),
    tip_amount            NUMERIC(10,2),
    tolls_amount          NUMERIC(10,2),
    improvement_surcharge NUMERIC(10,2),
    congestion_surcharge  NUMERIC(10,2),
    airport_fee           NUMERIC(10,2),
    cbd_congestion_fee    NUMERIC(10,2),
    total_amount          NUMERIC(10,2),
    
    -- attribut sans table dédiée
    store_and_fwd_flag    CHAR(1)       CHECK (store_and_fwd_flag IN ('Y', 'N')),
);


