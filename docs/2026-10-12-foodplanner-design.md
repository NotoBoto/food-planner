# FoodPlanner — Designspezifikation

**Datum:** 12.10.2026
**Abgabe:** ca. 15.12.2026 (Praxistransferprojekt, RFH Köln)
**Status:** Plan. Es ist noch nichts gebaut. Das Dokument beschreibt das System,
das entstehen soll, und die Reihenfolge, in der es entsteht.

Jede Funktion ist einer von zwei Stufen zugeordnet:

- **Kern** — muss zur Abgabe fertig sein. Die Abschnitte 1–9 beschreiben nur den
  Kern.
- **Erweiterung** — wird umgesetzt, wenn nach dem Kern Zeit bleibt, in der in
  §13 festgelegten Reihenfolge. Jede Erweiterung ist in §10 vollständig
  beschrieben, damit sie sich ergänzen lässt, ohne den Kern umzubauen.

## 1. Zweck

Eine Webanwendung, mit der Nutzer aus Produkten von Aldi Süd eine
Einkaufsliste zusammenstellen. Der Nutzer durchsucht einen lokalen
Produktkatalog, fügt Artikel zur Liste hinzu und sieht laufend den Gesamtpreis
sowie die Summe von Eiweiß, Fett, Kohlenhydraten und Kalorien, jeweils mit dem
Fortschritt gegenüber selbst gesetzten Zielen. Ein Klick auf einen
Listeneintrag öffnet die vollständige Produktkarte einschließlich der
Preishistorie, die der Katalog beobachtet hat.

Das Projekt ist eine Studienarbeit. Daraus folgen zwei Randbedingungen, die das
gesamte Design prägen:

- **Die Anwendung muss ohne Netzwerkzugriff auf Drittseiten laufen und korrekt
  vorführbar sein.** Jeder Scrape-Lauf und jede Suche in Open Food Facts wird
  als Snapshot unter `data/snapshots/` abgelegt und committet, und der Katalog
  befüllt sich selbst aus `data/seed/products.json`. Zum Bauen der Container
  ist Netzwerkzugriff nötig (Basis-Images, Maven-Abhängigkeiten, npm); zum
  Betrieb nicht.
- **Die Architektur soll erkennbare Muster demonstrieren.** Clean Architecture
  mit getrennten Commands und Queries pro Use Case wird einem einfachen
  Schichtenmodell vorgezogen; die höhere Anzahl an Dateien wird als Preis dafür
  akzeptiert. Die Abhängigkeitsrichtung wird durch ArchUnit-Tests erzwungen,
  nicht nur beschrieben.

### 1.1 Vorerkenntnisse aus einem Prototyp

Ein früherer Prototyp hat die folgenden Punkte gemessen. Sie bestimmen
Entscheidungen in diesem Plan und werden beim ersten Live-Lauf (§9.3) erneut
geprüft, weil sich die Aldi-Seite seitdem geändert haben kann.

- Rund **vier Fünftel** der Aldi-Produktseiten enthalten eine Nährwerttabelle
  pro 100 g bzw. 100 ml. Für diese Produkte ist kein Abgleich mit Open Food
  Facts nötig.
- Rund **ein Fünftel** der Lebensmittelzeilen landet im Review, weil Open Food
  Facts deutsche Eigenmarken lückenhaft abdeckt.
- Der Nuxt-Payload `__NUXT_DATA__` einer Produktseite enthält rund **8 600
  Einträge**, die meisten davon ein i18n-Wörterbuch.
- Eine **Atwater-Gegenprüfung** der Nährwerte ließ in einem Live-Lauf **67 von
  100** Seiten durchfallen (Alkohol, Ballaststoffe), ohne den Parserfehler, den
  sie abfangen sollte, zuverlässig zu erkennen (§4.3).
- `/api/v2/search` von Open Food Facts filtert **nicht** nach Suchbegriffen; ein
  Test für ein einzelnes Produkt lieferte den gesamten Katalog. Die Suche läuft
  deshalb über SearchaLicious.
- **SearchaLicious** bediente fünfzehn Anfragen direkt hintereinander ohne
  Ablehnung und sendet keine Rate-Limit-Header. Die früher verwendete
  Verzögerung von sechs Sekunden war das Limit des Haupthosts, nicht das von
  SearchaLicious.
- Die `robots.txt` von Rewe verbietet jeden Pfad mit Preisen.

## 2. Umfang

### 2.1 Kern

- Produktsuche mit Filtern (Kategorie, Preisspanne, Mindest-Eiweiß),
  Sortierung und tippfehlertolerantem, wortweisem Abgleich über Name, Marke und
  Kategorie; EAN-Suche durch Eingabe der Ziffern.
- Produktdetail: vollständige Nährwerte, Packungsgröße, Kilo- bzw. Literpreis,
  Preishistorie, EAN (wo vorhanden), Links zu den Quellseiten bei Aldi und Open
  Food Facts.
- Einheiten: Produkte tragen `unit` (`G` | `ML`); die Oberfläche zeigt g / kg
  bzw. ml / l und „pro 100 g" bzw. „pro 100 ml".
- Eine im Browser gehaltene Einkaufsliste mit serverseitig berechneten Summen
  und dem Hinweis „seit dem Hinzufügen günstiger / teurer" pro Zeile und für den
  gesamten Warenkorb.
- Nährwert- und Budgetziele mit Fortschrittsbalken gegenüber den Listensummen.
- Export und Import der Liste als JSON; eine Druckansicht der Liste, die sich
  über den Druckdialog auch als PDF speichern lässt.
- Preise, inkrementell von Aldi Süd gescrapt, unter Beachtung von `robots.txt`.
- Nährwerte von der Aldi-Produktseite, wo die Seite sie angibt; sonst aus Open
  Food Facts über bewertete Kandidaten und menschliches Review.
- Admin-Oberfläche mit den Tabs **„Zu prüfen"** (Filter nach Kategorie und
  Konfidenz, Freigabe pro Zeile, Sammelfreigabe über „Alle ab 90% auswählen")
  und **„Ohne Treffer"** (Open-Food-Facts-URL von Hand einfügen).
- Zeitbudget für `search-off`.
- Ein committeter Abfragesatz für die Suche mit einem automatisierten Test.
- Command `seed export`, der den Seed aus der Datenbank erzeugt.

### 2.2 Erweiterungen

In der Reihenfolge, in der sie umgesetzt werden (Details in §10):

1. **Rückgängig** einer Review-Entscheidung innerhalb einer Stunde.
2. **Tab „Katalog"** mit Ausblenden / Einblenden eines Produkts.
3. **Hybride Suche:** semantische Vektorsuche über `pgvector`, fusioniert mit
   der lexikalischen Suche; Commands `embed` und `eval-search`.
4. **Nährwerte verknüpfen:** ein Open-Food-Facts-Produkt ersetzt die Nährwerte
   der Aldi-Seite.
5. **Auslistung** von Produkten, deren Seite aus der Sitemap verschwunden ist.
6. **`eval-confidence`:** Prüfung eines embeddingbasierten Namensscores gegen
   die Review-Historie.
7. **PNG-Export** der Liste als Bild, ergänzend zur Druckansicht.

### 2.3 Nicht im Umfang

- **Andere Märkte als Aldi Süd.** Die `robots.txt` von Rewe verbietet jeden
  Pfad mit Preisen; Lidl wird nicht betrachtet. Das Datenmodell kennt keinen
  Markt.
- **Preisvergleich zwischen Märkten.** Mit nur einem Markt sinnlos.
- **Benutzerkonten, Authentifizierung und serverseitige Speicherung der Liste.**
  Die Liste lebt im Browser; gespeichert und übertragen wird sie per JSON-Export
  oder als Ausdruck bzw. PDF. Die Admin-Endpunkte sind durch einen gemeinsamen
  Schlüssel geschützt.
- **Mehrere benannte Listen.** Export und Import erfüllen diesen Zweck.
- **Rezepte, Speisepläne und mehrtägige Planung.**
- **Barcodes für Produkte aus Aldi-Quelle.** Aldi veröffentlicht keine. Eine EAN
  wird nur angezeigt, wo Open Food Facts sie liefert.
- **Bewusst ausgeschlossen:** geplante Ingestion innerhalb der API,
  Tastenkürzel im Review, ein Diagramm der Preishistorie.

## 3. Architektur

### 3.1 Module

Java 25, Spring Boot 4.1, Maven (mit Maven Wrapper) als Multi-Modul-Build.

```
foodplanner-domain          Entitäten und Value Objects (Records), reine Arithmetik.
                            Keine Abhängigkeiten.
foodplanner-application     Use Cases (Commands, Queries, Handler), Port-Interfaces, DTOs,
                            Validierungsannotationen.
foodplanner-infrastructure  PostgreSQL über JdbcClient, Flyway-Migrationen, der
                            Open-Food-Facts-Client, der Aldi-Scraper, der Nuxt-Payload-Leser.
                            Implementiert die Ports.
foodplanner-api             Spring-Boot-REST-API, OpenAPI (springdoc). Composition Root #1.
foodplanner-ingestion       Spring Boot + picocli: scrape, search-off, review, seed export.
                            Composition Root #2.
architecture-tests          ArchUnit-Regeln über alle Module.
web/                        Frontend mit React 19 + Vite + TypeScript.
```

Tests liegen nach Java-Konvention unter `src/test` des jeweiligen Moduls.

Abhängigkeiten zeigen nur nach innen: `api` und `ingestion` hängen von
`application` ab, das von `domain` abhängt. `infrastructure` hängt von
`application` ab, weil es dessen Ports implementiert, und wird ausschließlich in
den Composition Roots eingebunden.

**Spring in `application`.** Die Schicht `application` darf `spring-context`,
`spring-tx` und `jakarta.validation-api` verwenden (`@Service`,
`@Transactional`, `@Validated`). Sie darf nichts aus Spring Web, JDBC, JPA oder
einem HTTP-Client verwenden und weiß nichts von PostgreSQL, HTTP oder Open Food
Facts. Diese Grenze ist eine ArchUnit-Regel.

**Ports:** `ProductRepository`, `ReviewRepository`, `ScrapedPageRepository`,
`OffCatalog`, `PriceScraper`, `ProductSearch`. Die Suche läuft von Anfang an
über den Port `ProductSearch`, zunächst mit einer rein lexikalischen
Implementierung; die hybride Suche (§10.3) ist später eine zweite
Implementierung, ohne dass sich der Such-Handler ändert.

