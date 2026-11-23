package dev.cuda.zeroweapons.weapons;

import dev.cuda.zeroweapons.core.Palette;

/**
 * The ZeroWeapons arsenal.
 *
 * Each entry carries its own accent colour so the boss bar, the item lore and the config
 * comment all agree without three copies of the same hex code drifting apart.
 */
public enum WeaponType {

    DASH_SWORD("Dash Sword", Palette.DASH, Palette.DASH_2),
    VORTEX_SWORD("Vortex Sword", Palette.VORTEX_SWORD, Palette.VORTEX_SWORD_2),
    NULLBLADE("Nullblade", Palette.NULL, Palette.NULL_2),
    OVERDRIVE("Overdrive", Palette.OVERDRIVE, Palette.OVERDRIVE_2),
    DREADCLEAVER("Dreadcleaver", Palette.DREAD, Palette.DREAD_2),
    LIFESTEAL_AXE("Lifesteal Axe", Palette.LEECH, Palette.LEECH_2),
    COB_AXE("Cob Axe", Palette.COB, Palette.COB_2),
    VOIDCHOPPER("Voidchopper", Palette.VOID, Palette.VOID_2),
    SPLITSHOT("Splitshot", Palette.SPLIT, Palette.SPLIT_2),
    FLAMING_BOW("Flaming Bow", Palette.FLAME, Palette.FLAME_2),
    CURVE_SHOT("Curve Shot", Palette.CURVE, Palette.CURVE_2);

    private final String displayName;
    private final String colour;
    private final String colour2;

    WeaponType(String displayName, String colour, String colour2) {
        this.displayName = displayName;
        this.colour = colour;
        this.colour2 = colour2;
    }

    /** The far end of the weapon's gradient. */
    public String colour2() {
        return this.colour2;
    }

    /** {@code text} in this weapon's gradient. */
    public String gradient(String text) {
        return "<gradient:" + this.colour + ":" + this.colour2 + ">" + text + "</gradient>";
    }

    public String displayName() {
        return this.displayName;
    }

    public String colour() {
        return this.colour;
    }

    /** Bold small caps, in the weapon's gradient, ready to drop into MiniMessage. */
    public String coloredName() {
        return "<bold>" + this.gradient(Palette.sc(this.displayName)) + "</bold>";
    }

    public String configKey() {
        return this.name().toLowerCase();
    }
}
