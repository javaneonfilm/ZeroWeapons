package dev.cuda.zeroweapons.weapons.weapons;

import dev.cuda.zeroweapons.core.Palette;
import dev.cuda.zeroweapons.weapons.WeaponType;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Builds a weapon's tooltip, in the old ZeroWeapons-weapons style: small-caps labels, the
 * important bits in normal bold.
 *
 * <pre>
 *   ᴇɴᴄʜᴀɴᴛꜱ
 *    • ꜱʜᴀʀᴘɴᴇꜱꜱ V                    grey small caps, level in bold
 *    • ꜰɪʀᴇ ᴀꜱᴘᴇᴄᴛ II
 *
 *   ᴀʙɪʟɪᴛʏ › Dash                     ability name bold, in the weapon's gradient
 *    ꜱʜɪꜰᴛ + ʀɪɢʜᴛ ᴄʟɪᴄᴋ               how it goes off, weapon colour
 *    ᴅᴀꜱʜ ᴡʜᴇʀᴇ ʏᴏᴜ'ʀᴇ ʟᴏᴏᴋɪɴɢ           what it does; {numbers} in bold white
 *
 *   ᴄᴏᴏʟᴅᴏᴡɴ › 30s                     bold
 * </pre>
 * {@code ability}, {@code use} and {@code does} are written in plain lowercase in weapons.yml;
 * the styling is put on here.
 */
public final class WeaponLore {

    private static final MiniMessage MM = dev.cuda.zeroweapons.core.Msg.MM;
    private static final String ARROW = " <dark_gray>›</dark_gray> ";
    private static final String BULLET = " <dark_gray>•</dark_gray> ";

    private WeaponLore() {}

    public static List<Component> build(ConfigurationSection cfg, WeaponType type, int cooldown,
                                        java.util.Map<String, Integer> enchants) {
        List<String> lines = new ArrayList<>();

        // Enchants. Unbreaking and mending do nothing on an unbreakable item.
        boolean unbreakable = cfg.getBoolean("unbreakable", true);
        List<String> shown = new ArrayList<>();
        for (var e : enchants.entrySet()) {
            String key = e.getKey();
            if (unbreakable && (key.equalsIgnoreCase("unbreaking") || key.equalsIgnoreCase("mending"))) continue;
            shown.add(BULLET + Palette.l(Palette.INK, prettify(key)) + " <bold><white>" + roman(e.getValue()) + "</white></bold>");
        }
        if (!shown.isEmpty()) {
            lines.add(Palette.l(Palette.DUSK, "enchants"));
            lines.addAll(shown);
            lines.add("");
        }

        String ability = cfg.getString("ability", "");
        String use = cfg.getString("use", "");
        String does = cfg.getString("does", "");
        if (!ability.isEmpty()) {
            lines.add(Palette.l(Palette.DUSK, "ability") + ARROW + "<bold>" + type.gradient(title(ability)) + "</bold>");
        }
        if (!use.isEmpty()) lines.add(" " + "<color:" + type.colour() + ">" + Palette.sc(use) + "</color>");
        if (!does.isEmpty()) {
            for (String line : does.split("\n")) lines.add(" " + describe(line.trim()));
        }

        if (cooldown > 0) {
            lines.add("");
            lines.add(Palette.l(Palette.DUSK, "cooldown") + ARROW + "<bold><white>" + cooldown + "s</white></bold>");
        }

        while (!lines.isEmpty() && lines.get(lines.size() - 1).isEmpty()) {
            lines.remove(lines.size() - 1);
        }

        List<Component> lore = new ArrayList<>(lines.size());
        for (String line : lines) {
            lore.add(line.isEmpty()
                ? Component.empty()
                : MM.deserialize(line).decoration(TextDecoration.ITALIC, false));
        }
        return lore;
    }

    /** Grey small caps, with anything in {curly brackets} in bold white so the numbers stand out. */
    private static String describe(String line) {
        StringBuilder out = new StringBuilder();
        int at = 0;
        while (at < line.length()) {
            int open = line.indexOf('{', at);
            int close = open < 0 ? -1 : line.indexOf('}', open);
            if (open < 0 || close < 0) {
                out.append(Palette.l(Palette.INK, line.substring(at)));
                break;
            }
            if (open > at) out.append(Palette.l(Palette.INK, line.substring(at, open)));
            out.append("<bold><white>").append(line, open + 1, close).append("</white></bold>");
            at = close + 1;
        }
        return out.toString();
    }

    /** "venom dash" -> "Venom Dash". */
    private static String title(String text) {
        StringBuilder out = new StringBuilder();
        for (String word : text.split(" ")) {
            if (word.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.toString();
    }

    /** "fire_aspect" -> "Fire Aspect". Small caps is applied by the caller. */
    private static String prettify(String enchantKey) {
        String[] words = enchantKey.replace('_', ' ').split(" ");
        StringBuilder out = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(' ');
        }
        return out.toString().trim();
    }

    private static String roman(int value) {
        return switch (value) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> String.valueOf(value);
        };
    }
}
