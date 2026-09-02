package io.odyssey.miniredis;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MiniRedisApplicationTest {

    @Test
    void applicationStarts() {
        assertThat(MiniRedisApplication.class).isNotNull();
    }
}
