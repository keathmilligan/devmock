package net.keathmilligan.devmock.common;

import com.github.mustachejava.DefaultMustacheFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;

public abstract class CLISession {
    private static final Logger logger = LogManager.getLogger();

    protected CLIMockServer server;
    protected HashMap<String, String> sessionContext = new HashMap<>();

    public CLISession(CLIMockServer server) {
        this.server = server;
    }

    private String renderTemplate(String template) {
        var props = new HashMap<String, Object>(sessionContext);

        // get response properties from provider
        props.putAll(server.getConfig().responsePropertiesProvider().getResponseProperties());

        var mf = new DefaultMustacheFactory();
        var m = mf.compile(new StringReader(template), "template");
        var w = new StringWriter();
        m.execute(w, props);
        return w.toString();
    }

    protected void emit(PrintWriter out, String template, boolean includeEol) {
        var output = renderTemplate(template);
        if (includeEol) {
            output += server.getEol();
        }
        out.write(output);
        out.flush();
        server.writeJournal(this, true, output);
    }

    public abstract String remoteAddress();

    public abstract int remotePort();

    protected void handleCommand(String receivedCommand, PrintWriter out) {
        // process command line input
        logger.trace("RECV {} bytes: {}", receivedCommand.length(), receivedCommand);

        // make the command string available to template
        sessionContext.put("command", receivedCommand);

        server.writeJournal(this, false, receivedCommand);

        // find the response mapping or default
        var mapping = server.findMapping(receivedCommand);

        // add matched elements
        for (int i = 0; i < 10; i++) {
            sessionContext.remove("match" + i);
        }
        if (mapping.getCommand().length() > 0) {
            var m = mapping.getPattern().matcher(receivedCommand);
            if (m.find() && m.groupCount() > 0) {
                for (int i = 1; i <= m.groupCount(); i++) {
                    sessionContext.put("match" + i, m.group(i));
                }
            }
        }

        // output the result
        emit(out, mapping.getResponse(), mapping.isIncludeEol());
    }
}
