package dev.cuda.zeroweapons.core;

/**
 * Colours and text styling. Greys carry the text; each weapon has its own two-colour gradient.
 *
 * <p>The small-caps table is declared first: static fields initialise in order.
 */
public final class Palette {

    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    /** Unicode small capitals. 'x' has no small-cap form, so it stays as-is. */
    private static final String[] SMALL_CAPS = {
        "ᴀ", "ʙ", "ᴄ", "ᴅ", "ᴇ", "ꜰ", "ɢ", "ʜ", "ɪ", "ᴊ", "ᴋ", "ʟ", "ᴍ",
        "ɴ", "ᴏ", "ᴘ", "ǫ", "ʀ", "ꜱ", "ᴛ", "ᴜ", "ᴠ", "ᴡ", "x", "ʏ", "ᴢ"
    };

    public static final String BRAND = "#8C00F6";
    public static final String GLOW = "#FFFFFF";
    public static final String INK = "#AAAAAA";
    public static final String DIM = "#555555";
    public static final String DUSK = "#777777";
    public static final String BAD = "#FF5555";
    public static final String GOOD = "#57D957";

    // Weapon gradients (start, end).
    public static final String DASH = "#D9A6FF";
    public static final String DASH_2 = "#8C00F6";
    public static final String VORTEX_SWORD = "#D07CFF";
    public static final String VORTEX_SWORD_2 = "#3D0066";
    public static final String NULL = "#E3E8EF";
    public static final String NULL_2 = "#6B7280";
    public static final String OVERDRIVE = "#6BFFF2";
    public static final String OVERDRIVE_2 = "#0091FF";
    public static final String DREAD = "#FF6B6B";
    public static final String DREAD_2 = "#7A0010";
    public static final String LEECH = "#FF9EB0";
    public static final String LEECH_2 = "#E0002B";
    public static final String COB = "#FFFFFF";
    public static final String COB_2 = "#9A9A9A";
    public static final String VOID = "#7A9CFF";
    public static final String VOID_2 = "#1B2A80";
    public static final String SPLIT = "#FFB3F0";
    public static final String SPLIT_2 = "#FF3DA8";
    public static final String FLAME = "#FFD36B";
    public static final String FLAME_2 = "#FF3D00";
    public static final String CURVE = "#C4FF7A";
    public static final String CURVE_2 = "#00A86B";

    private Palette() {}

    /**
     * Letters to small capitals. Anything inside {@code <...>} (MiniMessage tags) or
     * {@code {...}} (placeholders) is copied through untouched.
     */
    public static String sc(String text) {
        StringBuilder out = new StringBuilder(text.length());
        char closing = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (closing == 0 && (ch == '<' || ch == '{')) {
                closing = ch == '<' ? '>' : '}';
            }
            if (closing != 0) {
                out.append(ch);
                if (ch == closing) closing = 0;
                continue;
            }
            int index = LOWER.indexOf(Character.toLowerCase(ch));
            out.append(index >= 0 ? SMALL_CAPS[index] : ch);
        }
        return out.toString();
    }

    /** Colour, text as written. For names, ids, numbers. */
    public static String c(String hex, String text) {
        return "<color:" + hex + ">" + text + "</color>";
    }

    /** Colour + small caps. */
    public static String l(String hex, String text) {
        return "<color:" + hex + ">" + sc(text) + "</color>";
    }

    /** Colour + small caps for headers. */
    public static String b(String hex, String text) {
        return "<color:" + hex + ">" + sc(text) + "</color>";
    }
}
