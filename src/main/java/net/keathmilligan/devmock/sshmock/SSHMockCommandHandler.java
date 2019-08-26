package net.keathmilligan.devmock.sshmock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SSHMockCommandHandler implements Command {
    private static final Logger logger = LogManager.getLogger();
    private SSHMockServer server;
    private ChannelSession channel;
    private String command;
    private InputStream in;
    private OutputStream out;
    private OutputStream err;
    private ExitCallback callback;

    SSHMockCommandHandler(SSHMockServer server, ChannelSession channel, String command) {
        this.server = server;
        this.channel = channel;
        this.command = command;
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
    public void start(ChannelSession channel, Environment env) throws IOException {

    }

    @Override
    public void destroy(ChannelSession channel) throws Exception {

    }
}
