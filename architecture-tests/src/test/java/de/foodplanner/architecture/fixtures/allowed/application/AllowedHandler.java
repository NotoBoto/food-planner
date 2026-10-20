package de.foodplanner.architecture.fixtures.allowed.application;

import de.foodplanner.architecture.fixtures.allowed.domain.Money;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
public class AllowedHandler {

    private static final Logger log = LoggerFactory.getLogger(AllowedHandler.class);

    public Money handle(@Valid AllowedQuery query) {
        log.debug("handle {}", query);
        return new Money(query.amount());
    }
}
