package net.keathmilligan.devmock;

import com.github.jknack.handlebars.Helper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.FatalStartupException;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

class WebInterface extends MockInterface {
    private static final Logger logger = LogManager.getLogger();

    private WireMockServer wmServer;

    WebInterface(MockDevice device, String mappingsDir, String serverName, int httpPort, int httpsPort) {
        super(device, mappingsDir);

        // mustache helpers - return device info
        var helpers = new HashMap<String, Helper>();
        helpers.put("name", (context, options) -> device.getName());
        helpers.put("address", (context, options) -> device.getIpAddress());
        device.getInfo().forEach((key, item) -> helpers.put("info." + key, (context, options) -> item));

        wmServer = new WireMockServer(options()
                .bindAddress(device.getIpAddress())
                .port(httpPort)
                .httpsPort(httpsPort)
                .extensions(new ServerNameTransformer(serverName),
                        new StaticFileTransformer(device, mappingsDir),
                        new ResponseTemplateTransformer(true, helpers)
                ));
        logger.debug("{}:{} web interface created: http {} / https {}",
                device.getName(), device.getIpAddress(), httpPort, httpsPort);
    }

    void addMappings(List<String> mappings) {
        this.mappings.addAll(mappings);
    }

    void start() {
        logger.trace("{}: starting", device.getName());
        mappings.forEach(mapping -> {
            logger.debug("{}: adding mapping {}", device.getName(), mapping);
            try {
                var json = new String(Files.readAllBytes(Paths.get(mappingsDir, mapping)));
                wmServer.addStubMapping(StubMapping.buildFrom(json));
            } catch (NoSuchFileException e) {
                logger.error("{}: mapping not found", device.getName());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        try {
            wmServer.start();
        } catch(FatalStartupException e) {
            logger.error("{}: could not start interface: {}", device.getName(), e.toString());
        }
    }

    void stop() {
        logger.trace("{}: stopping", device.getName());
        wmServer.stop();
    }

    void clearJournal() {
        logger.trace("{}: clearing request journal", device.getName());
        wmServer.resetRequests();
    }

    void killSessions() {
        logger.trace("{}: killing sessions / resetting scenarios", device.getName());
        wmServer.resetScenarios();
    }

    List<String> listSessions() {
        return new ArrayList<>();
    }
}