### 3.2 Use Cases

Die Schicht `application` ist in vertikale Slices gegliedert:

```
application/products/search/        SearchProductsQuery, SearchProductsHandler
application/products/getbyid/
application/products/batch/
application/products/categories/
application/catalog/info/
application/shoppinglist/summarise/
application/review/blocks/          (auch Kategorien mit Anzahl)
application/review/unmatched/
application/review/choose/
application/review/bulkchoose/
application/review/reject/
application/review/linkmanually/
```

Jeder Use Case besteht aus einem Record für die Eingabe und einer
Handler-Klasse mit einer Methode `handle(...)`. Endpunkte und
Ingestion-Commands rufen den Handler direkt auf; es gibt keinen Mediator und
kein gemeinsames Handler-Interface.

**Validierung** liegt nie im Endpunktcode. Die Eingabe-Records tragen
Bean-Validation-Annotationen (`@Min`, `@Max`, `@Size`, `@NotNull` …), der
Handler ist `@Validated`, und Spring prüft die Eingabe, bevor die Logik läuft.
Damit gilt dieselbe Validierung für die API und die Konsole.

Die **Sammelfreigabe** ruft `ChooseCandidateHandler` pro Paar auf. Sie kann sich
daher nicht von der Einzelfreigabe darin unterscheiden, was eine Freigabe ist.

### 3.3 Zwei Composition Roots

Die Ingestion-CLI ist ein eigenes Programm und kein Hintergrunddienst innerhalb
der API. Deshalb hängt die API zur Anfragezeit nie von der Erreichbarkeit von
openfoodfacts.org oder aldi-sued.de ab, Hintergrundjobs können die Tests nicht
stören, und die Demo läuft gegen eine bereits befüllte Datenbank.

Die API löst bewusst auch `OffCatalog` auf: Das manuelle Einfügen einer
Open-Food-Facts-URL im Tab „Ohne Treffer" braucht eine Live-Abfrage per Barcode.
Sonst verlässt im Anfragepfad nichts die Maschine.

## 4. Domänenmodell

### 4.1 Persistenz

Die Datenbank wird über Spring `JdbcClient` mit handgeschriebenem SQL
angesprochen; das Schema verwaltet Flyway. Es gibt kein ORM. Zeilen werden
direkt auf die Records aus `domain` abgebildet, sodass es nur eine
Darstellung jeder Entität gibt und `domain` frei von Persistenz-Annotationen
bleibt. Die Suche besteht ohnehin aus nativem SQL (`pg_trgm`).

Spalten, die nur eine Erweiterung braucht, kommen mit der Flyway-Migration
dieser Erweiterung. Das Kern-Schema enthält keine ungenutzten Spalten.

### 4.2 Entitäten

Felder in Java-Schreibweise; die Spalten heißen entsprechend in snake_case.

```
Product       id, offCode (EAN, nullable, eindeutig), aldiSku (nullable, eindeutig),
              name, brand, category, packageGrams, unit (G | ML),
              per100g (kcal, protein, fat, carbs),
              source (ALDI | OPENFOODFACTS | SEED)
              Generierte Spalten: search_name, search_brand, search_category
              = immutable_unaccent(lower(…))

Price         id, productId, priceCents, sourceName, scrapedAt, isCurrent

ScrapedItem   id, rawName, priceCents, url, scrapedAt, category, imageUrl,
              searchState, aldiSku, kcal100, protein100, fat100, carbs100,
              packageGrams, unit, nutritionBasis

OffCandidate  id, scrapedItemId, offCode, name, brand, category, packageGrams,
              packageGramsFromScrapedName, imageUrl, protein/fat/carbs/kcal pro 100 g,
              confidence (0..1), rank (1..5), isManual,
              status (PENDING | APPROVED | REJECTED), decidedAt

ScrapedPage   id, url, lastModified, fetchedAt, productId (nullable)
```

`SearchState` ist `NOT_SEARCHED | SEARCHED | NO_MATCH | RESOLVED | NOT_FOOD`.
`RESOLVED` bedeutet, dass aus der Zeile bereits ein Katalogprodukt entstanden
ist und sie keine Review-Arbeit ist; `NOT_FOOD` steht für eine konfigurierte
Non-Food-Kategorie, die gar nicht erst gesucht wird.

Das Review ist pro gescrapter Zeile organisiert: `OffCandidate`-Zeilen hängen an
einem `ScrapedItem`. Status und `decidedAt` der Kandidaten sind die
Aufzeichnung jeder Entscheidung: Eine Freigabe setzt den gewählten Kandidaten
auf `APPROVED` und die übrigen auf `REJECTED`; „keiner passt" setzt alle
Kandidaten der Zeile auf `REJECTED`. Ein abgelehnter Block ist dadurch von einem
unbearbeiteten unterscheidbar.

**`productKey`** ist der stabile, rechnerübergreifende Schlüssel eines Produkts:
`aldi:<aldiSku>`, wenn das Produkt eine Artikelnummer hat, sonst
`off:<offCode>`. Jedes Produkt hat mindestens eins von beiden. Datenbank-IDs
sind nur innerhalb einer Datenbank gültig; nach einem Neuaufbau auf einem
anderen Rechner könnte dieselbe ID ein anderes Produkt bezeichnen. Alles, was
die Datenbank überlebt — die Liste im Browser, exportierte Dateien, der
Abfragesatz der Suche —, referenziert Produkte deshalb über `productKey`.

### 4.3 Datenfluss

Ein Produkt gelangt auf genau zwei Wegen in den Katalog.

1. **Die Aldi-Seite beschreibt das Produkt selbst.** `scrape aldi` liest
   Artikelnummer, Einheit und die vier Makronährstoffe aus dem Nuxt-Payload der
   Seite. Eine Zeile mit allen vier Werten, einer Packungsgröße und einer
   Artikelnummer wird sofort und ohne Review zu einem `Product` mit
   `source = ALDI` samt `Price`: Es gibt nichts abzugleichen, weil Nährwerte und
   Preis von derselben Seite für denselben Artikel stammen. Die Zeile wird als
   `RESOLVED` markiert.
2. **Ein Mensch gibt einen Kandidaten aus Open Food Facts frei.** Eine Zeile,
   die die Seite nicht beschrieben hat, sucht `search-off` in Open Food Facts;
   bis zu fünf bewertete `OffCandidate`-Zeilen werden gespeichert. Die Auswahl
   eines Kandidaten — in `/admin` oder in der Review-Schleife der Konsole —
   erzeugt in einem Schritt das `Product` (`source = OPENFOODFACTS`) und seinen
   `Price`. Eine Ablehnung markiert die Zeile als entschieden und erzeugt
   nichts.

Eine gescrapte Zeile für sich ist nirgends sichtbar. In öffentlichen
Suchergebnissen erscheinen nur Produkte mit mindestens einem aktuellen `Price`.
Ein falscher automatischer Treffer kann das Schaufenster deshalb nie erreichen.

### 4.4 Invarianten

- **Geld wird als ganzzahlige Cent (`long`) gespeichert.** Im Preispfad gibt es
  weder `double` noch `BigDecimal`, daher enthalten Listensummen keine
  Rundungsfehler.
- **Nährwerte sind `BigDecimal`** und werden erst an der API-Grenze auf eine
  Nachkommastelle gerundet.
- **Ein Produkt ohne Packungsgröße kann nicht angelegt werden.** Die Freigabe
  liefert `REFUSED_NO_PACKAGE_SIZE`, und der Aldi-Pfad überspringt die Zeile.
  Ohne Packungsgröße wären die Nährwertsummen unbemerkt falsch.
- **Alle vier Makronährstoffe oder keiner.** Unvollständige Nährwerte werden aus
  keiner Quelle gespeichert.
- **`quantity` in einer Listenzeile bedeutet ganze Packungen, nicht Gramm.**
  Nährwerte sind `quantity × packageGrams × per100g / 100`.
- **Die Freigabe ist idempotent.** Die Auswahl eines bereits entschiedenen
  Kandidaten liefert `ALREADY_DECIDED` und erzeugt nichts.
- **Identitäten kollidieren nie.** Ein Produkt aus Aldi-Quelle ist über
  `aldiSku` (eindeutig) identifiziert, ein Produkt aus Open Food Facts über
  `offCode` (eindeutig). Gehört der `offCode` eines Kandidaten bereits einem
  Produkt, liefert die Freigabe `REFUSED_OFF_CODE_TAKEN`, erzeugt nichts und
  lässt den Kandidaten `PENDING`.
- **Ein Preis wird abgelöst, nie überschrieben.** Wird derselbe Artikel zu einem
  neuen Preis gescrapt, wird der alte `Price` auf `isCurrent = false` gesetzt
  und ein neuer eingefügt. Das macht die Preishistorie möglich. Bei
  unverändertem Preis entsteht kein neuer `Price`; nur `scrapedAt` des
  aktuellen `Price` wird auf den neuen Scrape gesetzt, damit `lastScrapedAt`
  (§5.1) den letzten Scrape zeigt.
- **Ein eingehendes null löscht nie einen gespeicherten Wert.** Eine einzige
  Methode (`refreshScrapedFields`) darf auf eine bereits gespeicherte gescrapte
  Zeile schreiben, und sie schreibt ein Feld nur, wenn der eingehende Wert
  vorhanden und anders ist. `searchState`, Preis und `scrapedAt` fasst sie nie
  an.
- **Einheiten gehen nie in die Arithmetik ein.** `unit` dient nur der Anzeige;
  die Konvention ml-als-g aus §6.1 hält die Summen korrekt.

### 4.5 Plausibilität

`NutritionPlausibility` verwirft kcal außerhalb von 0…900, jeden
Makronährstoff außerhalb von 0…100 und eine Makrosumme über 105 g pro 100 g.
Eine Zeile, die durchfällt, wird behandelt, als hätte sie keine
Aldi-Nährwerte, und landet im Open-Food-Facts-Pfad, wo ein Mensch sie sieht.

