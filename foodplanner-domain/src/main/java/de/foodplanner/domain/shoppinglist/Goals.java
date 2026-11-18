package de.foodplanner.domain.shoppinglist;

/**
 * Ziele der Einkaufsliste (§7.2). Jedes Ziel ist einzeln optional: {@code null} heißt „nicht gesetzt". Ein gesetztes
 * Ziel ist mindestens 1, sonst teilte {@link GoalProgress} durch null. Die Anfrage prüft das schon vorher und meldet
 * es als Fehler je Feld; der Konstruktor sichert die Regel für jeden anderen Aufrufer ab.
 *
 * @param protein     Eiweiß in Gramm
 * @param fat         Fett in Gramm
 * @param carbs       Kohlenhydrate in Gramm
 * @param kcal        Energie in Kilokalorien
 * @param budgetCents Budget in Cent
 */
public record Goals(Long protein, Long fat, Long carbs, Long kcal, Long budgetCents) {

    /** @throws IllegalArgumentException wenn ein gesetztes Ziel kleiner als 1 ist */
    public Goals {
        requireAtLeastOne("protein", protein);
        requireAtLeastOne("fat", fat);
        requireAtLeastOne("carbs", carbs);
        requireAtLeastOne("kcal", kcal);
        requireAtLeastOne("budgetCents", budgetCents);
    }

    private static void requireAtLeastOne(String name, Long goal) {
        if (goal != null && goal < 1) {
            throw new IllegalArgumentException(name + " muss mindestens 1 sein, war " + goal);
        }
    }
}
