package net.keathmilligan.devmock.ttymock;

import net.keathmilligan.devmock.common.CLISession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class TTYSession extends CLISession implements Runnable {
    private static final Logger logger = LogManager.getLogger();

    private Socket socket;
    private TTYMockServer server;
    private Thread clientThread;
    private boolean terminate;

    TTYSession(Socket socket, TTYMockServer server) {
        super(server);
        this.socket = socket;
        this.server = server;
        logger.debug("creating session - remote IP: {}:{}", remoteAddress(), remotePort());

        clientThread = new Thread(this);
        clientThread.start();
    }

    public String remoteAddress() {
        return ((InetSocketAddress)socket.getRemoteSocketAddress()).getAddress().getHostAddress();
    }

    public int remotePort() {
        return ((InetSocketAddress)socket.getRemoteSocketAddress()).getPort();
    }

    @Override
    public void run() {
        ThreadContext.push("{}:{}/{}:{}",
                server.getConfig().bindAddress(),
                server.getConfig().port(),
                remoteAddress(),
                remotePort());
        sessionContext.put("remoteAddress", remoteAddress());

        OutputStream rawOut;
        PrintWriter out;
        BufferedReader in;
        try {
            socket.setSoTimeout(1000);
            rawOut = socket.getOutputStream();
            out = new PrintWriter(rawOut, true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            logger.fatal("session initialization failed");
            e.printStackTrace();
            throw new RuntimeException();
        }

        byte[] connectString = server.getConnectString();
        var welcomeMessage = server.getWelcomeMessage();
        var prompt = server.getPrompt();

        // output the connect string (e.g. telnet control chars)
        if (connectString != null) {
            try {
                rawOut.write(connectString);
                rawOut.flush();
                logger.trace("SEND RAW: {} bytes", connectString.length);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // output the welcome message
        if (welcomeMessage != null) {
            emit(out, welcomeMessage, false);
        }

        terminate = false;
        var sendPrompt = true;
        while(!terminate) {
            if (prompt != null && sendPrompt) {
                out.write(prompt);
                out.flush();
            }
            sendPrompt = true;
            try {
                var data = in.readLine();
                if (data == null) {
                    logger.info("session closed by remote");
                    break;
                }

                handleCommand(data.trim(), out);

            } catch (SocketTimeoutException e) {
                // allow loop to check for terminate
                sendPrompt = false;
            } catch (IOException e) {
                logger.debug(e.toString());
                break;
            }
        }

        logger.info("terminating session");
        try {
            socket.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        server.removeSession(this);
    }

    void kill() {
        terminate = true;
        try {
            clientThread.join(10000);
        } catch (InterruptedException e) {
            logger.fatal("could not kill client thread");
            e.printStackTrace();
            throw new RuntimeException();
        }
    }
}
