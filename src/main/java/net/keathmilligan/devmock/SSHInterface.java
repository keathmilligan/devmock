package net.keathmilligan.devmock;

import net.keathmilligan.devmock.common.Configuration;
import net.keathmilligan.devmock.sshmock.SSHMockServer;
import net.keathmilligan.devmock.common.ResponsePropertiesProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SSHInterface extends MockInterface implements ResponsePropertiesProvider {
    private static final Logger logger = LogManager.getLogger();

    private int port;
    private SSHMockServer smServer;

    SSHInterface(MockDevice device, String mappingsDir, int port) {
        super(device, mappingsDir);
        this.device = device;
        this.port = port;
        smServer = new SSHMockServer(Configuration.options()
                .bindAddress(device.getIpAddress())
                .port(port)
                .mappingsDir(mappingsDir)
                .journalDir(DevMockServer.getInstance().getJournalDir())
                .name(device.getName() + "-ssh")
                .responsePropertiesProvider(this));
        logger.debug("{}: {} ssh interface created: post {}",
                device.getName(), device.getIpAddress(), port);
    }

    @Override
    void addMappings(List<String> mappings) {
        this.mappings.addAll(mappings);
    }

    @Override
    void start() {
        logger.trace("{}: starting", device.getName());
        mappings.forEach(mapping -> {
            logger.debug("{}: adding mapping {}", device.getName(), mapping);
            try {
                smServer.addMapping(new String(Files.readAllBytes(Paths.get(mappingsDir, mapping))));
            } catch (NoSuchFileException e) {
                logger.error("{}: mapping not found: {}", device.getName(), mapping);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        smServer.start();
    }

    void stop() {
        logger.trace("{}: stopping", device.getName());
        smServer.stop();
    }

    void clearJournal() {
        logger.trace("{}: clearing command journal", device.getName());
        smServer.resetJournal();
    }

    void killSessions() {
        logger.trace("{}: killing all sessions", device.getName());
        smServer.killAllSessions();
    }

    List<String> listSessions() {
        List<String> output = new ArrayList<>();
        // TODO: get session list
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
