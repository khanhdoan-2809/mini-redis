package io.odyssey.miniredis.command.string;

import io.odyssey.miniredis.command.CommandExecutionException;
import io.odyssey.miniredis.command.CommandRequest;
import io.odyssey.miniredis.command.RedisCommand;
import io.odyssey.miniredis.datastore.RedisDatabase;
import io.odyssey.miniredis.datastore.RedisKey;
import io.odyssey.miniredis.datastore.RedisString;
import io.odyssey.miniredis.protocol.RespSimpleString;
import io.odyssey.miniredis.protocol.RespValue;

import java.util.Objects;

public final class SetCommand implements RedisCommand {

    private final RedisDatabase database;

    public SetCommand(RedisDatabase database) {
        this.database = Objects.requireNonNull(database);
    }

    @Override
    public RespValue execute(CommandRequest request) {
        if (request.arguments().size() != 2) {
            throw new CommandExecutionException("ERR wrong number of arguments for 'set' command");
        }

        var key = RedisKey.from(request.arguments().get(0));
        var value = new RedisString(request.arguments().get(1).value());

        database.set(key, value);

        return new RespSimpleString("OK");
    }
}