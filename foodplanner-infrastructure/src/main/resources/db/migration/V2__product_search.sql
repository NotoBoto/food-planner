-- Suchspalten und Trigramm-Indizes für die lexikalische Produktsuche (§5.5, #19).
-- Name, Marke und Kategorie werden gefaltet gespeichert: Kleinschreibung, danach unaccent.
-- Die Java-Faltung (SearchText.fold) bildet denselben Ausdruck nach; SearchFoldingTest hält beide gleich.
--
-- lower() läuft mit COLLATE pg_c_utf8, dem eingebauten Provider mit Unicode-Abbildung. Ohne die Angabe hinge das
-- Ergebnis von lc_ctype der Datenbank ab: Unter einer C-Locale bliebe Ä groß, und docker-compose und
-- Testcontainers könnten sich unterscheiden.
--
-- Eine generierte Spalte braucht eine IMMUTABLE-Funktion, unaccent ist aber nur STABLE. Der Wrapper legt Funktion
-- und Wörterbuch ausdrücklich fest und ist damit unveränderlich genug.

CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS unaccent;

CREATE FUNCTION immutable_unaccent(text) RETURNS text
    LANGUAGE sql IMMUTABLE PARALLEL SAFE STRICT
    RETURN public.unaccent('public.unaccent'::regdictionary, $1);

ALTER TABLE product
    ADD COLUMN search_name     text GENERATED ALWAYS AS (immutable_unaccent(lower(name COLLATE pg_c_utf8))) STORED,
    ADD COLUMN search_brand    text GENERATED ALWAYS AS (immutable_unaccent(lower(brand COLLATE pg_c_utf8))) STORED,
    ADD COLUMN search_category text GENERATED ALWAYS AS (immutable_unaccent(lower(category COLLATE pg_c_utf8))) STORED;

CREATE INDEX product_search_name_trgm ON product USING gin (search_name gin_trgm_ops);
CREATE INDEX product_search_brand_trgm ON product USING gin (search_brand gin_trgm_ops);
CREATE INDEX product_search_category_trgm ON product USING gin (search_category gin_trgm_ops);
