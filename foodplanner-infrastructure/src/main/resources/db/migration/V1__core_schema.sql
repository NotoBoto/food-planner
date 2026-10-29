-- Kern-Schema aus §4.2 der Spezifikation. Die generierten Suchspalten von product,
-- immutable_unaccent und die Suchindizes kommen mit einer eigenen Migration (#19).
-- Geld in Cent als bigint, Nährwerte pro 100 g als numeric, Packungsgrößen in ganzen Gramm.

CREATE TABLE product (
    id            bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    off_code      text UNIQUE,
    aldi_sku      text UNIQUE,
    name          text    NOT NULL,
    brand         text,
    category      text,
    package_grams integer NOT NULL CHECK (package_grams BETWEEN 1 AND 50000),
    unit          text    NOT NULL CHECK (unit IN ('G', 'ML')),
    kcal100       numeric,
    protein100    numeric,
    fat100        numeric,
    carbs100      numeric,
    source        text    NOT NULL CHECK (source IN ('ALDI', 'OPENFOODFACTS', 'SEED')),
    CONSTRAINT product_has_key CHECK (off_code IS NOT NULL OR aldi_sku IS NOT NULL),
    CONSTRAINT product_nutrition_complete CHECK (
        num_nulls(kcal100, protein100, fat100, carbs100) IN (0, 4))
);

CREATE TABLE price (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id  bigint      NOT NULL REFERENCES product (id),
    price_cents bigint      NOT NULL CHECK (price_cents > 0),
    source_name text        NOT NULL,
    scraped_at  timestamptz NOT NULL,
    is_current  boolean     NOT NULL
);

CREATE INDEX price_product_id_idx ON price (product_id);

-- Ein Preis wird abgelöst, nie überschrieben (§4.4): höchstens ein aktueller Preis je Produkt.
CREATE UNIQUE INDEX price_one_current_per_product ON price (product_id) WHERE is_current;

CREATE TABLE scraped_item (
    id              bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    raw_name        text        NOT NULL,
    price_cents     bigint      NOT NULL CHECK (price_cents > 0),
    url             text        NOT NULL,
    scraped_at      timestamptz NOT NULL,
    category        text,
    image_url       text,
    search_state    text        NOT NULL DEFAULT 'NOT_SEARCHED'
        CHECK (search_state IN ('NOT_SEARCHED', 'SEARCHED', 'NO_MATCH', 'RESOLVED', 'NOT_FOOD')),
    aldi_sku        text,
    kcal100         numeric,
    protein100      numeric,
    fat100          numeric,
    carbs100        numeric,
    package_grams   integer CHECK (package_grams BETWEEN 1 AND 50000),
    unit            text CHECK (unit IN ('G', 'ML')),
    nutrition_basis text,
    CONSTRAINT scraped_item_nutrition_complete CHECK (
        num_nulls(kcal100, protein100, fat100, carbs100) IN (0, 4))
);

CREATE TABLE off_candidate (
    id                              bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    scraped_item_id                 bigint  NOT NULL REFERENCES scraped_item (id),
    off_code                        text    NOT NULL,
    name                            text,
    brand                           text,
    category                        text,
    package_grams                   integer CHECK (package_grams BETWEEN 1 AND 50000),
    package_grams_from_scraped_name integer CHECK (package_grams_from_scraped_name BETWEEN 1 AND 50000),
    image_url                       text,
    kcal100                         numeric,
    protein100                      numeric,
    fat100                          numeric,
    carbs100                        numeric,
    confidence                      numeric CHECK (confidence BETWEEN 0 AND 1),
    rank                            smallint CHECK (rank BETWEEN 1 AND 5),
    is_manual                       boolean NOT NULL DEFAULT false,
    status                          text    NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    decided_at                      timestamptz,
    CONSTRAINT off_candidate_once_per_item UNIQUE (scraped_item_id, off_code),
    CONSTRAINT off_candidate_scored_unless_manual CHECK (
        is_manual OR (confidence IS NOT NULL AND rank IS NOT NULL)),
    CONSTRAINT off_candidate_decided_at_matches_status CHECK (
        (status = 'PENDING') = (decided_at IS NULL)),
    CONSTRAINT off_candidate_nutrition_complete CHECK (
        num_nulls(kcal100, protein100, fat100, carbs100) IN (0, 4))
);

-- Die Freigabe ist idempotent (§4.4): höchstens ein freigegebener Kandidat je gescrapter Zeile.
CREATE UNIQUE INDEX off_candidate_one_approved_per_item ON off_candidate (scraped_item_id) WHERE status = 'APPROVED';

CREATE TABLE scraped_page (
    id            bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    url           text        NOT NULL UNIQUE,
    last_modified timestamptz,
    fetched_at    timestamptz NOT NULL,
    product_id    bigint REFERENCES product (id)
);

CREATE INDEX scraped_page_product_id_idx ON scraped_page (product_id);
