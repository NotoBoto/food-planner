package de.foodplanner.domain.shoppinglist;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class GoalsTest {

    @Test
    void setGoalsMustBeAtLeastOne() {
        assertThatThrownBy(() -> new Goals(0L, null, null, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Goals(null, -1L, null, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Goals(null, null, 0L, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Goals(null, null, null, 0L, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Goals(null, null, null, null, 0L)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unsetAndPositiveGoalsAreAccepted() {
        assertThatNoException().isThrownBy(() -> new Goals(null, null, null, null, null));
        assertThatNoException().isThrownBy(() -> new Goals(1L, 1L, 1L, 1L, 1L));
    }
}
