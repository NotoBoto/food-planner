package de.foodplanner.infrastructure.aldi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class NuxtPayloadResolverTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private static NuxtPayloadResolver resolver(String json) {
        return new NuxtPayloadResolver(MAPPER.readTree(json));
    }

    @Test
    void numericEntryIsLiteralNotIndex() {
        // Index 2 enthält die Zahl 0 -> bleibt 0 (kein Verweis auf Index 0).
        JsonNode result = resolver("[{\"a\":1},{\"b\":2},0]").resolve(0);

        assertThat(result.get("a").get("b").isInt()).isTrue();
        assertThat(result.get("a").get("b").asInt()).isZero();
    }

    @Test
    void followsEveryWrapperToElementOne() {
        for (String wrapper : new String[] {"Reactive", "Ref", "ShallowReactive", "EmptyRef",
                "skipHydrate", "Set", "NuxtError"}) {
            var resolver = resolver("[[\"" + wrapper + "\",1],{\"x\":2},\"wert\"]");

            assertThat(resolver.resolve(0).get("x").asText()).as(wrapper).isEqualTo("wert");
        }
    }

    @Test
    void cycleDoesNotLoopForever() {
        // 0 -> {"self":0}; 1 -> Ref -> 1
        var resolver = resolver("[{\"self\":0},[\"Ref\",1]]");

        org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            assertThat(resolver.resolve(0).get("self").isNull()).isTrue();
            assertThat(resolver.resolve(1).isNull()).isTrue();
        });
    }

    @Test
    void sharedNodeIsNotMistakenForCycle() {
        JsonNode result = resolver("[{\"a\":2,\"b\":2},0,{\"x\":3},\"v\"]").resolve(0);

        assertThat(result.get("a").get("x").asText()).isEqualTo("v");
        assertThat(result.get("b").get("x").asText()).isEqualTo("v");
    }

    @Test
    void resolvesOnlyProductSubtree() {
        var subtree = resolver("[{\"other\":1,\"$spdp-product-99\":2},\"ignored\",{\"sku\":3},\"99\"]")
                .resolveProduct().orElseThrow();

        assertThat(subtree.sku()).isEqualTo("99");
        assertThat(subtree.node().get("sku").asText()).isEqualTo("99");
    }

    @Test
    void noProductKeyGivesEmpty() {
        assertThat(resolver("[{\"foo\":1},\"bar\"]").resolveProduct()).isEmpty();
    }

    @Test
    void outOfRangeAndNegativeIndexGiveNull() {
        JsonNode result = resolver("[{\"a\":7,\"b\":-1}]").resolve(0);

        assertThat(result.get("a").isNull()).isTrue();
        assertThat(result.get("b").isNull()).isTrue();
    }

    @Test
    void rejectsPayloadThatIsNotAnArray() {
        assertThatThrownBy(() -> resolver("{\"a\":1}")).isInstanceOf(IllegalArgumentException.class);
    }
}