Eine Atwater-Gegenprüfung (`4×Eiweiß + 9×Fett + 4×Kohlenhydrate` gegen die
angegebenen kcal) gibt es **bewusst nicht** (§1.1): Die Atwater-Faktoren
berücksichtigen weder Alkohol noch Ballaststoffe, daher fiel im Prototyp jedes
Bier, jeder Wein, jede Spirituose und jedes ballaststoffreiche Lebensmittel
durch, während der Parserfehler, den sie abfangen sollte, eine Abweichung
erzeugte, die sich nicht von echtem Backkakao unterscheiden ließ. Der exakte
Abgleich der Bezeichnungen (§6.1) verhindert diesen Fehler schon vorher und
wird durch einen Test abgesichert, der die Reihenfolge der Tabellenzeilen
umkehrt.

### 4.6 Speicherung der Einkaufsliste

Die Einkaufsliste wird nie auf dem Server gespeichert. Sie liegt im
`localStorage` des Browsers; jede Zeile enthält
`{ productKey, quantity, name, priceAtAddCents, addedAt }`, dazu optionale
Ziele (Format in §7.2).

`priceAtAddCents` und `addedAt` halten den Preis zum Zeitpunkt fest, an dem die
Zeile angelegt wurde. Eine Mengenänderung behält sie bei; wird das Produkt
entfernt und erneut hinzugefügt, werden sie neu gesetzt. Alle Felder sind
Pflicht: Hinzufügen lässt sich nur ein Produkt aus der Suche, und dort hat jedes
Produkt einen aktuellen Preis. Die für den Nutzer sinnvolle Frage lautet „hat
sich das geändert, seit ich es auf die Liste gesetzt habe".

Der gespeicherte `name` existiert, damit eine Zeile, deren Produkt den Katalog
verlassen hat, weiterhin mit Namen statt als nackter Schlüssel angezeigt wird.

Der Client lädt Produktdaten über `POST /products/batch` nach und erhält die
Summen über `POST /shopping-list/summary`. Beide Endpunkte sind reine
Funktionen ihres Request-Bodys und halten keinen Zustand.

## 5. API-Vertrag

Basispfad `/api/v1`. Alle Fehler werden als `ProblemDetail` nach RFC 9457
zurückgegeben (Spring 6). Ein `@RestControllerAdvice` übersetzt
Validierungsfehler — sowohl Bindungsfehler des Controllers als auch
`ConstraintViolationException` aus `@Validated`-Handlern — in **400** mit einem
Objekt `errors`, geschlüsselt nach Feldname. Enums werden als Strings
serialisiert; Werte wie `price_asc` werden über einen Konverter in snake_case
abgebildet.

### 5.1 Öffentliche Endpunkte

```
GET  /api/v1/products/search
       ?q= &category=
       &minPriceCents= &maxPriceCents= &minProtein100g=
       &sort=relevance|price_asc|price_desc|protein_desc|kcal_asc
       &page=1 &pageSize=20              // page 1..100000, pageSize 1..100
  → 200 PagedResult<ProductSummary>
  → 400 unbekannter sort-Wert oder minPriceCents > maxPriceCents
       // ein q aus genau 8 oder 13 Ziffern ist eine EAN-Suche

GET  /api/v1/products/{id}
  → 200 ProductDetail | 404

POST /api/v1/products/batch
       { "keys": ["aldi:1234567", "off:4008400401211"] }   // höchstens 200
  → 200 ProductDetail[]                  // unbekannte Schlüssel fehlen in der Antwort

GET  /api/v1/categories                  → 200 string[]
GET  /api/v1/catalog/info                → 200 { productCount, lastScrapedAt }
GET  /health                             → 200 (Spring Boot Actuator)
```

`/products/{id}` nutzt die interne ID, weil Detail-URLs nur innerhalb einer
laufenden Instanz verwendet werden. Alles, was gespeichert wird, nutzt
`productKey`.

`GET /products/{id}` und `POST /products/batch` prüfen bewusst **nicht** auf
einen aktuellen Preis: Die Browser-App lädt eine gespeicherte Einkaufsliste
über den Batch-Endpunkt und würde brechen, wenn sich der Zustand eines Produkts
darunter ändert.

`productCount` zählt die in der Suche sichtbaren Produkte. `lastScrapedAt` hält
die Aktualität der Preise sichtbar: Es ist das größte `scrapedAt` der aktuellen
Preise (`isCurrent`) dieser Produkte, bei leerem Katalog `null`.

Der Health-Endpunkt ist Spring Boot Actuator, auf den Pfad `/health` gelegt.

### 5.2 Listenzusammenfassung

```
POST /api/v1/shopping-list/summary
  {
    "items": [ { "productKey": "aldi:1234567", "quantity": 2,
                 "priceAtAddCents": 129, "addedAt": "2026-11-20T10:00:00Z" } ],
             // ≤ 200 Einträge, quantity 1..1000, priceAtAddCents 1..100000,
             // alle Felder Pflicht
    "goals": { "protein": 120, "fat": 70, "carbs": 250,
               "kcal": 2000, "budgetCents": 4000 }     // nullable
  }
  → 200 {
    "lines": [ { "productKey", "name", "quantity", "unitPriceCents", "lineTotalCents",
                 "protein", "fat", "carbs", "kcal",
                 "protein100g", "fat100g", "carbs100g", "kcal100g", "unit",
                 "priceAtAddCents", "addedAt", "priceChangeCents" } ],
    "totals": { "priceCents", "protein", "fat", "carbs", "kcal",
                "priceDifferenceCents" },
    "goalProgress": { "proteinPct", "fatPct", "carbsPct",
                      "kcalPct", "budgetPct" },        // null, wenn goals null ist
    "missingProductKeys": [ ]
  }
```

`priceChangeCents = unitPriceCents − priceAtAddCents`.
`priceDifferenceCents` ist die Summe von `priceChangeCents × quantity` über alle
Zeilen. Produkte, die nicht mehr im Katalog sind, erscheinen in
`missingProductKeys` und nicht in `lines`. Als nicht mehr im Katalog gilt auch
ein Produkt ohne aktuellen `Price` — dieselbe Sichtbarkeitsregel wie in der
Suche.

Die Summen werden serverseitig berechnet, damit die gesamte Geld- und
Nährwertarithmetik in der unit-getesteten Schicht `domain` liegt und nicht in
JavaScript.

### 5.3 Admin-Endpunkte

```
GET  /api/v1/admin/review?page=1&pageSize=20&category=&minConfidence=
  → 200 PagedResult<ReviewBlock>         // Zeilen mit offenen Kandidaten, bester Score zuerst

GET  /api/v1/admin/review/categories     → 200 CategoryCount[]  (category, count)

POST /api/v1/admin/review/{itemId}/choose
       { "candidateId": 88 }
  → 204   gewählt oder bereits entschieden (idempotent)
  → 409   Kandidat hat keine Packungsgröße — kann kein Preis werden
  → 409   `offCode` des Kandidaten gehört bereits einem Produkt
  → 404   Kandidat für diese gescrapte Zeile nicht gefunden

POST /api/v1/admin/review/bulk-choose
       { "pairs": [ { "scrapedItemId": 12, "candidateId": 88 } ] }
  → 200 { approvedCount, refusedCount, outcomes[] { scrapedItemId, candidateId, result } }

POST /api/v1/admin/review/{itemId}/none  → 204   (alle Kandidaten REJECTED, kein Produkt)
                                         → 404   Zeile nicht gefunden

GET  /api/v1/admin/unmatched?page=1&pageSize=20   → 200 PagedResult<UnmatchedRow>

POST /api/v1/admin/unmatched/{itemId}/link
       { "url": "https://world.openfoodfacts.org/product/4008400401211" }
  → 204   lädt das Produkt per Barcode und speichert es als manuellen Kandidaten
  → 400   url ist keine Produkt-URL von openfoodfacts.org
  → 404   kein Open-Food-Facts-Produkt zu diesem Barcode, oder die Zeile selbst fehlt
  → 409   das Produkt hat keine Nährwertdaten
```

`ReviewBlock` enthält die gescrapte Zeile (Rohname, Preis, Aldi-Quell-URL, Bild)
und bis zu fünf Kandidaten, jeweils mit eigener Konfidenz, Nährwerten, Bild und
Open-Food-Facts-URL. `UnmatchedRow` hat dieselbe Zeilenform ohne Kandidaten.

Die **Sammelfreigabe** ist kein blinder Ein-Klick-Durchlauf: Die
Admin-Oberfläche setzt nur beim bestplatzierten Kandidaten jedes Blocks eine
Checkbox, „Alle ab 90% auswählen" hakt die geeigneten an, der Bearbeiter kann
jeden davon wieder abwählen, und „Auswahl bestätigen" sendet die Paare. Das
Ergebnis jedes Paares wird einzeln gemeldet.

Jeder Admin-Endpunkt liegt hinter einem `OncePerRequestFilter` auf
`/api/v1/admin/**`, der den Header `X-Admin-Key` in konstanter Zeit
(`MessageDigest.isEqual`) mit `admin.key` vergleicht. Ein nicht gesetzter
Schlüssel ergibt **503** (Admin-Zugang ist nicht konfiguriert, also ist nichts
erreichbar); ein falscher oder fehlender Header ergibt **401**. Spring Security
wird dafür nicht eingesetzt.

### 5.4 DTO-Formen

Alle DTOs sind Java-Records.

```
ProductSummary   id, productKey, name, brand, category, packageGrams, unit,
                 priceCents, protein100g, fat100g, carbs100g, kcal100g

ProductDetail    ProductSummary + offCode, pricePerKgCents,
                 aldiDepartment, aldiUrl, offUrl,
                 priceHistory[] { priceCents, scrapedAt, isCurrent }

PagedResult<T>   items[], page, pageSize, totalCount, totalPages

ReviewBlock      scrapedItemId, rawName, priceCents, sourceUrl, imageUrl, candidates[]
ReviewCandidate  id, offCode, name, brand, packageGrams, packageGramsFromScrapedName,
                 confidence, offUrl, protein100g, fat100g, carbs100g, kcal100g, imageUrl
UnmatchedRow     scrapedItemId, rawName, priceCents, sourceUrl
```

`pricePerKgCents` ist bei `unit = ML` der Literpreis. `priceHistory` wird nur im
Detailpfad befüllt (die Suche lädt bewusst nur die aktuellen Preise) und ist
leer, wenn das Produkt keinen abgelösten Preis hat.

### 5.5 Umsetzung der Suche

Die Suche läuft über den Port `ProductSearch`.

*Routing.*

