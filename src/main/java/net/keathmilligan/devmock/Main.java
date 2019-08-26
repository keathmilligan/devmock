/**
 * Create and manage mock HTTP/S/REST and telnet network interfaces
 * Copyright 2018 Keath Milligan
 */

package net.keathmilligan.devmock;

import org.apache.commons.cli.*;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

import java.util.Scanner;

class Main {
    private static final Logger logger = LogManager.getLogger();

    public static void main(String args[]) {
        logger.info("DevMock server starting");
        var options = new Options();
        options.addOption(Option.builder("?").longOpt("help").desc("show command-line help and exit").build());
        options.addOption(Option.builder("c").longOpt("config").hasArg().desc("configuration file").build());
        options.addOption(Option.builder("v").longOpt("verbose").desc("enable verbose logging").build());
        var parser = new DefaultParser();
        try {
            var cmd = parser.parse(options, args);
            if (cmd.hasOption("help")) {
                var formatter = new HelpFormatter();
                formatter.printHelp("devmock", options);
                return;
            }
            if (cmd.hasOption("verbose")) {
                final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
                final Configuration config = ctx.getConfiguration();
                LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
                loggerConfig.setLevel(Level.ALL);
                ctx.updateLoggers();
            }

            DevMockServer devMockServer;
            if (cmd.hasOption("config")) {
                devMockServer = DevMockServer.create(cmd.getOptionValue("config"));
            } else {
                devMockServer = DevMockServer.create();
            }
            devMockServer.start();

            var scanner = new Scanner(System.in);
            var quit = false;
            while (!quit) {
                System.out.println("\nEnter command (\"quit\" to exit, \"?\" for help):");
                var command = scanner.nextLine();
                switch (command) {
                    case "?":
                    case "help":
                        System.out.println(
                                "rs      Restart server / reload mappings\n" +
                                "clear   Clear the command journal\n" +
                                "list    List sessions\n" +
                                "kill    Kill all sessions\n" +
                                "q[uit]  Stop server and exit\n" +
                                "?       Print this message\n"
                        );
                        break;
                    case "rs":
                        devMockServer.restart();
                        break;
                    case "clear":
                        devMockServer.clearJournals();
                        break;
                    case "list":
                        var sessions = devMockServer.listSessions();
                        sessions.forEach(System.out::println);
                        System.out.println(sessions.size() + " sessions");
                        break;
                    case "kill":
                        devMockServer.killSessions();
                        break;
                    case "quit":
                    case "q":
                        logger.info("exiting");
                        quit = true;
                        break;
                    default:
                        System.out.println("Invalid command - enter \"?\" for help");
                        break;
                }
            }

            devMockServer.stop();

        } catch (ParseException e) {
            System.err.println(e.getMessage());
            var formatter = new HelpFormatter();
            formatter.printHelp("devmock", options);
            System.exit(1);
        }
    }
}
