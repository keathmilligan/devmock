package net.keathmilligan.devmock;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;

public class StaticFileTransformer extends ResponseDefinitionTransformer {
    private static final Logger logger = LogManager.getLogger();
    private MockDevice device;
    private String mappingsDir;

    StaticFileTransformer(MockDevice device, String mappingsDir) {
        this.device = device;
        this.mappingsDir = mappingsDir;
    }

    @Override
    public ResponseDefinition transform(Request request, ResponseDefinition responseDefinition, FileSource files, Parameters parameters) {
        var fileName = responseDefinition.getBodyFileName();
        if (fileName != null) {
            logger.debug("serving static file: {}", fileName);
            String body = "";
            try {
                body = new String(Files.readAllBytes(Paths.get(mappingsDir, fileName)));
            } catch (IOException e) {
                logger.error("I/O exception reading file: " + fileName);
            }

            var rb = ResponseDefinitionBuilder.like(responseDefinition).but()
                    .withBodyFile(null)
                    // Wiremock will ignore this content-length header due to:
                    // https://github.com/tomakehurst/wiremock/issues/406
                    .withHeader("Content-Length", Integer.toString(body.length()));
            var contentType = URLConnection.guessContentTypeFromName(fileName);
            if (contentType != null) {
                logger.debug("guessed content-type: {}", contentType);
                rb.withHeader("Content-Type", contentType);
            }
            return rb.withBody(body).build();
        } else {
            return responseDefinition;
        }
    }

    @Override
    public String getName() {
        return StaticFileTransformer.class.getName();
    }
}