- Eine Anfrage aus genau 8 oder 13 Ziffern ist eine EAN-Suche: exakter Abgleich
  auf `offCode`, sonst läuft nichts.
- Eine leere Anfrage führt nur die Filter aus, sortiert nach Name.
- Alles andere ist eine lexikalische Suche.

*Faltung.* Die generierten Spalten `search_name`, `search_brand` und
`search_category` enthalten `immutable_unaccent(lower(…))`, jeweils mit eigenem
GIN-Trigramm-Index. Die Anfrage wird in Java genauso gefaltet, sodass `caffe`
ohne Aufruf von `unaccent()` zur Abfragezeit `Caffè` findet. Weil Java
(`Normalizer` + Entfernen der Diakritika) und PostgreSQL (`unaccent`) nicht von
selbst identisch falten (z. B. `ß`), prüft ein Test die Java-Faltung gegen
`unaccent` über alle Zeichen, die im Katalog vorkommen.

*Lexikalische Suche.*

- Die gefaltete Anfrage wird an Leerzeichen und Satzzeichen in Tokens zerlegt.
  **Jedes Token muss treffen**, in beliebiger Reihenfolge, sodass
  `milch fettarm` die `Fettarme Milch 1,5 %` findet.
- Ein Token trifft, wenn es gegen `search_name`, `search_brand` oder
  `search_category` die `word_similarity`-Schwelle von 0,4 erreicht (Operator
  `<%`).
- `word_similarity` bewertet gegen einen Ausschnitt des Ziels, sodass ein Token
  auch innerhalb eines deutschen Kompositums trifft (`schokolade` in
  `Vollmilchschokolade`).
- Die Schwelle wird pro Verbindung über `connectionInitSql` des HikariCP-Pools
  gesetzt (`SET pg_trgm.word_similarity_threshold = 0.4`), weil der
  Datenbank-Standard bei gepoolten Verbindungen nicht verlässlich ist und ein
  Setzen pro Abfrage vergessen werden kann.
- Der Score eines Produkts ist die Summe über alle Tokens des jeweils besten
  Feldscores, gewichtet mit Name 1,0, Marke 0,6 und Kategorie 0,4, plus
  Stufenboni für ein exaktes Wort und einen Wortanfang.

*Filter* (Kategorie, Preisspanne, Mindest-Eiweiß, aktueller Preis) stehen in
derselben SQL-Abfrage, nicht danach.

*Sortierung.* `sort=relevance` sortiert nach dem Score und ist auf insgesamt 200
Treffer begrenzt, aus denen die Seiten geschnitten werden. Die anderen
Sortierungen sortieren dieselbe Treffermenge nach ihrem Feld.

*Abfragesatz.* `data/search-eval/queries.json` enthält deutsche Anfragen, ihre
Gruppe (Tippfehler, Komposita, Synonyme, englische Wörter, Absichtsanfragen,
einfache exakte Namen) und die erwarteten Produkte, geschlüsselt über
`productKey`. Ein parametrisierter Test prüft die lexikalische Suche und die
Schwelle 0,4 pro Token dagegen. Der Satz wird geschrieben, bevor Ergebnisse
angesehen werden; ein an die Ausgabe angepasster Satz misst nichts. Gruppen, die
nur die Vektorsuche lösen kann (Synonyme, Absichtsanfragen), sind im Kern als
erwartete Fehlschläge markiert und werden mit §10.3 scharf geschaltet.

## 6. Ingestion-Pipeline

Die Ingestion ist eine Spring-Boot-Anwendung mit picocli-Subcommands. Die drei
Pipeline-Commands laufen der Reihe nach, und jeder arbeitet auf dem Zustand, den
der vorherige in der Datenbank hinterlassen hat, statt Dateien weiterzureichen.

```
java -jar foodplanner-ingestion.jar scrape aldi [--fetch] [--full]
java -jar foodplanner-ingestion.jar search-off
java -jar foodplanner-ingestion.jar review
java -jar foodplanner-ingestion.jar seed export
```

In der Entwicklung alternativ über
`./mvnw -pl foodplanner-ingestion spring-boot:run -Dspring-boot.run.arguments="scrape aldi"`
(nach einmaligem `./mvnw install -DskipTests`, damit die Nachbarmodule im lokalen
Repository liegen).

Gemeinsame Infrastruktur: HTTP über den eingebauten `java.net.http.HttpClient`;
Nebenläufigkeit über Virtual Threads mit einem `Semaphore`; alle Wartezeiten und
Zeitbudgets laufen über eine injizierte `Clock` und einen injizierten Sleeper,
damit Tests sie ohne echte Wartezeit prüfen können. Der `User-Agent` für Aldi
und Open Food Facts ist eine einzige Einstellung (`http.user-agent`), die API
und Ingestion gemeinsam lesen; beide starten nur, wenn er eine echte
Kontaktadresse enthält.

Fehlgeschlagene Anfragen werden bei `IOException`, HTTP 429 und 5xx bis zu
dreimal wiederholt, mit 1 s, 2 s und 4 s Wartezeit über den injizierten
Sleeper; ein `Retry-After`-Header hat Vorrang, andere 4xx-Antworten sind sofort
ein Fehler. Die Werte sind konfigurierbar.

### 6.1 `scrape aldi`

Der Scraper liest zuerst `robots.txt` (crawler-commons,
`SimpleRobotRulesParser`) und bricht den Lauf ab, wenn die Sitemap oder der
Produktpfad verboten ist. Ohne `--fetch` liest er den neuesten committeten
Snapshot aus `data/snapshots/aldi-prices-*.json`; mit `--fetch` durchläuft er
`sitemap_products.xml` (crawler-commons, `SiteMapParser`) und lädt die
Produktseiten selbst.

Der Abruf ist begrenzt und rücksichtsvoll:

- `ingestion.aldi-max-products` (Standard 100, begrenzt auf 1…10 000) deckelt,
  wie viele Sitemap-Einträge betrachtet werden, sodass ein Tippfehler keinen
  unbegrenzten Crawl starten kann. Der Demo-Katalog wird mit **300** erzeugt.
- `ingestion.scrape-concurrency` (Standard 4) Anfragen laufen parallel, hinter
  einem Request-Gate, das vor jede Anfrage seriell
  `ingestion.scrape-delay-seconds` (Standard 0,2 s) setzt. So bleibt die
  Gesamtrate bei etwa 1/Verzögerung, statt Bursts abzufeuern.
- **Inkrementell nach `lastmod`.** `ScrapedPage` merkt sich zu jeder URL
  `lastmod` und den letzten Abruf; eine Seite, deren `lastmod` sich nicht
  bewegt hat, wird übersprungen. `--full` lädt alles neu — der Notausgang für
  eine Seite, die sich geändert hat, ohne dass ihr `lastmod` sich bewegt hat.

Aus jeder Seite werden mit jsoup der ld+json-Block `Product` (Name mit Marke,
Preis, URL), die `BreadcrumbList` (Kategorie), das Meta-Tag `og:image` und der
Nuxt-Payload `__NUXT_DATA__` gelesen; aus Letzterem kommen Artikelnummer,
Nährwerttabelle und Packungsgröße.

Das Lesen des Payloads ist ein Durchlauf durch einen devalue-Graphen (als
Jackson-`JsonNode`) mit drei Fallen, die der Leser explizit behandelt:

- Ein numerischer Eintrag ist ein Literal, kein Index.
- Wrapper-Einträge (`Reactive`, `Ref`, `ShallowReactive`, `EmptyRef`,
  `skipHydrate`, `Set`, `NuxtError`) verweisen über ihr Element 1 weiter.
- Der Graph enthält Zyklen.

Aufgelöst wird nur der Teilbaum `$spdp-product-<sku>`.

Nährwerte werden nur akzeptiert, wenn die Basis `pro 100 g` oder `pro 100 ml`
ist, und die vier Makronährstoffe werden über **exakte** Gleichheit der
Bezeichnung zugeordnet (`Energie [kcal]`, `Fett`, `Kohlenhydrate`, `Eiweiß`),
nie über ein Präfix — ein Präfixabgleich würde unbemerkt
`Fett, davon gesättigte Fettsäuren` übernehmen.

Die Mengen sind deutsch formatiert (`2.342 kJ`, `33,8 g`) und werden mit
`NumberFormat.getInstance(Locale.GERMANY)` **strikt** geparst: Über eine
`ParsePosition` wird geprüft, dass der gesamte Zahlenteil verbraucht wurde, weil
der Standardparser einen gültigen Präfix stillschweigend akzeptiert.

**Einheiten.** Milliliter werden als Gramm behandelt, Liter als Kilogramm. Der
Fehler hebt sich auf: Bei einem Produkt mit Nährwerten pro 100 ml und einer
Größe in Millilitern stimmt die Summe, weil auf beiden Seiten der
Multiplikation dieselbe Einheit steht. Open Food Facts macht dieselbe
Gleichsetzung. Die tatsächliche Einheit wird in `unit` festgehalten: `ML`, wenn
die Nährwertbasis `pro 100 ml` oder die Vergleichseinheit des Preises `/1 l`
ist, sonst `G`. Das Preispaar steht auch auf Seiten ohne Nährwerttabelle, sodass
auch Produkte aus dem Open-Food-Facts-Pfad ihre Einheit von Aldi erhalten.

**Packungsgröße**, genaueste Quelle zuerst: aus dem Preispaar
(`Betrag / Vergleich × 1000`, wenn die Vergleichseinheit `/1 kg` oder `/1 l`
ist), dann `sellingSize`, dann die Größe im gescrapten Namen. Eine Größe unter
1 g oder über 50 000 g wird verworfen und die nächste Quelle versucht. Die
Rechnung aus dem Preispaar läuft in `BigDecimal` und wird auf ganze Gramm
gerundet (`RoundingMode.HALF_UP`).

Jede Zeile wird anschließend validiert (Preis 1…100 000 Cent, nichtleerer Name
mit höchstens 300 Zeichen, URL mit höchstens 1000), und Zeilen in einer
Kategorie aus `ingestion.non-food-categories` werden als `NOT_FOOD` markiert,
damit sie nie gesucht werden.

**Schreiben.** Beim Speichern trennen sich die beiden Katalogwege aus §4.3:

- Eine Zeile, deren Seiten-URL bereits an ein Produkt gebunden ist, erfasst
  einen neuen Preis für dieses Produkt, statt Review-Arbeit einzureihen.
