package de.foodplanner.domain;

public class NumberParseException extends IllegalArgumentException {

    public NumberParseException(String message) {
        super(message);
    }
}
