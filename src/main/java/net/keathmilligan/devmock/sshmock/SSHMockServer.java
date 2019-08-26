package net.keathmilligan.devmock.sshmock;

import net.keathmilligan.devmock.common.CLIMockServer;
import net.keathmilligan.devmock.common.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;

import java.io.IOException;

public class SSHMockServer extends CLIMockServer {
    private static final Logger logger = LogManager.getLogger();
    private SshServer sshServer;

    public SSHMockServer(Configuration config) {
        super(config);
        sshServer = SshServer.setUpDefaultServer();
        sshServer.setHost(config.bindAddress());
        sshServer.setPort(config.port());
        sshServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
        sshServer.setPasswordAuthenticator((username, password, session) -> {
            return true;
        });
        sshServer.setShellFactory((channel) -> new SSHMockShell(this));
        sshServer.setCommandFactory(((channel, command) -> new SSHMockCommandHandler(this, channel, command)));
    }

    @Override
    public void start() {
        resetJournal();
        try {
            sshServer.start();
        } catch (IOException e) {
            logger.error(e);
        }
    }

    @Override
    public void stop() {
        try {
            sshServer.stop();
        } catch (IOException e) {
            logger.error(e);
        }
    }

    @Override
    public void killAllSessions() {
        // TODO: support killing sessions
    }
}
