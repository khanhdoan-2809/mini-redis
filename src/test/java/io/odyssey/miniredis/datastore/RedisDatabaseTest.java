package io.odyssey.miniredis.datastore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class RedisDatabaseTest {

    private final AtomicLong now = new AtomicLong(1_000_000);
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

    @Test
    void shouldExpireExistingKey() {
        var key = key("session");

        database.set(key, string("abc"));

        var result = database.expire(key, 10);

        assertThat(result).isTrue();
        assertThat(database.ttl(key)).isEqualTo(10);
    }


    @Test
    void shouldNotExpireMissingKey() {
        var result = database.expire(key("missing"), 10);

        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnMinusOneWhenKeyHasNoExpiration() {
        var key = key("name");

        database.set(key, string("Alice"));

        assertThat(database.ttl(key)).isEqualTo(-1);
    }

    @Test
    void shouldReturnMinusTwoWhenKeyDoesNotExist() {
        assertThat(database.ttl(key("missing"))).isEqualTo(-2);
    }

    @Test
    void shouldDecreaseTtlAsTimePasses() {
        var key = key("session");

        database.set(key, string("abc"));
        database.expire(key, 10);

        advance(Duration.ofSeconds(4));

        assertThat(database.ttl(key)).isEqualTo(6);
    }

    @Test
    void shouldLazilyDeleteExpiredKey() {
        var key = key("session");

        database.set(key, string("abc"));
        database.expire(key, 10);

        advance(Duration.ofSeconds(11));

        assertThat(database.get(key)).isEmpty();
        assertThat(database.ttl(key)).isEqualTo(-2);
    }

    @Test
    void shouldRemoveExpirationWhenValueIsOverwritten() {
        var key = key("name");

        database.set(key, string("Alice"));
        database.expire(key, 10);

        database.set(key, string("Bob"));

        assertThat(database.ttl(key)).isEqualTo(-1);
        assertThat(database.get(key)).contains(string("Bob"));
    }

    @Test
    void shouldDeleteImmediatelyForNonPositiveExpiration() {
        var key = key("session");

        database.set(key, string("abc"));

        var result = database.expire(key, 0);

        assertThat(result).isTrue();
        assertThat(database.get(key)).isEmpty();
    }

    @Test
    void shouldDeleteExpiredKeysDuringActiveExpiration() {
        var first = key("first");
        var second = key("second");

        database.set(first, string("1"));
        database.set(second, string("2"));

        database.expire(first, 5);
        database.expire(second, 10);

        advance(Duration.ofSeconds(6));

        var deleted = database.deleteExpiredKeys();

        assertThat(deleted).isEqualTo(1);
        assertThat(database.get(first)).isEmpty();
        assertThat(database.get(second)).contains(string("2"));
    }

    private void advance(Duration duration) {
        now.addAndGet(duration.toMillis());
    }

    private RedisKey key(String value) {
        return new RedisKey(value.getBytes(StandardCharsets.UTF_8));
    }

    private RedisString string(String value) {
        return new RedisString(value.getBytes(StandardCharsets.UTF_8));
    }
}