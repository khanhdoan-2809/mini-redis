package io.odyssey.miniredis.command.string;

import io.odyssey.miniredis.command.CommandExecutionException;
import io.odyssey.miniredis.command.CommandRequest;
import io.odyssey.miniredis.command.RedisCommand;
import io.odyssey.miniredis.datastore.RedisDatabase;
import io.odyssey.miniredis.datastore.RedisKey;
import io.odyssey.miniredis.datastore.RedisString;
import io.odyssey.miniredis.protocol.RespBulkString;
import io.odyssey.miniredis.protocol.RespNullBulkString;
import io.odyssey.miniredis.protocol.RespValue;

import java.util.Objects;

public final class GetCommand implements RedisCommand {

    private final RedisDatabase database;

    public GetCommand(RedisDatabase database) {
        this.database = Objects.requireNonNull(database);
    }

    @Override
    public RespValue execute(CommandRequest request) {
        if (request.arguments().size() != 1) {
            throw new CommandExecutionException("ERR wrong number of arguments for 'get' command");
        }

        var key = RedisKey.from(request.arguments().getFirst());
        var value = database.get(key);

        if (value.isEmpty()) {
            return RespNullBulkString.INSTANCE;
        }

        if (!(value.get() instanceof RedisString string)) {
            throw new CommandExecutionException("WRONGTYPE Operation against a key holding the wrong kind of value");
        }

        return new RespBulkString(string.value());
    }
}