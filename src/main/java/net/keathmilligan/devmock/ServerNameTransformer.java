package net.keathmilligan.devmock;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;

public class ServerNameTransformer extends ResponseDefinitionTransformer {

    private String serverName;

    ServerNameTransformer(String serverName) {
        this.serverName = serverName;
    }

    @Override
    public ResponseDefinition transform(Request request, ResponseDefinition responseDefinition, FileSource files, Parameters parameters) {
        if (serverName != null) {
            return ResponseDefinitionBuilder.like(responseDefinition).but()
                    .withHeader("Server", serverName)
                    .build();
        } else {
            return responseDefinition;
        }
    }

    @Override
    public String getName() {
        return ServerNameTransformer.class.getName();
    }
}
