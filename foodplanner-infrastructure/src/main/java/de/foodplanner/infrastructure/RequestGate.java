package de.foodplanner.infrastructure;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

public final class RequestGate {

    private final Clock clock;
    private final Sleeper sleeper;
    private final Duration delay;
    private Instant nextRequest;

    public RequestGate(Clock clock, Sleeper sleeper, Duration delay) {
        this.clock = clock;
        this.sleeper = sleeper;
        this.delay = delay;
        this.nextRequest = clock.instant();
    }

    public synchronized void awaitPermission() throws InterruptedException {
        Instant now = clock.instant();
        if (nextRequest.isAfter(now)) {
            sleeper.sleep(Duration.between(now, nextRequest));
        }
        nextRequest = clock.instant().plus(delay);
    }
}
