package io.odyssey.miniredis.datastore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RedisDatabaseTest {

    private RedisDatabase database;

    @BeforeEach
    void setUp() {
        database = new RedisDatabase();
    }

    @Test
    void shouldSetAndGetValue() {
        var key = key("name");
        var value = string("Alice");

        database.set(key, value);

        assertThat(database.get(key)).contains(value);
    }

    @Test
    void shouldOverwriteExistingValue() {
        var key = key("name");

        database.set(key, string("Alice"));
        database.set(key, string("Bob"));

        assertThat(database.get(key)).contains(string("Bob"));
    }

    @Test
    void shouldReturnEmptyForMissingKey() {
        assertThat(database.get(key("missing"))).isEmpty();
    }

    @Test
    void shouldDeleteExistingKeys() {
        database.set(key("a"), string("1"));
        database.set(key("b"), string("2"));

        var deleted = database.delete(List.of(key("a"), key("b"), key("missing")));

        assertThat(deleted).isEqualTo(2);
        assertThat(database.size()).isZero();
    }

    @Test
    void shouldCountDeletedKeyOnlyOnceWhenRepeated() {
        database.set(key("name"), string("Alice"));

        var deleted = database.delete(List.of(key("name"), key("name")));

        assertThat(deleted).isEqualTo(1);
    }

    @Test
    void shouldCountExistingKeys() {
        database.set(key("a"), string("1"));
        database.set(key("b"), string("2"));

        var existing = database.exists(List.of(key("a"), key("b"), key("missing")));

        assertThat(existing).isEqualTo(2);
    }

    private RedisKey key(String value) {
        return new RedisKey(value.getBytes(StandardCharsets.UTF_8));
    }

    private RedisString string(String value) {
        return new RedisString(value.getBytes(StandardCharsets.UTF_8));
    }
}