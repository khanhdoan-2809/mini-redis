package io.odyssey.miniredis.protocol;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class RespEncoder {

    private static final byte[] CRLF =
            "\r\n".getBytes(
                    StandardCharsets.US_ASCII
            );

    public byte[] encode(
            RespValue value
    ) {

        Objects.requireNonNull(value);

        var output = new ByteArrayOutputStream();
        writeValue(output, value);

        return output.toByteArray();
    }

    private void writeValue(ByteArrayOutputStream output, RespValue value) {
        switch (value) {
            case RespSimpleString simple -> {
                writeAscii(output, "+");
                writeUtf8(output, simple.value());
                writeCrlf(output);
            }

            case RespError error -> {
                writeAscii(output, "-");
                writeUtf8(output, error.message());
                writeCrlf(output);
            }

            case RespInteger integer -> {
                writeAscii(output, ":" + integer.value());
                writeCrlf(output);
            }

            case RespBulkString bulk -> {
                var bulkValue = bulk.value();
                writeAscii(output,"$" + bulkValue.length);
                writeCrlf(output);
                output.writeBytes(bulkValue);
                writeCrlf(output);
            }

            case RespNullBulkString ignored -> {
                writeAscii(output, "$-1");
                writeCrlf(output);
            }

            case RespArray array -> {
                writeAscii(output, "*" + array.values().size());
                writeCrlf(output);
                for (var element : array.values()) {
                    writeValue(output, element);
                }
            }

            case RespNullArray ignored -> {
                writeAscii(output, "*-1");
                writeCrlf(output);
            }
        }
    }

    private void writeAscii(ByteArrayOutputStream output, String value) {
        output.writeBytes(value.getBytes(StandardCharsets.US_ASCII));
    }

    private void writeUtf8(
            ByteArrayOutputStream output,
            String value
    ) {

        output.writeBytes(
                value.getBytes(
                        StandardCharsets.UTF_8
                )
        );
    }

    private void writeCrlf(ByteArrayOutputStream output) {
        output.writeBytes(CRLF);
    }
}