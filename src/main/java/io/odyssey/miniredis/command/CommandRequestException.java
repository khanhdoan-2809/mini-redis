package io.odyssey.miniredis.command;

public final class CommandRequestException extends RuntimeException {

    public CommandRequestException(String message) {
        super(message);
    }
}