package de.foodplanner.infrastructure.aldi;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import de.foodplanner.domain.Per100g;
import de.foodplanner.domain.RawProduct;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;


public final class AldiProductParser {

    private final ObjectMapper mapper = JsonMapper.builder().build();

    public Optional<RawProduct> parse(String html, String sourceUrl) {
        Document document = Jsoup.parse(html, sourceUrl == null ? "" : sourceUrl);

        JsonNode product = null;
        JsonNode breadcrumb = null;
        for (Element script : document.select("script[type=application/ld+json]")) {
            for (JsonNode candidate : flatten(readJson(script.data()))) {
                String type = candidate.path("@type").asText("");
                if (type.equals("Product") && product == null) {
                    product = candidate;
                } else if (type.equals("BreadcrumbList") && breadcrumb == null) {
                    breadcrumb = candidate;
                }
            }
        }
        if (product == null) {
            return Optional.empty();
        }

        String productName = textOrNull(product.path("name"));
        String brand = brandName(product.path("brand"));
        String name = productName;
        if (name != null && brand != null && !name.toLowerCase().startsWith(brand.toLowerCase())) {
            name = brand + " " + name; // "Name mit Marke"
        }

        JsonNode offers = firstOf(product.path("offers"));
        Long priceCents = toCents(offers.path("price"));
        String url = firstNonNull(textOrNull(product.path("url")), textOrNull(offers.path("url")), sourceUrl);

        List<String> categories = categoryPath(breadcrumb, productName);

        Element ogImage = document.selectFirst("meta[property=og:image]");
        String image = ogImage == null ? null : ogImage.attr("content");

        String sku = null;
        Per100g nutrition = null;
        Element nuxt = document.selectFirst("script#__NUXT_DATA__");
        if (nuxt != null) {
            JsonNode payload = readJson(nuxt.data());
            if (payload != null && payload.isArray()) {
                Optional<NuxtPayloadResolver.ProductSubtree> subtree = new NuxtPayloadResolver(payload).resolveProduct();
                if (subtree.isPresent()) {
                    sku = subtree.get().sku();
                    nutrition = NutritionTableReader.read(subtree.get().node()).orElse(null);
                }
            }
        }

        return Optional.of(new RawProduct(name, priceCents, url, categories, image, sku, nutrition));
    }

    private JsonNode readJson(String json) {
        try {
            return mapper.readTree(json);
        } catch (JacksonException e) {
            return null;
        }
    }

    private static List<JsonNode> flatten(JsonNode node) {
        List<JsonNode> out = new ArrayList<>();
        if (node == null) {
            return out;
        }
        if (node.isArray()) {
            node.forEach(child -> out.addAll(flatten(child)));
        } else if (node.has("@graph")) {
            out.addAll(flatten(node.get("@graph")));
        } else {
            out.add(node);
        }
        return out;
    }

    private static JsonNode firstOf(JsonNode node) {
        return node.isArray() && node.size() > 0 ? node.get(0) : node;
    }

    private static String brandName(JsonNode brand) {
        if (brand.isObject()) {
            return textOrNull(brand.path("name"));
        }
        return textOrNull(brand);
    }

    private static Long toCents(JsonNode price) {
        if (price.isMissingNode() || price.isNull()) {
            return null;
        }
        try {
            // ld+json ist englisch formatiert: "1.99"
            BigDecimal value = new BigDecimal(price.asText().strip());
            return value.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
        } catch (NumberFormatException | ArithmeticException e) {
            return null;
        }
    }

    /** Pfad ohne den letzten Eintrag, falls dieser das Produkt selbst ist. */
    private static List<String> categoryPath(JsonNode breadcrumb, String productName) {
        List<String> names = new ArrayList<>();
        if (breadcrumb == null) {
            return names;
        }
        List<JsonNode> items = new ArrayList<>();
        breadcrumb.path("itemListElement").forEach(items::add);
        items.sort(Comparator.comparingInt(item -> item.path("position").asInt(0)));
        for (JsonNode item : items) {
            String itemName = textOrNull(item.path("name"));
            if (itemName == null) {
                itemName = textOrNull(item.path("item").path("name"));
            }
            if (itemName != null) {
                names.add(itemName);
            }
        }
        if (!names.isEmpty() && productName != null && names.get(names.size() - 1).equals(productName)) {
            names.remove(names.size() - 1);
        }
        return names;
    }

    private static String textOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String text = node.asText().strip();
        return text.isEmpty() ? null : text;
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
