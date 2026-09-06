package io.odyssey.miniredis.command;

import io.odyssey.miniredis.protocol.RespArray;
import io.odyssey.miniredis.protocol.RespBulkString;
import io.odyssey.miniredis.protocol.RespValue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;

public final class CommandRequestParser {

    public CommandRequest parse(RespValue value) {
        if (!(value instanceof RespArray array)) {
            throw new CommandRequestException("Command request must be a RESP array");
        }

        if (array.values().isEmpty()) {
            throw new CommandRequestException("Command request must not be empty");
        }

        var commandValue = array.values().getFirst();

        if (!(commandValue instanceof RespBulkString commandBulkString)) {
            throw new CommandRequestException("Command name must be a bulk string");
        }

        var commandName = new String(commandBulkString.value(), StandardCharsets.US_ASCII).toUpperCase(Locale.ROOT);
        var arguments = new ArrayList<RespBulkString>(array.values().size() - 1);

        for (var i = 1; i < array.values().size(); i++) {
            var argument = array.values().get(i);

            if (!(argument instanceof RespBulkString bulkString)) {
                throw new CommandRequestException("Command arguments must be bulk strings");
            }

            arguments.add(bulkString);
        }

        return new CommandRequest(commandName, arguments);
    }
}