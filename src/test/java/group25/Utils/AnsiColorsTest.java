package group25.Utils;

import org.junit.Test;

import java.util.Arrays;

public class AnsiColorsTest {
    @Test
    public void doTest() {
        AnsiColors[] colors = AnsiColors.values();
        String s1 = "X";

        // test all colors
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < colors.length; i++) {
            sb.append(colors[i].colorString(s1));
        }

        // test color combo
        sb.append(AnsiColors.colorString(
            Arrays.asList(AnsiColors.ON_RED, AnsiColors.BBLUE),
            s1+s1+s1
        ));

        System.out.println(sb.toString());
    }
}