- Eine Zeile mit vollständigen Nährwerten, Artikelnummer und Packungsgröße legt
  das Produkt per SKU an oder findet es und erfasst seinen Preis.
- Alles andere wird für das Review eingereiht.

**Snapshots** werden in camelCase geschrieben. Neben den Seiten enthalten sie die
vollständige Liste der Sitemap-URLs und die Abdeckung des Laufs (wie viele
Einträge betrachtet wurden). Der Kern braucht diese Angaben nicht; ohne sie
wären die committeten Snapshots für die Auslistung (§10.5) unbrauchbar.

### 6.2 `search-off`

Für jede Aldi-Zeile, die noch `NOT_SEARCHED` ist: den Rohnamen normalisieren,
Open Food Facts über **SearchaLicious**
(`https://search.openfoodfacts.org/search?q=`) abfragen, jeden Treffer mit
`MatchConfidence` bewerten und die fünf besten über der Verwerfungsschwelle als
`OffCandidate`-Zeilen mit Rang speichern. Eine Zeile ohne Treffer über der
Schwelle wird `NO_MATCH` und erscheint im Tab „Ohne Treffer" statt in „Zu
prüfen". Hat eine Zeile nichts erreicht und ließ sich aus ihrem Namen eine Marke
extrahieren, wird als zweite Chance nur nach der Marke gesucht; so gerettete
Zeilen werden in der Logzeile des Laufs separat gezählt.

Abfragen per Barcode gehen an den Haupthost (`world.openfoodfacts.org`); nur die
Suche geht an SearchaLicious, weil `/api/v2/search` nicht nach Suchbegriffen
filtert (§1.1).

Der Command hat keinen Offline-Modus: Für die Suche nach Namen gibt es keinen
lokalen Cache. Er verarbeitet `ingestion.search-concurrency` Zeilen parallel
(Standard 4), hinter demselben Request-Gate wie der Scraper, mit
`ingestion.search-delay-seconds` (Standard 0,25 s) zwischen den Anfragen. Der
Wert stützt sich auf die Messung aus §1.1.

**Zeitbudget.** `ingestion.search-time-budget-minutes` begrenzt einen Lauf. Ist
es abgelaufen, wird keine neue Zeile mehr begonnen, laufende Anfragen werden
abgeschlossen, und jede unbearbeitete Zeile bleibt für den nächsten Lauf
`NOT_SEARCHED`. Die Logzeile meldet, wie viele Zeilen übrig geblieben sind.

Eine Zeile, deren Suche eine Ausnahme wirft, bleibt `NOT_SEARCHED`, damit ein
späterer Lauf sie aufgreift: Ein Fehlschlag sagt nichts aus und darf nicht als
`NO_MATCH` erfasst werden. Die Rohantworten werden in einem `finally`-Block nach
`data/snapshots/off-search-*.json` geschrieben und mit dem zusammengeführt, was
ein früherer Lauf am selben Tag bereits geschrieben hat. So behält auch ein
abgebrochener oder durch das Zeitbudget beendeter Lauf jede Antwort.

**Konfidenz.** Eine gewichtete Summe, renormalisiert über die Komponenten, die
sich tatsächlich vergleichen lassen — ein Feld, das auf einer Seite fehlt, wird
aus Summe und Divisor ausgeschlossen, statt null zu zählen:

| Komponente | Gewicht |
|---|---|
| Jaro-Winkler-Ähnlichkeit der normalisierten Namen (Apache Commons Text) | 0,60 |
| Übereinstimmung der Marke | 0,25 |
| Übereinstimmung der Packungsgröße (absolut, ±1 g) | 0,15 |

Über dem Rohscore liegen zwei Korrekturen:

- **Widersprüchliche Zusätze.** Hat jede Seite mindestens ein Wort, das der
  anderen fehlt, beschreiben die Namen unterschiedliche Varianten, und der
  Namensscore wird mit 0,55 multipliziert. Jaro-Winkler gewichtet ein
  gemeinsames Präfix so stark, dass „Sandwichtoast Saaten-Harmonie" und
  „Sandwichtoast - Dinkel" sonst über 0,86 kämen. Ein bloß *kürzerer* Name ist
  kein Widerspruch.
- **Generische Einzelwörter.** Reduziert sich der gescrapte Name auf ein kurzes
  Wort und lässt sich keine Marke vergleichen, wird die Summe auf 0,90
  gedeckelt, damit sie den Bereich der Sammelfreigabe nicht erreicht. Open Food
  Facts ist voll von Dokumenten namens „Pils" oder „Riesen".

| Konfidenz | Ergebnis |
|---|---|
| `< 0,55` | Verworfen, nie als Kandidat gespeichert |
| `≥ 0,55` | Als einer von bis zu fünf Kandidaten gespeichert, nach Score gereiht |
| `> 0,90` | Zusätzlich für „Alle ab 90% auswählen" in Frage kommend |

Die Toleranz von einem Gramm ist absichtlich absolut und kein Prozentsatz: Die
Packungsgrößen dieser Quellen sind auf beiden Seiten meist auf dieselbe Einheit
gerundet.

### 6.3 `review`

Eine Konsolenschleife über Zeilen, die auf eine Entscheidung warten, geführt
über dieselben Handler wie die `/admin`-Endpunkte. So können sich die beiden
Oberflächen nicht darin unterscheiden, was als Freigabe zählt. Die
React-Admin-Oberfläche ist die primäre Schnittstelle; die Konsolenschleife
existiert für das Review ohne laufende Web-App (und für die Zeit, bevor die
Admin-Oberfläche existiert, §13) und bietet nur auswählen / none / überspringen
/ beenden.

### 6.4 `seed export`

Schreibt alle in der Suche sichtbaren Produkte samt aktuellem Preis und
Preishistorie nach `data/seed/products.json`. Der Seed-Loader der API liest
diese Datei beim ersten Start in eine leere Datenbank. Produkte werden im Seed
über `productKey` identifiziert, nicht über ihre ID.

Die Datei hat die Form `{ "version": 1, "products": [ … ] }` in camelCase,
eingerückt, Produkte nach `productKey` und Preishistorie nach `scrapedAt`
sortiert, ohne Exportzeitpunkt: Ein erneuter Export ohne Datenänderung erzeugt
eine byte-identische Datei und damit keinen Diff. Der Seed-Loader prüft
`version`.

## 7. Frontend

React 19, Vite, TypeScript, TanStack Query für den API-Zustand, Tailwind 4 für
das Styling, React Router für die Navigation, zod zum Parsen von allem, was aus
`localStorage` oder einer Datei kommt. Die Oberfläche ist auf Deutsch.

Die TypeScript-Typen der API werden mit `openapi-typescript` aus der
springdoc-Spezifikation generiert. Ändert sich ein DTO in Java, baut das
Frontend nicht mehr, statt still vom Backend abzuweichen. Der Maven-Build der
API exportiert die Spezifikation nach `web/openapi.json`, die committet wird;
ein Test schlägt fehl, wenn die Datei veraltet ist. Das Frontend generiert die
Typen aus dieser Datei und baut damit ohne laufende API.

### 7.1 Ansichten

1. `/` — Suche. Eingabe mit 300 ms Debounce, Filterleiste (Kategorie,
   Preisspanne, Mindest-Eiweiß, Sortierung), Kartenraster, Seitennavigation,
   „zur Liste hinzufügen" auf jeder Karte. Dasselbe Eingabefeld akzeptiert eine
   EAN. Unter dem Suchfeld stehen die Zahl der Produkte und das Datum der
   Preise aus `GET /catalog/info` (§11).
2. `/product/:id` — Detail: vollständige Nährwerte „pro 100 g" bzw. „pro
   100 ml", Packungsgröße in g / kg bzw. ml / l, Kilo- bzw. Literpreis, aktueller
   Preis, Preishistorie als Tabelle, die EAN (wo vorhanden), die Aldi-Abteilung,
   Links zur Aldi-Seite und zur Open-Food-Facts-Quelle. Eine Komponente bedient
   sowohl die aus dem Raster geöffnete Ansicht als auch den Direktlink.
3. `/list` — die Einkaufsliste: Zeilen mit Mengenschaltern, Nährwerte pro 100 g
   bzw. 100 ml pro Zeile, der Hinweis „seit dem Hinzufügen günstiger / teurer",
   Gesamtpreis mit der Preisdifferenz des Warenkorbs, Summe der
   Makronährstoffe, Fortschrittsbalken für die Ziele, Bedienelemente für
   Export/Import und Drucken sowie Zeilen für Produkte, die den Katalog
   verlassen haben (angezeigt mit ihrem gespeicherten Namen, mit eigener
   Entfernen-Schaltfläche).
4. `/admin` — Review in zwei Tabs. **„Zu prüfen"** zeigt jede gescrapte Zeile
   mit ihrem Rohnamen (verlinkt auf die Aldi-Seite), ihrem Bild und bis zu fünf
   Kandidaten aus Open Food Facts mit eigenen Bildern, Konfidenzen und Links;
   Filter nach Kategorie (mit Anzahl) und nach einer Konfidenz-Voreinstellung;
   eine Checkbox beim besten Kandidaten jedes Blocks, die „Alle ab 90%
   auswählen" und „Auswahl bestätigen" speist. **„Ohne Treffer"** zeigt Zeilen,
   für die die Suche nichts gefunden hat, mit einem Feld zum Einfügen und
   Verknüpfen einer Open-Food-Facts-Produkt-URL. Der Wert für `X-Admin-Key` wird
   in ein Feld eingegeben und in `sessionStorage` gehalten, nie committet.

In der Review-Ansicht tragen der Name der gescrapten Zeile und der Name jedes
Kandidaten eine `ImageHoverPreview`: Das Bild erscheint bei Hover oder
Tastaturfokus außerhalb des Dokumentflusses, sodass sich nichts verschiebt, mit
einem neutralen Platzhalter, wenn die Quelle kein Bild hat oder es nicht lädt.

### 7.2 Format im Local Storage

```ts
// localStorage-Schlüssel: "foodplanner.list.v1"
{
  version: 1,
  items: [ { productKey: "aldi:1234567", quantity: 2, name: "Milch 3,5%",
             priceAtAddCents: 109, addedAt: "2026-11-20T10:00:00Z" } ],
  goals: { protein: 120, fat: 70, carbs: 250, kcal: 2000, budgetCents: 4000 } | null
}
```

