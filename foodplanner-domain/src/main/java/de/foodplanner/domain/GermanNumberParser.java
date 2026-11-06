package de.foodplanner.domain;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class GermanNumberParser {

    private static final Pattern NUMBER_WITH_UNIT =
            Pattern.compile("^(\\S+?)(?:\\s+(?:g|kg|mg|ml|l|kj|kcal))?$", Pattern.CASE_INSENSITIVE);

    private GermanNumberParser() {
    }

    public static BigDecimal parse(String input) {
        if (input == null || input.isBlank()) {
            throw new NumberParseException("Leere Eingabe");
        }
        Matcher matcher = NUMBER_WITH_UNIT.matcher(input.strip());
        if (!matcher.matches()) {
            throw new NumberParseException("Keine gültige Zahl: '" + input + "'");
        }
        String number = matcher.group(1);

        DecimalFormat format = (DecimalFormat) NumberFormat.getInstance(Locale.GERMANY);
        format.setParseBigDecimal(true);

        ParsePosition position = new ParsePosition(0);
        Number parsed = format.parse(number, position);
        if (parsed == null || position.getErrorIndex() >= 0 || position.getIndex() != number.length()) {
            throw new NumberParseException("Zahl nicht vollständig lesbar: '" + input + "'");
        }
        return (BigDecimal) parsed;
    }
}
