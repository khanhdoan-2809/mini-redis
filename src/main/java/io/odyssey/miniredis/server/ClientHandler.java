package io.odyssey.miniredis.server;

import io.odyssey.miniredis.protocol.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ClientHandler.class);

    private static final int BUFFER_SIZE = 1024 * 8;

    private final Socket socket;

    private final RespDecoder decoder = new RespDecoder();

    private final RespEncoder encoder = new RespEncoder();

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        var remoteAddress = socket.getRemoteSocketAddress();

        try(socket) {
            log.debug("Client connected: {}", remoteAddress);
            var input = socket.getInputStream();
            var output = socket.getOutputStream();
            var readBuffer = new byte[BUFFER_SIZE];
            while (true) {
                var bytesRead = input.read(readBuffer );
                if (bytesRead == -1) {
                    break;
                }
                try {
                    decoder.feed(readBuffer, 0, bytesRead);
                    processFrames(output);
                } catch (RespProtocolException e) {
                    log.debug(
                            "Protocol error from {}: {}",
                            remoteAddress,
                            e.getMessage()
                    );

                    output.write(encoder.encode(new RespError("ERR Protocol error")));
                    output.flush();
                    break;
                }
            }
        }
        catch (IOException e) {
            if (!socket.isClosed()) {
                log.debug("Client connection failed: {}", remoteAddress, e);
            }
        } finally {
            log.debug("Client disconnected: {}", socket.getRemoteSocketAddress());
        }
    }

    private void processFrames(OutputStream output) throws IOException {
        while (true) {
            var decoded = decoder.decodeOne();
            if (decoded.isEmpty()) {
                return;
            }
            var request = decoded.get();
            log.debug("Decoded RESP frame: {}", request.getClass().getSimpleName());
            output.write(encoder.encode(new RespSimpleString("OK")));
            output.flush();
        }
    }
}