`version` wird zuerst von einem zod-Schema geprüft. Ein fremdes oder
zukünftiges Format wird abgelehnt, statt die aktuelle Liste unbemerkt zu
beschädigen. Alle Felder einer Zeile sind Pflicht. Pro `productKey` gibt es
höchstens eine Zeile: Ein Produkt erneut hinzuzufügen erhöht seine Menge um 1,
`priceAtAddCents` und `addedAt` bleiben.

`web/src/limits.ts` spiegelt die Grenzen des Servers (200 Einträge, Menge
1…1000, Budget innerhalb von `Integer.MAX_VALUE`, Preis 1…100 000 Cent), sodass
ein Wert, den das Schema beim nächsten Laden ablehnen würde, gar nicht erst
gespeichert wird.

### 7.3 Export, Import und Drucken

Export und Import sind auch der Weg, mehrere Listen zu führen.

- **JSON-Export** — das gespeicherte Objekt, ausgeliefert über `Blob` und einen
  Anker-Download, benannt `foodplanner-list-YYYY-MM-DD.json`. Die Preise zum
  Zeitpunkt des Hinzufügens reisen mit, sodass eine einen Monat später wieder
  importierte Liste zeigt, wie sich die Preise seitdem verändert haben. Weil die
  Datei `productKey` statt Datenbank-IDs enthält, lässt sie sich auch in eine
  andere Instanz importieren.
- **JSON-Import** — Dateiauswahl, Parsen, zod-Validierung, dann
  `POST /products/batch`, um zu bestätigen, dass die Schlüssel noch existieren.
  Produkte, die nicht mehr existieren, werden in einem Block „nicht mehr im
  Katalog" aufgeführt; der Rest wird geladen. Nichts wird unbemerkt verworfen.
  Der Import ersetzt Liste und Ziele; ist die aktuelle Liste nicht leer, fragt
  die Oberfläche vorher nach.
- **Drucken** — `window.print()` mit einem Stylesheet für `@media print`, das
  nur die Liste mit ihren Summen zeigt: Kopfzeile, Bedienelemente,
  Mengenschalter und Ziele fallen weg, lange Listen brechen zwischen Zeilen um.
  Über den Druckdialog lässt sich die Liste auch als PDF speichern; anders als
  ein Bild enthält das PDF durchsuchbaren Text. Es gibt keine eigene
  Druckvorlage, gedruckt wird dieselbe Ansicht.

### 7.4 Neuberechnung

Jede Änderung der Liste löst über TanStack Query `POST /shopping-list/summary`
aus, mit 200 ms Debounce, damit ein gedrückt gehaltener Mengenschalter nicht pro
Klick eine Anfrage sendet. Die Query behält bei einem fehlgeschlagenen Refetch
ihre vorherigen Daten (`placeholderData: keepPreviousData`) und zeigt das
Fehlerbanner *über* der Liste statt an ihrer Stelle — ein dauerhafter Fehler
(etwa eine Liste, die über die Obergrenze des Servers gewachsen ist) darf den
Nutzer nicht von den Schaltflächen zum Entfernen und Leeren aussperren, die der
Weg hinaus sind.

API-Fehler werden mit `ProblemDetail.title` und `detail` angezeigt statt mit
einer allgemeinen Fehlermeldung.

## 8. Teststrategie

Werkzeuge: JUnit 5 und AssertJ. Ports werden in Tests überwiegend durch
handgeschriebene Fakes ersetzt; Mockito nur, wo ein Fake unverhältnismäßig wäre.

| Modul | Werkzeuge | Abdeckung |
|---|---|---|
| `domain` | JUnit, keine Mocks | Summenarithmetik, Zielfortschritt, `priceChangeCents` und Preisdifferenz, Namensnormalisierer und Markenextraktion, die Konfidenzformel mit beiden Korrekturen und beiden Schwellen, der strikte deutsche Zahlenparser, die Plausibilitätsprüfung, `productKey` |
| `application` | JUnit mit Fake-Ports | Routing der Suche (EAN, leer, lexikalisch); die Handler für auswählen / ablehnen / Sammelfreigabe / verknüpfen; „none" setzt alle Kandidaten auf `REJECTED`; Idempotenz; Batch mit unbekanntem Schlüssel; Validierungsregeln direkt über `Validator`, plus ein Test, dass `@Validated` am Handler tatsächlich greift |
| `infrastructure` | Testcontainers (`pgvector/pgvector`), WireMock, aufgezeichnete Fixtures | Flyway-Migrationen laufen durch; tokenweiser lexikalischer Abgleich (Reihenfolgeunabhängigkeit, Marken- und Kategoriefelder, Komposita); EAN-Suche; Filter; Seitennavigation; die Schwelle über `connectionInitSql`; der Abfragesatz `queries.json`; Java-Faltung gegen `unaccent`; die Verzweigung beim Schreiben eines Scrapes; „null löscht nichts"; der Aldi-Parser auf gespeicherten Seiten; der Nuxt-Resolver (numerische Literale, Wrapper, Zyklen); Auslesen der Einheit; der Open-Food-Facts-Client; `robots.txt`; Retry und Backoff; der Seed-Loader |
| `api` | `@SpringBootTest` + MockMvc gegen eine echte Datenbank | Endpunktverträge, Durchsetzung des Admin-Schlüssels (401 / 503), Form von `ProblemDetail`, CORS aus der Konfiguration |
| `ingestion` | JUnit mit Fakes, injizierte `Clock` | Snapshot- und Abrufpfad von `scrape`; Validierung der Einstellungen; Nebenläufigkeit und Taktung; das Zeitbudget lässt Zeilen `NOT_SEARCHED`; Retry-Rettung und Snapshot-Zusammenführung in `search-off`; Sitemap-Liste und Abdeckung im Snapshot; `seed export` |
| `architecture-tests` | ArchUnit | Abhängigkeitsrichtung der Module; kein Spring Web, JDBC, JPA oder HTTP-Client in `application`; keine Abhängigkeiten in `domain` |
| `web/` | Vitest, Testing Library, MSW | Komponenten, Seiten, der Listen-Reducer und die Speicherung, Round-Trips von Export/Import, die Admin-Ansichten |

Testcontainers verwendet von Anfang an das Image `pgvector/pgvector` (PostgreSQL
mit der Erweiterung), damit §10.3 kein anderes Image braucht.

Für die Java-Suite muss Docker laufen. **Kein Test greift auf das Netzwerk zu** —
jede Antwort von Drittanbietern ist ein aufgezeichnetes Fixture, und die
Aldi-Fixtures sind echte gespeicherte Seiten aus dem ersten Live-Lauf (§9.3).

## 9. Projekt starten

### 9.1 Mit Docker

```bash
docker compose up --build          # db + api + web, unter http://localhost:8080
ADMIN_KEY=choose-something docker compose up --build   # Admin-Endpunkte nutzbar
docker compose down -v             # stoppen und die Datenbank verwerfen
```

Das Dockerfile der API baut mehrstufig: Maven im Build-Stage, Ausführung auf
`eclipse-temurin:25-jre`. Die API ist direkt unter <http://localhost:5199>
erreichbar, mit Swagger UI (springdoc) unter `/swagger-ui.html`. Flyway migriert
das Schema beim Start, und der Katalog befüllt sich aus `data/seed/products.json`,
wenn er leer ist (`seed.path` zeigt auf die im Image eingebaute Kopie). Die
CORS-Origins kommen aus `cors.allowed-origins` — Standard ist der Vite-Dev-Server,
in `docker compose` der Origin des Web-Containers.

Die Datenbank läuft auf Port 5432 mit Datenbank, Benutzer und Passwort
`foodplanner`. Die Werte lassen sich über `POSTGRES_*` überschreiben; die
Standardwerte stehen in der Compose-Datei, sodass die Demo ohne `.env` startet.

### 9.2 Entwicklung

Die drei Teile laufen getrennt, weil ein Neubau der Container bei jeder Änderung
zu langsam ist:

```bash
docker compose up -d db
./mvnw install -DskipTests            # einmalig bzw. nach Änderungen an Nachbarmodulen
./mvnw -pl foodplanner-api spring-boot:run
cd web && npm install && npm run dev      # http://localhost:5173, /api wird durchgereicht
```

Die Ingestion wird manuell über die CLI ausgeführt, und ihre Snapshots werden
committet, sodass ein frischer Klon seine Datenbank ohne Netzwerkzugriff auf
Drittseiten befüllen kann.

### 9.3 Erster Live-Lauf

Es gibt noch keine Daten. Einmalig, früh im Projekt (§13, Phase 1–2):

1. `scrape aldi --fetch` mit `ingestion.aldi-max-products=300`.
2. Den Snapshot committen.
3. 10–20 der geladenen Seiten als Test-Fixtures übernehmen, darunter Produkte mit
   Nährwerten pro 100 ml, mit `/1 l`-Preispaar, ohne Nährwerttabelle und mit
   einer Nährwerttabelle in ungewöhnlicher Zeilenreihenfolge.
4. Die Vorerkenntnisse aus §1.1 gegen den Lauf prüfen.
5. `search-off`, Snapshot committen.
6. `review` in der Konsole.
7. `seed export`, Seed committen.

## 10. Erweiterungen

Die Reihenfolge entspricht §2.2. Jede Erweiterung bringt ihre eigene
Flyway-Migration und ihre eigenen Tests mit.

### 10.1 Rückgängig

**Modell.**

```
ReviewDecision  id, scrapedItemId, kind (APPROVAL | REJECTION), decidedAt,
                undone, candidateIdsCsv, chosenCandidateId, productId,
                productCreated, newPriceId, demotedPriceId,
                scrapedPageId, priorPageProductId, pageBindingChanged
```

`ReviewDecision` zeichnet genau auf, was eine Entscheidung bewirkt hat — welche
Kandidaten unterdrückt wurden, ob ein Produkt entstanden ist, welcher Preis
eingefügt und welcher herabgestuft wurde und ob sich eine Seitenbindung geändert
hat. Die Umkehr stellt den vorherigen Zustand wieder her, statt ihn zu erraten.

**API.**

```
POST /api/v1/admin/review/{itemId}/undo  → 204   (macht die letzte Entscheidung zu der Zeile rückgängig)
                                         → 404   nichts aufgezeichnet oder bereits rückgängig gemacht
                                         → 409   die Entscheidung ist älter als eine Stunde
```

