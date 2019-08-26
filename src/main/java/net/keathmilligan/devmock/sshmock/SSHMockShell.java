package net.keathmilligan.devmock.sshmock;

import net.keathmilligan.devmock.common.CLISession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.sshd.common.util.threads.ThreadUtils;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.SessionAware;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.session.ServerSession;

import java.io.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

public class SSHMockShell extends CLISession implements Command, SessionAware {
    private static final Logger logger = LogManager.getLogger();
    private InputStream in;
    private OutputStream out;
    private OutputStream err;
    private Executor executor;
    private ExitCallback callback;
    private ServerSession session;
    private ChannelSession channel;
    private Environment environment;

    SSHMockShell(SSHMockServer server) {
        super(server);
        executor = ThreadUtils.newSingleThreadExecutor("mockshell[0x" + Integer.toHexString(hashCode()) + "]");
    }

    @Override
    public void setSession(ServerSession session) {
        this.session = session;
    }

    @Override
    public void setInputStream(InputStream in) {
        this.in = in;
    }

    @Override
    public void setOutputStream(OutputStream out) {
        this.out = out;
    }

    @Override
    public void setErrorStream(OutputStream err) {
        this.err = err;
    }

    @Override
    public void setExitCallback(ExitCallback callback) {
        this.callback = callback;
    }

    @Override
    public void start(ChannelSession channel, Environment env) {
        this.channel = channel;
        this.environment = env;
        executor.execute(this::pumpStreams);
    }

    @Override
    public void destroy(ChannelSession channel) throws Exception {
        in.close();
        out.close();
        err.close();
        try {
            ((ExecutorService) executor).shutdown();
        } catch (Exception e) {
            logger.warn("failed to shutdown executor: {} {} {}", this, e.getClass().getSimpleName(), e.getMessage());
        }
    }

    @Override
    public String remoteAddress() {
        // TODO: get remote IP address
        return "0.0.0.0";
    }

    @Override
    public int remotePort() {
        // TODO: get remote port
        return 0;
    }

    private void pumpStreams() {
        sessionContext.put("remoteAddress", remoteAddress());

        BufferedReader r = new BufferedReader(new InputStreamReader(in));
        PrintWriter w = new PrintWriter(out, true);

        byte[] connectString = server.getConnectString();
        var welcomeMessage = server.getWelcomeMessage();
        var prompt = server.getPrompt();
        if (connectString != null) {
            try {
                out.write(connectString);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // output the welcome message
        if (welcomeMessage != null) {
            emit(w, welcomeMessage, false);
        }

        try {
            for (;;) {
                if (prompt != null) {
                    w.write(prompt);
                    w.flush();
                }
                String s = r.readLine();
                if (s == null) {
                    logger.info("session closd by remote");
                    break;
                }

                handleCommand(s.trim(), w);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            callback.onExit(0);
        }

        logger.info("terminating session");
    }
}
