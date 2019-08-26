package net.keathmilligan.devmock;

import net.keathmilligan.devmock.common.ResponsePropertiesProvider;
import net.keathmilligan.devmock.ttymock.TTYMockServer;
import net.keathmilligan.devmock.common.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

class TTYInterface extends MockInterface implements ResponsePropertiesProvider {
    private static final Logger logger = LogManager.getLogger();

    private int port;
    private TTYMockServer tmServer;

    TTYInterface(MockDevice device, String mappingsDir, int port) {
        super(device, mappingsDir);
        this.device = device;
        this.port = port;
        tmServer = new TTYMockServer(Configuration.options()
                .bindAddress(device.getIpAddress())
                .port(port)
                .mappingsDir(mappingsDir)
                .journalDir(DevMockServer.getInstance().getJournalDir())
                .name(device.getName() + "-tty")
                .responsePropertiesProvider(this));
        logger.debug("{}:{} tty interface created: port {}",
                device.getName(), device.getIpAddress(), port);
    }

    void addMappings(List<String> mappings) {
        this.mappings.addAll(mappings);
    }

    void start() {
        logger.trace("{}: starting", device.getName());
        mappings.forEach(mapping -> {
            logger.debug("{}: adding mapping {}", device.getName(), mapping);
            try {
                tmServer.addMapping(new String(Files.readAllBytes(Paths.get(mappingsDir, mapping))));
            } catch (NoSuchFileException e) {
                logger.error("{}: mapping not found: {}", device.getName(), mapping);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        tmServer.start();
    }

    void stop() {
        logger.trace("{}: stopping", device.getName());
        tmServer.stop();
    }

    void clearJournal() {
        logger.trace("{}: clearing command journal", device.getName());
        tmServer.resetJournal();
    }

    void killSessions() {
        logger.trace("{}: killing all sessions", device.getName());
        tmServer.killAllSessions();
    }

    List<String> listSessions() {
        List<String> output = new ArrayList<>();
        tmServer.getSessionList().forEach(session -> output.add(
                String.format("tty:%d:%s", port, session.remoteAddress())));
        return output;
    }

    @Override
    public HashMap<String, Object> getResponseProperties() {
        var props = new HashMap<String, Object>();

        // interface properties
        props.put("port", port);

        // device properties
        props.putAll(device.getResponseProperties());

        return props;
    }
}
