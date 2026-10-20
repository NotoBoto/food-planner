package de.foodplanner.architecture.fixtures.allowed.application;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AllowedQuery(@NotNull @Min(1) Integer amount) {
}
