package dev.cuda.zeroweapons.weapons.utils;

import dev.cuda.zeroweapons.ZeroWeapons;
import java.util.List;
import org.bukkit.entity.Player;

/**
 * Where abilities are off.
 *
 * <p>Two ways, both in config.yml: whole worlds ({@code disabled-worlds}, e.g. a lobby), and a
 * WorldGuard region by name ({@code safe-region}, e.g. spawn) when WorldGuard is installed.
 * The hits themselves are left to whatever PvP rules the server already has.
 */
public final class RegionUtils {

    private RegionUtils() {}

    /** True if {@code player} stands where weapons should do nothing. The id argument is ignored; config decides. */
    public static boolean isInRegion(Player player, String ignored) {
        ZeroWeapons plugin = ZeroWeapons.get();
        if (plugin == null) return false;
        List<String> worlds = plugin.getConfig().getStringList("disabled-worlds");
        for (String world : worlds) {
            if (world.equalsIgnoreCase(player.getWorld().getName())) return true;
        }
        String region = plugin.getConfig().getString("safe-region", "");
        if (region == null || region.isEmpty()) return false;
        if (plugin.getServer().getPluginManager().getPlugin("WorldGuard") == null) return false;
        try {
            return WorldGuardHook.inRegion(player, region);
        } catch (Throwable t) {
            return false;
        }
    }
}
