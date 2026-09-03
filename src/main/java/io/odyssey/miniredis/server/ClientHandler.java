package io.odyssey.miniredis.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ClientHandler.class);

    private static final int BUFFER_SIZE = 1024 * 8;

    private final Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try(socket) {
            log.debug("Client connected: {}", socket.getRemoteSocketAddress());
            var input = socket.getInputStream();
            var output = socket.getOutputStream();
            var buffer = new byte[BUFFER_SIZE];
            while (true) {
                var bytesRead = input.read(buffer);
                if (bytesRead == -1) {
                    break;
                }
                output.write(buffer, 0, bytesRead);
                output.flush();
            }
        } catch (IOException e) {
            if (!socket.isClosed()) {
                log.debug(
                        "Client connection failed",
                        e
                );
            }
        } finally {
            log.debug(
                    "Client disconnected: {}",
                    socket.getRemoteSocketAddress()
            );
        }
    }
}
