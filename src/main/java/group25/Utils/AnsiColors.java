package group25.Utils;

import java.util.Collection;

public enum AnsiColors {

    COLOR_OFF("0"),

    // regular
    BLACK("0;30"),
    RED("0;31"),
    GREEN("0;32"),
    YELLOW("0;33"),
    BLUE("0;34"),
    PURPLE("0;35"),
    CYAN("0;36"),
    WHITE("0;37"),

    // bold
    BBLACK("1;30"),
    BRed("1;31"),
    BGREEN("1;32"),
    BYELLOW("1;33"),
    BBLUE("1;34"),
    BPURPLE("1;35"),
    BCYAN("1;36"),
    BWHITE("1;37"),

    // underline
    UBLACK("4;30"),
    URED("4;31"),
    UGREEN("4;32"),
    UYELLOW("4;33"),
    UBLUE("4;34"),
    UPURPLE("4;35"),
    UCYAN("4;36"),
    UWHITE("4;37"),

    // background
    ON_BLACK("40"),
    ON_RED("41"),
    ON_GREEN("42"),
    ON_YELLOW("43"),
    ON_BLUE("44"),
    ON_PURPLE("45"),
    ON_CYAN("46"),
    ON_WHITE("47"),
    ;

    private static final String ANSI_ESCAPE = "\u001B";
    private static final String ANSI_COLOR_CODE_START = "[";
    private static final String ANSI_COLOR_CODE_END = "m";

    public static String colorString(Collection<AnsiColors> colors, String s) {
        StringBuilder sb = new StringBuilder();
        for (AnsiColors color : colors)
            sb.append(color);
        sb.append(s);
        sb.append(AnsiColors.COLOR_OFF);

        return sb.toString();
    }

    public final String ansiColorCode;

    AnsiColors(String coreAnsiColorCode) {
        this.ansiColorCode = ANSI_ESCAPE + ANSI_COLOR_CODE_START + coreAnsiColorCode + ANSI_COLOR_CODE_END;
    }

    @Override
    public String toString() {
        return this.ansiColorCode;
    }

    public String colorString(String s) {
        return this + s + COLOR_OFF;
    }
}
