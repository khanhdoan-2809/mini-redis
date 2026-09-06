package io.odyssey.miniredis.command.key;

import io.odyssey.miniredis.command.CommandExecutionException;
import io.odyssey.miniredis.command.CommandRequest;
import io.odyssey.miniredis.command.RedisCommand;
import io.odyssey.miniredis.datastore.RedisDatabase;
import io.odyssey.miniredis.datastore.RedisKey;
import io.odyssey.miniredis.protocol.RespInteger;
import io.odyssey.miniredis.protocol.RespValue;

import java.util.Objects;

public final class DelCommand implements RedisCommand {

    private final RedisDatabase database;

    public DelCommand(RedisDatabase database) {
        this.database = Objects.requireNonNull(database);
    }

    @Override
    public RespValue execute(CommandRequest request) {
        if (request.arguments().isEmpty()) {
            throw new CommandExecutionException("ERR wrong number of arguments for 'del' command");
        }

        var keys = request.arguments().stream()
                .map(RedisKey::from)
                .toList();

        var deleted = database.delete(keys);

        return new RespInteger(deleted);
    }
}