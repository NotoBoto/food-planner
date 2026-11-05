package de.foodplanner.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class GermanNumberParserTest {

    @Test
    void parsesDecimalWithUnit() {
        assertThat(GermanNumberParser.parse("33,8 g")).isEqualByComparingTo("33.8");
    }

    @Test
    void parsesThousandsSeparatorAsGrouping() {
        assertThat(GermanNumberParser.parse("2.342")).isEqualByComparingTo("2342");
        assertThat(GermanNumberParser.parse("2.342 kJ")).isEqualByComparingTo("2342");
    }

    @Test
    void rejectsPrefixOnlyParse() {
        assertThatThrownBy(() -> GermanNumberParser.parse("12,5abc"))
                .isInstanceOf(NumberParseException.class);
    }

    @Test
    void rejectsGarbageAndEmpty() {
        assertThatThrownBy(() -> GermanNumberParser.parse("abc")).isInstanceOf(NumberParseException.class);
        assertThatThrownBy(() -> GermanNumberParser.parse("")).isInstanceOf(NumberParseException.class);
        assertThatThrownBy(() -> GermanNumberParser.parse(null)).isInstanceOf(NumberParseException.class);
        assertThatThrownBy(() -> GermanNumberParser.parse("1,2,3")).isInstanceOf(NumberParseException.class);
    }

    @Test
    void acceptsAldisWrongKcalUnit() {
        // "Energie [kcal]" steht bei Aldi mit der Einheit "g"
        assertThat(GermanNumberParser.parse("553 g")).isEqualByComparingTo("553");
    }

    @Test
    void returnsBigDecimalWithoutBinaryRoundingError() {
        assertThat(GermanNumberParser.parse("0,1")).isEqualTo(new BigDecimal("0.1"));
    }
}
