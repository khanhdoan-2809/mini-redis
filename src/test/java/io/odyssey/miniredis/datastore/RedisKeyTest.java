package io.odyssey.miniredis.datastore;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class RedisKeyTest {

    @Test
    void shouldCompareKeysByByteContent() {
        var first = new RedisKey("name".getBytes());
        var second = new RedisKey("name".getBytes());

        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    void shouldNotAllowOriginalByteArrayToModifyKey() {
        var bytes = "name".getBytes(StandardCharsets.UTF_8);
        var key = new RedisKey(bytes);

        bytes[0] = 'X';

        assertThat(new String(key.value(), StandardCharsets.UTF_8)).isEqualTo("name");
    }
}
