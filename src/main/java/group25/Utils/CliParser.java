package group25.Utils;

import org.apache.commons.cli.*;

import java.util.HashMap;

import static group25.Utils.AnsiColors.RED;

public class CliParser {

    private static final String HOSTNAME_SHORT = "h";
    private static final String HOSTNAME_LONG = "hostname";
    private static final String HOSTNAME_DESC = HOSTNAME_LONG+" or IP address";
    private static final String PORT_SHORT = "p";
    private static final String PORT_LONG = "port";
    private static final String PORT_DESC = PORT_LONG+" number";

    static enum Arg {

        MIDDLEWARE("mw"),
        CAR("c"),
        FLIGHT("f"),
        ROOM("r"),
        CUSTOMER("cu"),
        ;

        private final String shortName;
        private final String longName;

        Arg(String arg) {
            this.shortName = arg;
            this.longName = this.name().toLowerCase();
        }

        public static Arg fromShortName(String sn) {
            HashMap<String, Arg> m = new HashMap<>();
            for (Arg a : Arg.values())
                m.put(a.shortName,a);
            return m.get(sn);
        }
    }

    public static final String MIDDLEWARE_HOSTNAME = Arg.MIDDLEWARE.shortName+HOSTNAME_SHORT;
    public static final String MIDDLEWARE_PORT = Arg.MIDDLEWARE.shortName+PORT_SHORT;
    public static final String CAR_HOSTNAME = Arg.CAR.shortName+HOSTNAME_SHORT;
    public static final String CAR_PORT = Arg.CAR.shortName+PORT_SHORT;
    public static final String FLIGHT_HOSTNAME = Arg.FLIGHT.shortName+HOSTNAME_SHORT;
    public static final String FLIGHT_PORT = Arg.FLIGHT.shortName+PORT_SHORT;
    public static final String ROOM_HOSTNAME = Arg.ROOM.shortName+HOSTNAME_SHORT;
    public static final String ROOM_PORT = Arg.ROOM.shortName+PORT_SHORT;
    public static final String CUSTOMER_HOSTNAME = Arg.CUSTOMER.shortName+HOSTNAME_SHORT;
    public static final String CUSTOMER_PORT = Arg.CUSTOMER.shortName+PORT_SHORT;

    private Options options;
    private CommandLine parsedCli;

    public CliParser(String[] toParse, String[] optStrings) {
        options = new Options();
        for (String optString : optStrings)
            options.addOption(optionFrom(optString));

        try {
            parsedCli = new DefaultParser().parse(options, toParse);
        } catch (ParseException e) {
            printHelpMessageAndErrorAndExit(e);
        }
    }

    public boolean parsedArg(String optString) {
        return parsedCli.hasOption(optString);
    }

    public int getParsedPort(String optString) {
        try {
            return Integer.parseInt(parsedCli.getOptionValue(optString));
        } catch (NumberFormatException e) {
            printHelpMessageAndErrorAndExit(e);
        }
        return -1;
    }

    public String getParsedHostname(String optString) {
        return parsedCli.getOptionValue(optString);
    }

    private Option optionFrom(String optString) {
        boolean hostnameNOTport = forHostnameNotPort(optString);
        Arg arg = Arg.fromShortName(optString.substring(0,optString.length()-1));

        if (hostnameNOTport) {
            return Option
                    .builder(optString)
                    .longOpt(arg.longName+"-"+HOSTNAME_LONG)
                    .argName(HOSTNAME_LONG)
                    .desc(arg.longName+" "+HOSTNAME_DESC)
                    .hasArg()
                    .build();
        } else {
            return Option
                    .builder(optString)
                    .longOpt(arg.longName+"-"+PORT_LONG)
                    .argName(PORT_LONG)
                    .desc(arg.longName+" "+PORT_DESC)
                    .hasArg()
                    .type(Integer.class)
                    .build();
        }
    }

    private boolean forHostnameNotPort(String toParse) {
        char lastLetter = toParse.charAt(toParse.length()-1);
        if (lastLetter == 'h') {
            return true; // hostname
        } else if (lastLetter == 'p') {
            return false; // port
        }
        return false; // never happens
    }

    private void printHelpMessageAndErrorAndExit(Exception e) {
        System.out.println(RED.colorString("Client Exception: ")+e.getMessage()+"\n");
        new HelpFormatter().printHelp("java RMIClient", options, true);
        System.exit(1);
    }
}
