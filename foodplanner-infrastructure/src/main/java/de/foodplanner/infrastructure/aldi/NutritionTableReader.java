package de.foodplanner.infrastructure.aldi;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import de.foodplanner.domain.GermanNumberParser;
import de.foodplanner.domain.NumberParseException;
import de.foodplanner.domain.Per100g;

import tools.jackson.databind.JsonNode;


public final class NutritionTableReader {

    static final String LABEL_KCAL = "Energie [kcal]";
    static final String LABEL_FAT = "Fett";
    static final String LABEL_CARBS = "Kohlenhydrate";
    static final String LABEL_PROTEIN = "Eiweiß";

    private NutritionTableReader() {
    }

    public static Optional<Per100g> read(JsonNode product) {
        if (product == null) {
            return Optional.empty();
        }
        for (JsonNode information : product.path("multiInformations")) {
            Optional<Per100g> nutrition = readInformation(information);
            if (nutrition.isPresent()) {
                return nutrition;
            }
        }
        return Optional.empty();
    }

    private static Optional<Per100g> readInformation(JsonNode information) {
        String basis = information.path("nutritionalValueBasisDisplay").asText("")
                .strip().toLowerCase(Locale.ROOT);
        if (!basis.equals("pro 100 g") && !basis.equals("pro 100 ml")) {
            return Optional.empty();
        }

        Map<String, String> byLabel = new HashMap<>();
        for (JsonNode row : information.path("nutritionalValues")) {
            if (row.isArray()) {
                row.forEach(cell -> collect(cell, byLabel));
            } else {
                collect(row, byLabel);
            }
        }

        try {
            return Optional.of(new Per100g(
                    parse(byLabel, LABEL_KCAL),
                    parse(byLabel, LABEL_PROTEIN),
                    parse(byLabel, LABEL_FAT),
                    parse(byLabel, LABEL_CARBS)));
        } catch (NumberParseException | IllegalStateException e) {
            return Optional.empty(); // alle oder keiner
        }
    }

    private static void collect(JsonNode cell, Map<String, String> byLabel) {
        String name = cell.path("name").asText(null);
        String value = cell.path("amountDisplay").asText(null);
        if (name != null && value != null) {
            byLabel.putIfAbsent(name.strip(), value);
        }
    }

    private static BigDecimal parse(Map<String, String> byLabel, String label) {
        String raw = byLabel.get(label); // exakter Abgleich, kein startsWith
        if (raw == null) {
            throw new IllegalStateException("Fehlt: " + label);
        }
        return GermanNumberParser.parse(raw);
    }
}
