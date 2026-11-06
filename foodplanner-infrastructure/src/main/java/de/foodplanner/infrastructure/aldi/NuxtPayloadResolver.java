package de.foodplanner.infrastructure.aldi;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Löst einen Nuxt/devalue-Graphen (flaches JSON-Array) in einen normalen Baum auf (§6.1).
 *
 * <ol>
 *   <li>Ein numerischer Eintrag im Array ist ein Literal. Nur Zahlen, die als Wert in einem
 *       Objekt oder Array stehen, sind Indizes.</li>
 *   <li>Wrapper-Einträge ({@code ["Reactive", i]}, Ref, ShallowReactive, EmptyRef, skipHydrate, Set,
 *       NuxtError) verweisen über ihr Element 1 weiter.</li>
 *   <li>Zyklen werden über die Menge der gerade besuchten Indizes erkannt und durch null ersetzt.</li>
 * </ol>
 *
 * Aufgelöst wird nur der Teilbaum {@code $spdp-product-<sku>}.
 */
public final class NuxtPayloadResolver {

    public static final String PRODUCT_KEY_PREFIX = "$spdp-product-";

    private static final Set<String> WRAPPERS = Set.of(
            "Reactive", "Ref", "ShallowReactive", "EmptyRef", "skipHydrate", "Set", "NuxtError");

    private static final JsonNodeFactory NODES = JsonNodeFactory.instance;

    private final JsonNode payload;

    public NuxtPayloadResolver(JsonNode payload) {
        if (payload == null || !payload.isArray()) {
            throw new IllegalArgumentException("Nuxt-Payload muss ein JSON-Array sein");
        }
        this.payload = payload;
    }

    /** Artikelnummer (Suffix des Schlüssels) und aufgelöster Produktbaum. */
    public record ProductSubtree(String sku, JsonNode node) {
    }

    /** Sucht den Schlüssel {@code $spdp-product-<sku>} und löst nur diesen Teilbaum auf. */
    public Optional<ProductSubtree> resolveProduct() {
        for (JsonNode entry : payload) {
            if (!entry.isObject()) {
                continue;
            }
            for (Map.Entry<String, JsonNode> field : entry.properties()) {
                if (field.getKey().startsWith(PRODUCT_KEY_PREFIX) && field.getValue().isInt()) {
                    String sku = field.getKey().substring(PRODUCT_KEY_PREFIX.length());
                    return Optional.of(new ProductSubtree(sku, resolve(field.getValue().asInt())));
                }
            }
        }
        return Optional.empty();
    }

    /** Löst den Eintrag an {@code index} vollständig auf. */
    public JsonNode resolve(int index) {
        return resolve(index, new HashSet<>());
    }

    private JsonNode resolve(int index, Set<Integer> path) {
        if (index < 0 || index >= payload.size()) {
            return NullNode.getInstance(); // u. a. devalue-Marker wie -1 (undefined)
        }
        if (!path.add(index)) {
            return NullNode.getInstance(); // Zyklus
        }
        try {
            JsonNode node = payload.get(index);

            if (node.isArray()) {
                if (node.size() >= 2 && WRAPPERS.contains(node.get(0).asText())) {
                    JsonNode target = node.get(1);
                    // Element 1 verweist auf einen anderen Eintrag (Index).
                    return target.isInt() ? resolve(target.asInt(), path) : target;
                }
                ArrayNode out = NODES.arrayNode();
                for (JsonNode element : node) {
                    out.add(element.isInt() ? resolve(element.asInt(), path) : element);
                }
                return out;
            }
            if (node.isObject()) {
                ObjectNode out = NODES.objectNode();
                for (Map.Entry<String, JsonNode> field : node.properties()) {
                    JsonNode value = field.getValue();
                    out.set(field.getKey(), value.isInt() ? resolve(value.asInt(), path) : value);
                }
                return out;
            }
            // Literal (Zahl, String, Boolean, null): unverändert übernehmen.
            return node;
        } finally {
            path.remove(index);
        }
    }
}
