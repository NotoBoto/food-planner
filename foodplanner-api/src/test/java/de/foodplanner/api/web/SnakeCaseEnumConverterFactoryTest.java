package de.foodplanner.api.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.core.convert.converter.Converter;

import de.foodplanner.application.port.SearchSort;

/** Der Konverter nimmt nur die kleingeschriebene snake_case-Form an (§5.1). */
class SnakeCaseEnumConverterFactoryTest {

    private final Converter<String, SearchSort> converter =
            new SnakeCaseEnumConverterFactory().getConverter(SearchSort.class);

    @Test
    void acceptsLowercaseSnakeCase() {
        assertThat(converter.convert("price_asc")).isEqualTo(SearchSort.PRICE_ASC);
        assertThat(converter.convert("relevance")).isEqualTo(SearchSort.RELEVANCE);
    }

    @Test
    void emptyStringYieldsNullSoDefaultApplies() {
        assertThat(converter.convert("")).isNull();
    }

    @Test
    void rejectsUppercaseMixedCaseAndUnknown() {
        for (String bad : new String[] {"PRICE_ASC", "Price_Asc", "foo"}) {
            assertThatThrownBy(() -> converter.convert(bad))
                    .as(bad)
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