**Oberfläche.** Nach jeder Entscheidung erscheint im Tab „Zu prüfen" ein Banner
zum Rückgängigmachen. Die Konsolenschleife bietet kein Rückgängig.

### 10.2 Tab „Katalog" und Ausblenden

**Modell.** `Product.isHidden`.

**Verhalten.** Ausblenden nimmt das Produkt aus der öffentlichen Suche, sonst
ändert sich nichts: Es wird weiterhin über den Batch-Endpunkt geladen, zählt
weiterhin in den Listensummen, und sein Preis wird bei jedem Scrape weiter
aktualisiert. Nach dem Wiedereinblenden zeigt es also nie einen veralteten
Preis. Kein Scrape setzt `isHidden` je zurück.

**Invariante: Sichtbarkeit und Existenz sind getrennt.** Ausgeblendete Produkte
fehlen nur in der Suche. `GET /products/{id}`, der Batch-Endpunkt und die
Listenzusammenfassung liefern sie immer, mit Kennzeichnung.

**API.**

```
GET  /api/v1/admin/catalog?q=&hidden=&page=1&pageSize=20
  → 200 PagedResult<CatalogRow>          // einschließlich ausgeblendeter Produkte
POST /api/v1/admin/catalog/{id}/hide     → 204 | 404
POST /api/v1/admin/catalog/{id}/unhide   → 204 | 404
```

`CatalogRow`: id, productKey, name, brand, category, priceCents, offCode,
isHidden, aldiUrl. `ProductDetail` und die Zeilen der Listenzusammenfassung
erhalten `isHidden`.

**Oberfläche.** Tab „Katalog" in `/admin` mit Suche und Filter nach
Sichtbarkeit. Detailseite und Liste zeigen „Nicht mehr im Sortiment".

### 10.3 Hybride Suche

**Modell.** Das Embedding-Modell läuft prozessintern über ONNX Runtime (Java-API)
mit dem Tokenizer aus DJL HuggingFace Tokenizers; das Einbetten ist eine lokale
Berechnung, kein Netzwerkaufruf. Port: `TextEmbedder`, aufgelöst in beiden
Composition Roots.

```
ProductEmbedding  productId, modelId, textHash, embedding vector(N)
```

`N` wird durch das Modell festgelegt, das `eval-search` auswählt (384 für
multilingual-e5-small, 768 für jina-embeddings-v2-base-de). Ein späterer
Modellwechsel ist eine Migration. Vektoren werden über `pgvector-java` gelesen
und geschrieben.

**Einbetten.** Das Embedding jedes Produkts wird aus
`{name} | {brand} | {category}` berechnet, mit dem Passage-Präfix des Modells,
falls es eins hat (e5 nutzt `passage: ` / `query: `). `textHash` sorgt dafür,
dass nur neu berechnet wird, wenn sich dieser Text oder das Modell ändert. Beide
Katalogwege (§4.3) berechnen das Embedding in derselben Transaktion, in der das
Produkt entsteht; solange das Modell verfügbar ist, existiert also kein Produkt
ohne Embedding. Wird eine Freigabe rückgängig gemacht, die ein Produkt erzeugt
hat (§10.1), wird auch dessen Embedding entfernt.

**Vektorsuche.**

- Die Anfrage wird mit demselben Modell eingebettet. Anfrage-Embeddings liegen in
  einem LRU-Cache im Speicher (`search.query-cache-size`).
- Nächste Nachbarn nach Kosinusdistanz über einen HNSW-Index; behalten werden nur
  die innerhalb von `search.vector-max-distance`. Eine Vektorsuche liefert immer
  *irgendetwas*, daher entscheidet diese Grenze, ob ein semantischer Nachbar
  überhaupt als Treffer zählt; sie wird durch `eval-search` kalibriert.
- Filter werden innerhalb der Vektorsuche angewendet; der iterative Index-Scan
  von pgvector (ab 0.8) verhindert, dass ein selektiver Filter die Vektorseite
  aushungert.
- Die besten 100 innerhalb der Grenze gehen weiter; ebenso die besten 100 der
  lexikalischen Suche.

**Fusion.**

- Reciprocal Rank Fusion: Jedes Produkt erhält `Σ 1 / (60 + Rang)` über die
  Listen, in denen es vorkommt. Ein Produkt, das in beiden Listen gut platziert
  ist, steigt so über den Spitzenreiter jeder einzelnen Liste.
- Ein Produkt mit einem exakten Worttreffer für jedes Token erhält zusätzlich
  einen festen Bonus, sodass `Milch` Milch vor einem semantisch ähnlichen
  Joghurt liefert.
- `sort=relevance` sortiert nach dem fusionierten Score; die Begrenzung auf 200
  gilt für die fusionierte Liste.

**Degradierung.** Fehlt das Modell oder lässt es sich nicht laden, führt
`ProductSearch` nur die lexikalische Suche aus, loggt beim Start einmal eine
Warnung, und ein `HealthIndicator` meldet `searchMode: "lexical"` (sonst
`"hybrid"`). Die Suche scheitert nie am Modell.

**Was das bringt.** Die semantische Seite findet `Hähnchenbrustfilet` für
`Hühnchen` oder `Chicken`, Cerealien und Müsli für `Frühstück` und Riegel für
`Proteinriegel`, in deren Namen „Protein" nie vorkommt. Die im Kern als
erwartete Fehlschläge markierten Gruppen des Abfragesatzes werden scharf
geschaltet.

**Commands.**

- **`embed [--all]`** berechnet Embeddings für Produkte, denen eines fehlt oder
  deren `textHash` oder `modelId` nicht mehr passt; `--all` berechnet alles neu.
  Dient dem Nachfüllen nach der Migration und nach einem Modellwechsel.
- **`eval-search --model e5-small|jina-de`** bettet den Katalog mit dem
  genannten Modell in eine Arbeitstabelle ein (die produktiven Embeddings bleiben
  unberührt), führt `data/search-eval/queries.json` aus und meldet recall@10 und
  MRR@10 insgesamt und pro Gruppe, die Zeit für das Einbetten des Katalogs und
  die Anfragelatenz (p50/p95) — als Markdown-Tabelle auf stdout und in
  `data/search-eval/results-<model>-<date>.md`. Außerdem durchläuft es Werte für
  `search.vector-max-distance` und die lexikalische Schwelle und meldet den Wert,
  der recall@10 maximiert, ohne fremde Treffer zuzulassen. Der Gewinner wird in
  `search.model` eingetragen und ist das einzige ausgelieferte Modell.

**Bauen.** `scripts/fetch-model` lädt das in `search.model` gewählte Modell in
einer gepinnten Revision von Hugging Face und prüft es gegen den SHA-256 in
`models/manifest.json`; bei Abweichung bricht es ab. Das Dockerfile der API
führt dasselbe Skript aus; das erste `docker compose up --build` dauert dadurch
länger und braucht Netzwerkzugriff, der spätere Betrieb nicht. `models/` wird
nicht committet. Der Seed-Schritt bettet den befüllten Katalog beim ersten Start
ein.

**Tests.** Modellabhängige Tests tragen `@Tag("requires-model")` und sind im
Standard-Maven-Lauf ausgeschlossen (Surefire `excludedGroups`); sie prüfen Form
und Determinismus der Embeddings, die Vektorsuche mit der Distanzgrenze und einen kleinen festen
Ausschnitt des Abfragesatzes als Untergrenze gegen Regressionen. Fehlt die
Modelldatei, werden sie mit einem Hinweis auf `scripts/fetch-model`
übersprungen. Ohne Modell: Reciprocal Rank Fusion und der Bonus für exakte
Treffer (`domain`), Routing einschließlich lexikalischem Rückfall
(`application`), `searchMode` im Health-Endpunkt (`api`), Erkennung veralteter
Embeddings in `embed` (`ingestion`).

### 10.4 Nährwerte verknüpfen

**Modell.**

```
Product.nutritionSource   ALDI | OPENFOODFACTS | MANUAL_OFF | SEED
NutritionOverride         id, productId, offCode, linkedAt,
                          previousOffCode, previousNutritionSource,
                          vorherige kcal / protein / fat / carbs pro 100 g
```

`nutritionSource` entspricht anfangs `source` und weicht nur nach einer
manuellen Verknüpfung davon ab.

**Verhalten.** Im Tab „Katalog" nimmt „Nährwerte verknüpfen" eine
Open-Food-Facts-Produkt-URL entgegen, sucht das Produkt per Barcode und verlangt
alle vier Makronährstoffe sowie das Bestehen der Plausibilitätsprüfung. Zuerst
wird ein `NutritionOverride` mit den bisherigen Nährwerten, der bisherigen EAN
und der bisherigen `nutritionSource` angelegt, dann werden die Werte pro 100 g
ersetzt, `offCode` gesetzt und `nutritionSource = MANUAL_OFF` eingetragen.
Packungsgröße und Einheit bleiben so, wie Aldi sie angibt. Die Verknüpfung wird
abgelehnt, wenn die EAN bereits zu einem anderen Produkt gehört. „Verknüpfung
lösen" stellt exakt das wieder her, was der `NutritionOverride` aufgezeichnet
hat, und löscht ihn — derselbe Ansatz „erst aufzeichnen, dann umkehren" wie in
§10.1.

**Invarianten.** Solange `nutritionSource = MANUAL_OFF` gilt, schreibt kein
Scrape die Nährwerte des Produkts; Preise werden weiterhin aktualisiert. Ein
Aldi-Produkt kann nach einer Verknüpfung zusätzlich einen `offCode` tragen;
`aldiSku` bleibt seine Identität und sein `productKey`, und `offCode` bleibt
produktübergreifend eindeutig.

**API.**

```
POST   /api/v1/admin/catalog/{id}/nutrition-link
         { "url": "https://world.openfoodfacts.org/product/…" }
  → 204   Nährwerte ersetzt und festgelegt
  → 400   url ist keine Produkt-URL von openfoodfacts.org
  → 404   kein Open-Food-Facts-Produkt zu diesem Barcode oder kein solches Katalogprodukt
  → 409   einer der vier Makronährstoffe fehlt, die Plausibilitätsprüfung schlägt fehl,
          oder die EAN gehört bereits zu einem anderen Produkt

DELETE /api/v1/admin/catalog/{id}/nutrition-link    → 204 | 404
```

