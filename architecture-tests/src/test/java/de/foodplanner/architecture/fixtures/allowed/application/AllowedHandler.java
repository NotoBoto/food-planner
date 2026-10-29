package de.foodplanner.architecture.fixtures.allowed.application;

import de.foodplanner.architecture.fixtures.allowed.domain.Money;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
public class AllowedHandler {

    private static final Logger log = LoggerFactory.getLogger(AllowedHandler.class);

    @Cacheable("money")
    public Money handle(@Valid AllowedQuery query) {
        log.atDebug().addKeyValue("query", query).log("handle");
        return new Money(query.amount());
    }

    @Async
    @Scheduled(fixedDelay = 60_000)
    public void refresh() {
        log.debug("refresh");
    }
}
