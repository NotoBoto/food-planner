package de.foodplanner.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RequestGateTest {

    @Test
    void serializesConfiguredDelayBeforeRequests() throws InterruptedException {
        List<Duration> waits = new ArrayList<>();
        RequestGate gate = new RequestGate(
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC),
                waits::add,
                Duration.ofMillis(200));

        gate.awaitPermission();
        gate.awaitPermission();

        assertThat(waits).containsExactly(Duration.ofMillis(200));
    }
}
