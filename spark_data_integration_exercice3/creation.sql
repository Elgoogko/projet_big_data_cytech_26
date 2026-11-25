-- creation.sql (PostgreSQL) : Data Warehouse NYC Yellow Taxi (modèle en flocon) avec Data Dictionary Yellow Taxi Trip Records (18 march 2025) et taxi_zone_lookup.csv 

DROP TABLE IF EXISTS fact_trip         CASCADE;
DROP TABLE IF EXISTS dim_location      CASCADE;
DROP TABLE IF EXISTS dim_borough       CASCADE;
DROP TABLE IF EXISTS dim_service_zone  CASCADE;
DROP TABLE IF EXISTS dim_time          CASCADE;
DROP TABLE IF EXISTS dim_hm            CASCADE;
DROP TABLE IF EXISTS dim_md            CASCADE;
DROP TABLE IF EXISTS dim_vendor        CASCADE;
DROP TABLE IF EXISTS dim_ratecode      CASCADE;
DROP TABLE IF EXISTS dim_payment_type  CASCADE;

-- Dimensions simples

CREATE TABLE dim_vendor (
    vendorid   SMALLINT     PRIMARY KEY,
    vendorname VARCHAR(100) NOT NULL
);

CREATE TABLE dim_ratecode (
    ratecodeid SMALLINT     PRIMARY KEY,
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
    locationid     SMALLINT     PRIMARY KEY,   
    zone_name       VARCHAR(100) NOT NULL,
    borough_id      SMALLINT     NOT NULL REFERENCES dim_borough(borough_id),
    service_zone_id SMALLINT     NOT NULL REFERENCES dim_service_zone(service_zone_id)
);

-- Dimension temps (flocon : dim_time -> dim_hm et dim_md)

CREATE TABLE dim_hm (
    hm_id  SMALLINT PRIMARY KEY,                -- ex : 1435 pour 14h35
    hour   SMALLINT NOT NULL CHECK (hour   BETWEEN 0 AND 23),
    minute SMALLINT NOT NULL CHECK (minute BETWEEN 0 AND 59),
    UNIQUE (hour, minute)
);

CREATE TABLE dim_md (
    md_id SMALLINT PRIMARY KEY,                 -- ex : 115 pour 15 janvier 
    month SMALLINT NOT NULL CHECK (month BETWEEN 1 AND 12),
    day   SMALLINT NOT NULL CHECK (day   BETWEEN 1 AND 31),
    UNIQUE (month, day)
);

CREATE TABLE dim_time (
    time_id TIMESTAMP PRIMARY KEY,              -- timestamp arrondi à la minute
    year    SMALLINT  NOT NULL,
    hm_id   SMALLINT  NOT NULL REFERENCES dim_hm(hm_id),
    md_id   SMALLINT  NOT NULL REFERENCES dim_md(md_id),
    UNIQUE (year, hm_id, md_id)
);
-- Table de faits

CREATE TABLE fact_trip (
-- clés étrangères vers les dimensions
    vendorid              SMALLINT      REFERENCES dim_vendor(vendorid),
    tpep_pickup_datetime  TIMESTAMP     NOT NULL REFERENCES dim_time(time_id),
    tpep_dropoff_datetime TIMESTAMP     NOT NULL REFERENCES dim_time(time_id),
    passenger_count       SMALLINT      CHECK (passenger_count >= 0),
    trip_distance         NUMERIC(10,2) CHECK (trip_distance >= 0),
    ratecodeid            SMALLINT      REFERENCES dim_ratecode(ratecodeid),
    store_and_fwd_flag    CHAR(1)       CHECK (store_and_fwd_flag IN ('Y', 'N')),
    pulocationid          SMALLINT      NOT NULL REFERENCES dim_location(locationid),
    dolocationid          SMALLINT      NOT NULL REFERENCES dim_location(locationid),
    payment_type          SMALLINT      REFERENCES dim_payment_type(payment_type_id),

    -- mesures financières
    fare_amount           NUMERIC(10,2),
    extra                 NUMERIC(10,2),
    mta_tax               NUMERIC(10,2),
    tip_amount            NUMERIC(10,2),
    tolls_amount          NUMERIC(10,2),
    improvement_surcharge NUMERIC(10,2),
    total_amount          NUMERIC(10,2),
    congestion_surcharge  NUMERIC(10,2),
    airport_fee           NUMERIC(10,2),
    cbd_congestion_fee    NUMERIC(10,2)
);


