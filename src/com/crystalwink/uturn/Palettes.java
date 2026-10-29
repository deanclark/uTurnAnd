package com.crystalwink.uturn;

/**
 * Colour themes. Each theme gives the colour of every edge index (0 = outside edge, 1.. = interior colours) and the
 * colour of the board background. Colours are ARGB ints.
 */
public final class Palettes {
    public static final int SOFT = 0;
    public static final int VIVID = 1;
    /** Soft colours plus a distinct symbol on every coloured edge, so colours never have to be told apart by hue alone. */
    public static final int PATTERNS = 2;
    public static final int COUNT = 3;

    private static final int[] SOFT_EDGES = {
        0xFF5F6B73,   // outside: slate
        0xFFE57373,   // coral
        0xFF6FA8DC,   // sky blue
        0xFF81C784,   // sage green
        0xFFFFD54F,   // honey
        0xFF4DB6AC,   // teal
        0xFFB39DDB,   // lavender
        0xFFFFB38A,   // peach
        0xFFC5E1A5    // light lime (spare)
    };

    private static final int[] VIVID_EDGES = {
        0xFF444444,   // outside: dark grey
        0xFFFF0000,   // red
        0xFF0000FF,   // blue
        0xFF00FF00,   // green
        0xFFFFFF00,   // yellow
        0xFF00FFFF,   // cyan
        0xFFB39DDB,   // lavender
        0xFFFFB38A,   // peach
        0xFFC5E1A5    // light lime (spare)
    };

    private Palettes() {
    }

    public static int[] edges(int theme) {
        return (theme == VIVID ? VIVID_EDGES : SOFT_EDGES).clone();   // PATTERNS shares the soft colours
    }

    public static int background(int theme) {
        return background(theme, false);
    }

    /** Board background; the dark variants keep the edge colours readable (Vivid goes to black, Soft to a warm charcoal). */
    public static int background(int theme, boolean dark) {
        if (dark) {
            return theme == VIVID ? 0xFF000000 : 0xFF16191C;
        }
        return theme == VIVID ? 0xFFFFFFFF : 0xFFF6F3EE;
    }

    /** Colour of the logo, title and menu button on the background above. */
    public static int logo(boolean dark) {
        return dark ? 0xFFE8E4DC : 0xFF3A4750;
    }

    /** True when edges carry a symbol (indexed by edge colour; the outside edge has none). */
    public static boolean hasSymbols(int theme) {
        return theme == PATTERNS;
    }

    /** Outline colour drawn between segments. */
    public static int outline(int theme) {
        return theme == VIVID ? 0xFF000000 : 0xFF4A4A4A;
    }

    /** Valid theme number from a stored value. */
    public static int sanitise(int theme) {
        return theme == VIVID || theme == PATTERNS ? theme : SOFT;
    }
}
