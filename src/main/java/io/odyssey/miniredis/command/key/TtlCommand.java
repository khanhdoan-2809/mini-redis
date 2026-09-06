package io.odyssey.miniredis.command.key;

import io.odyssey.miniredis.command.CommandExecutionException;
import io.odyssey.miniredis.command.CommandRequest;
import io.odyssey.miniredis.command.RedisCommand;
import io.odyssey.miniredis.datastore.RedisDatabase;
import io.odyssey.miniredis.datastore.RedisKey;
import io.odyssey.miniredis.protocol.RespInteger;
import io.odyssey.miniredis.protocol.RespValue;

import java.util.Objects;

public class TtlCommand implements RedisCommand {

    private final RedisDatabase database;

    public TtlCommand(RedisDatabase database) {
        this.database = Objects.requireNonNull(database);
    }

    @Override
    public RespValue execute(CommandRequest request) {
        if (request.arguments().size() != 1) {
            throw new CommandExecutionException("ERR wrong number of arguments for 'ttl' command");
        }

        var key = new RedisKey(request.arguments().getFirst().value());

        return new RespInteger(database.ttl(key));
    }
}
