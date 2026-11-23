package de.foodplanner.infrastructure;

import java.time.Duration;

@FunctionalInterface
public interface Sleeper {

    void sleep(Duration duration) throws InterruptedException;

    static Sleeper system() {
        return duration -> Thread.sleep(duration.toMillis());
    }
}
