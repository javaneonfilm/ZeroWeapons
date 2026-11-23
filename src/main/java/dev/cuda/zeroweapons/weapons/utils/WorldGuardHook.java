package dev.cuda.zeroweapons.weapons.utils;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import org.bukkit.entity.Player;

/**
 * The only class that touches WorldGuard. Kept apart so the plugin still loads when WorldGuard
 * isn't installed: this class is only loaded once RegionUtils has checked it's there.
 */
final class WorldGuardHook {

    private WorldGuardHook() {}

    static boolean inRegion(Player player, String regionId) {
        var query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
        var regions = query.getApplicableRegions(BukkitAdapter.adapt(player.getLocation()));
        return regions.getRegions().stream().anyMatch(r -> r.getId().equalsIgnoreCase(regionId));
    }
}
