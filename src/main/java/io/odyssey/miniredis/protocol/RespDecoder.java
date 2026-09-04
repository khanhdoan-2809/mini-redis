package io.odyssey.miniredis.protocol;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class RespDecoder {

    private static final int INITIAL_BUFFER_SIZE = 8 * 1024;

    private static final int MAX_BUFFER_SIZE = 64 * 1024 * 1024;

    private static final int MAX_BULK_STRING_SIZE = 32 * 1024 * 1024;

    private static final int MAX_ARRAY_SIZE = 1_000_000;

    private static final int MAX_NESTING_DEPTH = 128;

    private byte[] buffer = new byte[INITIAL_BUFFER_SIZE];

    private int readPosition;

    private int writePosition;

    public void feed(byte[] bytes, int offset, int length) {
        if (length == 0) {
            return;
        }

        ensureWritable(length);

        System.arraycopy(
                bytes,
                offset,
                buffer,
                writePosition,
                length
        );
        writePosition += length;
    }

    public Optional<RespValue> decodeOne() {
        if (readPosition == writePosition) {
            return Optional.empty();
        }
        var cursor = new Cursor(readPosition);
        try {
            var value = parseValue(cursor, 0);
            readPosition = cursor.position;
            compactIfNecessary();
            return Optional.of(value);
        } catch (IncompleteRespException e) {
            return Optional.empty();
        }
    }

    private RespValue parseValue(Cursor cursor, int depth) {
        if (depth > MAX_NESTING_DEPTH) {
            throw new RespProtocolException(
                    "RESP nesting depth exceeded"
            );
        }

        requireAvailable(cursor, 1);

        byte prefix = buffer[cursor.position++];

        return switch (prefix) {
            case '+' -> parseSimpleString(cursor);

            case '-' -> parseError(cursor);

            case ':' -> parseInteger(cursor);

            case '$' -> parseBulkString(cursor);

            case '*' -> parseArray(
                            cursor,
                            depth + 1
                    );

            default ->
                    throw new RespProtocolException(
                            "Unknown RESP type prefix: "
                                    + (char) prefix
                    );
        };
    }

    private RespSimpleString parseSimpleString(Cursor cursor) {
        var line = readLine(cursor);

        return new RespSimpleString(new String(line, StandardCharsets.UTF_8));
    }

    private RespError parseError(Cursor cursor) {
        var line = readLine(cursor);

        return new RespError(
                new String(
                        line,
                        StandardCharsets.UTF_8
                )
        );
    }

    private RespInteger parseInteger(Cursor cursor) {
        var value = parseLong(readLine(cursor));
        return new RespInteger(value);
    }

    private RespValue parseBulkString(Cursor cursor) {
        var length = parseLong(readLine(cursor));

        if (length == -1) {
            return RespNullBulkString.INSTANCE;
        }

        if (length < -1) {
            throw new RespProtocolException(
                    "Invalid bulk string length: "
                            + length
            );
        }

        if (length > MAX_BULK_STRING_SIZE) {
            throw new RespProtocolException(
                    "Bulk string is too large: "
                            + length
            );
        }

        var size = Math.toIntExact(length);

        requireAvailable(cursor, size + 2);

        var valueStart = cursor.position;
        var valueEnd = valueStart + size;

        if (buffer[valueEnd] != '\r'
                || buffer[valueEnd + 1] != '\n') {

            throw new RespProtocolException(
                    "Bulk string must end with CRLF"
            );
        }

        var value = Arrays.copyOfRange(
                        buffer,
                        valueStart,
                        valueEnd
                );

        cursor.position = valueEnd + 2;

        return new RespBulkString(value);
    }

    private RespValue parseArray(Cursor cursor, int depth) {
        var length = parseLong(readLine(cursor));

        if (length == -1) {
            return RespNullArray.INSTANCE;
        }

        if (length < -1) {
            throw new RespProtocolException("Invalid array length: " + length);
        }

        if (length > MAX_ARRAY_SIZE) {
            throw new RespProtocolException("RESP array is too large: " + length);
        }

        var size = Math.toIntExact(length);

        List<RespValue> values = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            values.add(parseValue(cursor, depth));
        }

        return new RespArray(values);
    }

    private byte[] readLine(Cursor cursor) {
        var start = cursor.position;

        for (var i = start; i < writePosition; i++) {
            if (buffer[i] == '\n') {
                if (i == start || buffer[i - 1] != '\r') {
                    throw new RespProtocolException(
                            "RESP line must end with CRLF"
                    );
                }

                var line = Arrays.copyOfRange(
                                buffer,
                                start,
                                i - 1
                        );

                cursor.position = i + 1;
                return line;
            }
        }
        throw IncompleteRespException.INSTANCE;
    }

    private long parseLong(
            byte[] bytes
    ) {

        try {
            var value = new String(bytes, StandardCharsets.US_ASCII);
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new RespProtocolException(
                    "Invalid RESP integer",
                    e
            );
        }
    }

    private void requireAvailable(Cursor cursor, int required) {
        if (writePosition - cursor.position < required) {
            throw IncompleteRespException.INSTANCE;
        }
    }

    private void ensureWritable(int required) {
        if (buffer.length - writePosition >= required) {
            return;
        }
        compact();
        if (buffer.length - writePosition >= required) {
            return;
        }

        long requiredCapacity =
                (long) writePosition
                        + required;

        if (requiredCapacity
                > MAX_BUFFER_SIZE) {

            throw new RespProtocolException(
                    "RESP buffer limit exceeded"
            );
        }

        int newCapacity =
                buffer.length;

        while (
                newCapacity < requiredCapacity
        ) {

            newCapacity =
                    Math.min(
                            newCapacity * 2,
                            MAX_BUFFER_SIZE
                    );

            if (newCapacity
                    == MAX_BUFFER_SIZE) {

                break;
            }
        }

        buffer =
                Arrays.copyOf(
                        buffer,
                        newCapacity
                );
    }

    private void compactIfNecessary() {

        if (readPosition == writePosition) {

            readPosition = 0;
            writePosition = 0;

            return;
        }

        if (readPosition
                > buffer.length / 2) {

            compact();
        }
    }

    private void compact() {
        if (readPosition == 0) {
            return;
        }

        var unread = writePosition - readPosition;
        System.arraycopy(
                buffer,
                readPosition,
                buffer,
                0,
                unread
        );

        readPosition = 0;
        writePosition = unread;
    }

    private static final class Cursor {

        private int position;

        private Cursor(
                int position
        ) {
            this.position =
                    position;
        }
    }

    private static final class
    IncompleteRespException
            extends RuntimeException {

        private static final
        IncompleteRespException INSTANCE =
                new IncompleteRespException();

        private IncompleteRespException() {
            super(
                    null,
                    null,
                    false,
                    false
            );
        }
    }
}