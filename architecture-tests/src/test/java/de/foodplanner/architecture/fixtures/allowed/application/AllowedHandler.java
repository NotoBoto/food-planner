package de.foodplanner.architecture.fixtures.allowed.application;

import de.foodplanner.architecture.fixtures.allowed.domain.Money;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
public class AllowedHandler {

    public Money handle(@Valid AllowedQuery query) {
        return new Money(query.amount());
    }
}
