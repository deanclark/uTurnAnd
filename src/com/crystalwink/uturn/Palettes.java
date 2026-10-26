package com.crystalwink.uturn;

/**
 * Colour themes. Each theme gives the colour of every edge index (0 = outside edge, 1.. = interior colours) and the
 * colour of the board background. Colours are ARGB ints.
 */
public final class Palettes {
    public static final int SOFT = 0;
    public static final int VIVID = 1;
    public static final int COUNT = 2;

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
        return (theme == VIVID ? VIVID_EDGES : SOFT_EDGES).clone();
    }

    public static int background(int theme) {
        return theme == VIVID ? 0xFFFFFFFF : 0xFFF6F3EE;
    }

    /** Outline colour drawn between segments. */
    public static int outline(int theme) {
        return theme == VIVID ? 0xFF000000 : 0xFF4A4A4A;
    }

    /** Valid theme number from a stored value. */
    public static int sanitise(int theme) {
        return theme == VIVID ? VIVID : SOFT;
    }
}
