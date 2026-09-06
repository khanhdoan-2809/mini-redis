package io.odyssey.miniredis.command.key;

import io.odyssey.miniredis.command.CommandExecutionException;
import io.odyssey.miniredis.command.CommandRequest;
import io.odyssey.miniredis.command.RedisCommand;
import io.odyssey.miniredis.datastore.RedisDatabase;
import io.odyssey.miniredis.datastore.RedisKey;
import io.odyssey.miniredis.protocol.RespInteger;
import io.odyssey.miniredis.protocol.RespValue;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class ExpireCommand implements RedisCommand {

    private final RedisDatabase database;

    public ExpireCommand(RedisDatabase database) {
        this.database = Objects.requireNonNull(database);
    }

    @Override
    public RespValue execute(CommandRequest request) {
        if (request.arguments().size() != 2) {
            throw new CommandExecutionException("ERR wrong number of arguments for 'expire' command");
        }

        var key = new RedisKey(request.arguments().get(0).value());
        var seconds = parseSeconds(request.arguments().get(1).value());

        try {
            return new RespInteger(database.expire(key, seconds) ? 1 : 0);
        } catch (ArithmeticException e) {
            throw new CommandExecutionException("ERR invalid expire time in 'expire' command");
        }
    }

    private long parseSeconds(byte[] value) {
        try {
            return Long.parseLong(new String(value, StandardCharsets.US_ASCII));
        } catch (NumberFormatException e) {
            throw new CommandExecutionException("ERR value is not an integer or out of range");
        }
    }
}
