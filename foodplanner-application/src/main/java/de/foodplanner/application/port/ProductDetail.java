package de.foodplanner.application.port;

import java.math.BigDecimal;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Read-Model eines einzelnen Produkts für Detailseite und Sammelabruf (§5.1, §5.4): die Felder von
 * {@link ProductSummary} plus Herkunft, Links und Preisverlauf. Geld in Cent, Nährwerte pro 100 g auf eine
 * Nachkommastelle gerundet.
 *
 * <p>Ein eigener Record statt {@link ProductSummary} als Komponente: Detail und Sammelabruf filtern bewusst nicht auf
 * einen aktuellen Preis (§5.1), deshalb sind {@code priceCents} und {@code pricePerKgCents} hier nullable, in der
 * Suche dagegen nie.
 *
 * @param priceCents      aktueller Preis in Cent, {@code null} ohne aktuellen Preis
 * @param pricePerKgCents Preis je Kilogramm in Cent ({@code HALF_UP}), bei {@code unit = ML} je Liter; {@code null}
 *                        ohne aktuellen Preis
 * @param offCode         EAN bei Open Food Facts
 * @param aldiDepartment  Aldi-Abteilung; die Kategorie aus der {@code BreadcrumbList} der Aldi-Seite (§6.1), daher nur
 *                        bei Produkten mit {@code source = ALDI} gesetzt (§4.3)
 * @param aldiUrl         URL der zuletzt abgerufenen Aldi-Seite, die an das Produkt gebunden ist
 * @param offUrl          Produktseite bei Open Food Facts, nur mit {@code offCode}
 * @param priceHistory    alle Preise, neueste zuerst; leer, wenn kein abgelöster Preis existiert
 */
public record ProductDetail(
        long id,
        String productKey,
        String name,
        @Nullable String brand,
        @Nullable String category,
        int packageGrams,
        String unit,
        @Nullable Long priceCents,
        @Nullable Long pricePerKgCents,
        @Nullable BigDecimal protein100g,
        @Nullable BigDecimal fat100g,
        @Nullable BigDecimal carbs100g,
        @Nullable BigDecimal kcal100g,
        @Nullable String offCode,
        @Nullable String aldiDepartment,
        @Nullable String aldiUrl,
        @Nullable String offUrl,
        List<PriceHistoryEntry> priceHistory) {
}
