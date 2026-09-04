package io.odyssey.miniredis.protocol;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RespDecoderTest {

    private final RespDecoder decoder = new RespDecoder();

    @Test
    void shouldDecodeSimpleString() {
        feed("+OK\r\n");
        var result = decoder.decodeOne();
        assertThat(result).contains(new RespSimpleString("OK"));
    }

    private void feed(String value) {
        var bytes = value.getBytes(StandardCharsets.US_ASCII);
        decoder.feed(bytes, 0, bytes.length);
    }

    @Test
    void shouldDecodeInteger() {
        feed(":100\r\n");
        assertThat(decoder.decodeOne())
                .contains(new RespInteger(100));
    }

    @Test
    void shouldDecodeNegativeInteger() {
        feed(":-10\r\n");
        assertThat(decoder.decodeOne())
                .contains(new RespInteger(-10));
    }

    @Test
    void shouldDecodeBulkString() {
        feed("$5\r\nhello\r\n");
        var value = decoder.decodeOne().orElseThrow();
        assertThat(value).isInstanceOf(RespBulkString.class);

        var bulk =(RespBulkString) value;
        assertThat(bulk.value())
                .containsExactly("hello".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void shouldHandleCommandArrivingOneByteAtATime() {
        var request = ("*2\r\n"
                                + "$3\r\nGET\r\n"
                                + "$4\r\nname\r\n"
                ).getBytes(StandardCharsets.US_ASCII);

        for (int i = 0; i < request.length - 1; i++) {
            decoder.feed(request, i, 1);
            assertThat(decoder.decodeOne()).isEmpty();
        }

        decoder.feed(request, request.length - 1, 1);
        assertThat(decoder.decodeOne()).isPresent();
    }

    @Test
    void shouldDecodeNullBulkString() {
        feed("$-1\r\n");
        assertThat(decoder.decodeOne())
                .contains(RespNullBulkString.INSTANCE);
    }

    @Test
    void shouldRejectUnknownPrefix() {
        feed("%hello\r\n");
        assertThatThrownBy(decoder::decodeOne)
                .isInstanceOf(RespProtocolException.class);
    }
}