Der Katalog-Endpunkt erhält den Filter `nutritionSource`; `CatalogRow` und
`ProductDetail` erhalten `nutritionSource`.

### 10.5 Auslistung

**Modell.** `Product.delistedAt` (nullable).

**Verhalten.** Ein Produkt, dessen Aldi-Seite die Sitemap verlassen hat, würde
sonst seinen letzten Preis unbegrenzt als „aktuell" behalten. Am Ende eines
Laufs erhalten Produkte, die an eine `ScrapedPage` gebunden sind, deren URL in
der Sitemap fehlt, `delistedAt` mit dem Zeitpunkt des Laufs; Produkte, deren URL
wieder aufgetaucht ist, bekommen den Wert gelöscht. Das geschieht **nur, wenn
der Lauf die vollständige Sitemap betrachtet hat**: Enthält die Sitemap mehr
Einträge als `ingestion.aldi-max-products`, beweist eine fehlende URL nichts.
Weil Snapshots die Sitemap-Liste und die Abdeckung enthalten (§6.1), kommt ein
Snapshot-Lauf zum selben Ergebnis wie der Live-Lauf, der ihn erzeugt hat.
Preiszeilen bleiben unberührt.

Ausgelistete Produkte fehlen in der Suche, werden aber wie ausgeblendete Produkte
weiter über Detail, Batch und Listenzusammenfassung geliefert; `unitPriceCents`
ist der letzte beobachtete Preis. Die Oberfläche zeigt „Nicht mehr bei Aldi".

**Hinweis zur Demo.** Der Demo-Katalog betrachtet 300 Einträge einer deutlich
größeren Sitemap; die Bedingung „vollständige Abdeckung" ist dort nie erfüllt.
Die Auslistung ist deshalb nur in Tests sichtbar, nicht in der Demo. Die
Bedingung wird dafür nicht gelockert.

### 10.6 `eval-confidence`

Bewertet die aufgezeichnete Review-Historie mit der aktuellen Formel und mit
embeddingbasierten Varianten der Namenskomponente (Ersatz und Mischung mit
Jaro-Winkler). Setzt §10.3 voraus.

- Die gelabelten Paare stammen aus den Statuswerten der `OffCandidate`-Zeilen:
  `APPROVED` ist ein Positivbeispiel, `REJECTED` ein Negativbeispiel.
- Die Hauptkennzahl ist die **Präzision über 0,90**, weil dieser Bereich in die
  Sammelfreigabe einfließt. Daneben wird die Top-1-Genauigkeit gemeldet, jeweils
  mit der Größe des Bereichs.
- Die Historie enthält nur Kandidaten, die die aktuelle Formel mit mindestens
  0,55 bewertet hat. Die Evaluierung kann Ranking und Sammelbereich fair
  vergleichen, aber nicht zeigen, ob die Formel gute Treffer verwirft.
- Unter etwa 200 Entscheidungen warnt der Command, dass das Ergebnis nicht
  belastbar ist. Beim Demo-Katalog (rund 60 Review-Zeilen) ist das der
  Normalfall; Abhilfe schafft ein manuell gelabelter Ausschnitt aus „Ohne
  Treffer".
- Die Formel aus §6.2 bleibt, sofern die Evaluierung nicht zeigt, dass die
  Alternative mindestens gleich gut ist.

### 10.7 PNG-Export

Ergänzt die Druckansicht (§7.3) um ein Bild, das sich leichter in einen
Messenger schicken oder auf dem Handy öffnen lässt als ein PDF.

**Verhalten.** `html-to-image` erfasst einen eigens dafür gebauten Kartenknoten
statt der Live-Oberfläche, sodass keine Bedienelemente oder Scrollleisten im
Bild landen. Die Karte zeigt dieselben Zeilen und Summen wie die Druckansicht;
die Datei heißt `foodplanner-list-YYYY-MM-DD.png`.

**Kein Backend.** Die Erweiterung betrifft nur `web/` und braucht weder
Migration noch Endpunkt.

**Tests.** Vitest prüft, dass die Karte aus denselben Daten wie die Liste
entsteht und keine Bedienelemente enthält; das Bild selbst wird nicht
verglichen.

## 11. Bekannte Kompromisse

- **Clean Architecture** bedeutet mehrere Dateien pro Use Case statt einer
  Service-Methode. Akzeptiert, um das Muster zu demonstrieren; dafür laufen die
  `application`-Tests ohne Datenbank, und ArchUnit prüft die Grenzen.
- **Spring in `application`** ist eine bewusste Abweichung von einer
  framework-freien Use-Case-Schicht. Sie spart eine manuelle Validierung in jedem
  Handler und einen Transaktions-Port; ArchUnit begrenzt sie auf
  `spring-context`, `spring-tx` und die Validierungs-API.
- **Kein ORM.** SQL und Abbildung werden von Hand geschrieben. Dafür ist jede
  Abfrage vollständig sichtbar, und Suche und CRUD folgen demselben Muster.
- **Preise sind nur so aktuell wie der letzte Scrape.** Die Anwendung gibt
  `lastScrapedAt` aus, damit die Aktualität sichtbar ist statt nur angenommen.
- **Produkte aus Aldi-Quelle gelangen ohne Review in den Katalog.** Der
  menschliche Schritt ist eine Anforderung an den *Abgleich*, und es gibt nichts
  abzugleichen, wenn Nährwerte und Preis von derselben Seite für denselben
  Artikel stammen. Stattdessen schützen die Regel „alle vier oder keiner", der
  exakte Abgleich der Bezeichnungen, die Plausibilitätsprüfung und
  `source = ALDI`, das die gesamte Menge mit einem einzigen Prädikat löschbar und
  neu aufbaubar macht. Mit §10.2 und §10.4 kommt eine Korrektur pro Produkt
  hinzu.
- **Im Kern lässt sich eine falsche Freigabe nicht über die Oberfläche
  zurücknehmen.** Korrigieren lässt sie sich nur direkt in der Datenbank oder
  durch Löschen aller Produkte mit `source = OPENFOODFACTS` und erneutes Review.
  Deshalb ist Rückgängig (§10.1) die erste Erweiterung.
- **Die Abdeckung deutscher Eigenmarken in Open Food Facts ist lückenhaft**; rund
  ein Fünftel der Lebensmittelzeilen landet im Review (§1.1). Die Sammelauswahl
  über 90% hält diese Warteschlange beherrschbar, ohne die Latte dafür zu senken,
  was eine Freigabe bedeutet.
- **Nährwerte pro 100 ml werden gespeichert, als wären sie pro 100 g.** Die
  Arithmetik ist in sich stimmig (§6.1); `unit` beseitigt die einzige sichtbare
  Folge, das Wort „Gramm" bei Volumenprodukten.
- **Kern und Erweiterungen** sind durch die Abgabe Mitte Dezember getrennt. Die
  Erweiterungen sind vollständig beschrieben, damit jede einzeln ergänzt werden
  kann, ohne den Kern umzubauen.
- **Das Embedding-Modell (§10.3) ist ein zweites bewegliches Teil.** Es fügt
  einen Download zur Bauzeit, einige hundert Megabyte zum Image, einige zehn
  Millisekunden pro nicht gecachter Anfrage auf der CPU und eine Qualität hinzu,
  die gemessen statt angenommen werden muss — daher `eval-search`. Durch den
  lexikalischen Rückfall verschlechtert ein fehlendes Modell die Suche, statt sie
  zu zerstören.

## 12. Bei der Umsetzung zu prüfen

- Ob die Vorerkenntnisse aus §1.1 beim ersten Live-Lauf noch gelten: Struktur des
  Nuxt-Payloads, Anteil der Seiten mit Nährwerttabelle, Verhalten von
  SearchaLicious.
- Ob die Schwelle 0,4 auch für den tokenweisen Abgleich trägt; das zeigt der Test
  auf `queries.json`.
- Ob die Java-Faltung und `unaccent` für alle Zeichen im Katalog übereinstimmen.
- Bei §10.3: das Modell, das `eval-search` auswählt; es legt die
  Vektordimension, die Distanzgrenze und die lexikalische Schwelle der Fusion
  fest.
- Bei §10.6: wie viele Review-Entscheidungen vorliegen und ob eine manuell
  gelabelte Stichprobe nötig ist.

## 13. Umsetzungsreihenfolge

Der riskanteste Schritt — Live-Daten von einer fremden Seite — kommt zuerst;
alles Weitere wird auf echten Daten gebaut. Abgabe ist um den 15.12.2026
(KW 51).

| Phase | KW | Inhalt | Ergebnis |
|---|---|---|---|
| 0. Gerüst | 42 | Maven-Module, ArchUnit-Regeln, `docker compose` mit Datenbank, erste Flyway-Migration | Leeres Projekt baut, Tests grün |
| 1. Aldi-Daten | 42–44 | `robots.txt` und Sitemap, Seitenparser, Nuxt-Resolver, Einheit und Packungsgröße, Snapshots; erster Live-Lauf mit 300 Produkten (§9.3) | Snapshot und Fixtures committet, §1.1 geprüft |
| 2. Katalog und Review | 44–45 | Verzweigung beim Schreiben, `search-off` mit Konfidenzformel und Zeitbudget, Konsolen-`review`, `seed export` | Befüllte Datenbank, `products.json` committet |
| 3. API | 46 | Lexikalische und EAN-Suche, Detail, Batch, Listenzusammenfassung, `catalog/info`, Admin-Endpunkte, `ProblemDetail`, Admin-Filter, Abfragesatz | Vertrag aus §5 vollständig getestet |
| 4. Frontend | 47–48 | Suche, Detail, Liste, Export/Import, Druckansicht, Admin-Tabs, generierte API-Typen | Durchgehende Demo im Browser |
| 5. Abschluss Kern | 49 | Alles in `docker compose`, README, Offline-Demo aus einem frischen Klon geprüft | **Kern fertig** |
| 6. Erweiterungen | 50 | Rückgängig, dann Ausblenden; weitere nur, wenn Zeit bleibt | Je nach verfügbarer Zeit |
| Abgabe | 51 | Abgabe um den 15.12. | |

KW 49 ist zugleich Puffer: Verzögert sich eine der Phasen 1–4, verschiebt sich
Phase 6, nicht der Kern. Realistisch sind bis zur Abgabe höchstens die ersten
zwei Erweiterungen; die übrigen bleiben als ausgearbeitetes Design im Dokument.
