package com.crystalwink.uturn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PalettesTest {

    private static double distance(int a, int b) {
        int red = ((a >> 16) & 0xFF) - ((b >> 16) & 0xFF);
        int green = ((a >> 8) & 0xFF) - ((b >> 8) & 0xFF);
        int blue = (a & 0xFF) - (b & 0xFF);
        return Math.sqrt(red * red + green * green + blue * blue);
    }

    @Test
    public void everyThemeCoversTheOutsideEdgeAndAllLevelColours() {
        for (int theme = 0; theme < Palettes.COUNT; theme++) {
            assertTrue(Palettes.edges(theme).length >= LevelRules.MAX_COLOURS + 1);
        }
    }

    @Test
    public void edgeColoursAreEasyToTellApart() {
        for (int theme = 0; theme < Palettes.COUNT; theme++) {
            int[] edges = Palettes.edges(theme);
            for (int first = 0; first < edges.length; first++) {
                for (int second = first + 1; second < edges.length; second++) {
                    assertTrue("theme " + theme + " colours " + first + "," + second,
                            distance(edges[first], edges[second]) > 60);
                }
            }
        }
    }

    @Test
    public void softThemeIsCalmerThanVivid() {
        // average saturation proxy: distance of each interior colour from grey
        double soft = 0;
        double vivid = 0;
        for (int index = 1; index <= 5; index++) {
            soft += spread(Palettes.edges(Palettes.SOFT)[index]);
            vivid += spread(Palettes.edges(Palettes.VIVID)[index]);
        }
        assertTrue(soft < vivid);
    }

    private static int spread(int colour) {
        int red = (colour >> 16) & 0xFF;
        int green = (colour >> 8) & 0xFF;
        int blue = colour & 0xFF;
        return Math.max(red, Math.max(green, blue)) - Math.min(red, Math.min(green, blue));
    }

    @Test
    public void unknownStoredThemeFallsBackToSoft() {
        assertEquals(Palettes.SOFT, Palettes.sanitise(7));
        assertEquals(Palettes.SOFT, Palettes.sanitise(-1));
        assertEquals(Palettes.VIVID, Palettes.sanitise(Palettes.VIVID));
    }
}
