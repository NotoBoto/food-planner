package de.foodplanner.api.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.core.convert.converter.Converter;

import de.foodplanner.application.port.SearchSort;

/** Der Konverter nimmt nur die kleingeschriebene snake_case-Form an (§5.1). */
class SnakeCaseEnumConverterTest {

    private final Converter<String, SearchSort> converter = new SnakeCaseEnumConverter<>(SearchSort.class);

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

    @Test
    void webConfigAppliesItOnlyToSearchSort() {
        var conversion = new DefaultFormattingConversionService();
        new WebConfig().addFormatters(conversion);

        assertThat(conversion.convert("price_asc", SearchSort.class)).isEqualTo(SearchSort.PRICE_ASC);
        assertThatThrownBy(() -> conversion.convert("PRICE_ASC", SearchSort.class))
                .isInstanceOf(ConversionFailedException.class);
        // Andere Enums behalten die Standardumwandlung von Spring.
        assertThat(conversion.convert("SECONDS", TimeUnit.class)).isEqualTo(TimeUnit.SECONDS);
    }
}
