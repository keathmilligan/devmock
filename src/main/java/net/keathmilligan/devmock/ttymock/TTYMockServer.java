package net.keathmilligan.devmock.ttymock;

import net.keathmilligan.devmock.common.CLIMockServer;
import net.keathmilligan.devmock.common.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

public class TTYMockServer extends CLIMockServer implements Runnable {
    private static final Logger logger = LogManager.getLogger();
    private static final int ACCEPT_TIMEOUT = 1;
    private static final int SOCKET_BACKLOG = 5;
    private static final int MAX_SESSIONS = 5;

    private Thread serverThread;
    private boolean terminate;
    private List<TTYSession> sessionList = new ArrayList<>();

    public TTYMockServer(Configuration config) {
        super(config);
    }

    public void start() {
        serverThread = new Thread(this);
        resetJournal();
        serverThread.start();
    }

    public void stop() {
        terminate = true;
        try {
            serverThread.join(10000);
        } catch (InterruptedException e) {
            logger.error("could not terminate server thread");
            e.printStackTrace();
        }
        killAllSessions();
    }

    public void killAllSessions() {
        // kill all active sessions
        new ArrayList<>(sessionList).forEach(TTYSession::kill);
    }

    synchronized void removeSession(TTYSession session) {
        sessionList.remove(session);
    }

    public List<TTYSession> getSessionList() {
        return sessionList;
    }

    @Override
    public void run() {
        terminate = false;

        ThreadContext.push("{}:{}", config.bindAddress(), config.port());

        ServerSocket socket;
        try {
            socket = new ServerSocket(config.port(), SOCKET_BACKLOG, InetAddress.getByName(config.bindAddress()));
            socket.setSoTimeout(ACCEPT_TIMEOUT);
        } catch (IOException e) {
            logger.error("could not create server socket");
            e.printStackTrace();
            throw new RuntimeException();
        }

        while(!terminate) {
            try {
                var clientSocket = socket.accept();
                if (sessionList.size() < MAX_SESSIONS) {
                    synchronized(this) {
                        sessionList.add(new TTYSession(clientSocket, this));
                    }
                } else {
                    logger.error("Max # of sessions reached, rejecting connection");
                    clientSocket.close();
                }
            } catch (SocketTimeoutException e) {
                // check for terminate
            } catch (IOException e) {
                logger.error("error accepting connection");
                e.printStackTrace();
                throw new RuntimeException();
            }
        }

        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
