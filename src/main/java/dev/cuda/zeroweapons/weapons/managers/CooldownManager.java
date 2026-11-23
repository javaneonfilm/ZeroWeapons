package dev.cuda.zeroweapons.weapons.managers;

import dev.cuda.zeroweapons.weapons.WeaponType;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;

/** Ability cooldowns per player, per weapon. */
public class CooldownManager {

    private final Map<UUID, Map<WeaponType, Long>> cooldowns = new HashMap<>();

    public boolean isOnCooldown(Player player, WeaponType type) {
        Map<WeaponType, Long> map = this.cooldowns.get(player.getUniqueId());
        if (map == null) return false;
        Long expiry = map.get(type);
        if (expiry == null) return false;
        if (System.currentTimeMillis() >= expiry) {
            map.remove(type);
            return false;
        }
        return true;
    }

    public double getRemainingSeconds(Player player, WeaponType type) {
        Map<WeaponType, Long> map = this.cooldowns.get(player.getUniqueId());
        if (map == null) return 0.0;
        Long expiry = map.get(type);
        return expiry == null ? 0.0 : Math.max(0.0, (expiry - System.currentTimeMillis()) / 1000.0);
    }

    public void setCooldown(Player player, WeaponType type, int seconds) {
        this.cooldowns.computeIfAbsent(player.getUniqueId(), k -> new EnumMap<>(WeaponType.class))
            .put(type, System.currentTimeMillis() + seconds * 1000L);
    }

    public void removePlayer(UUID uuid) {
        this.cooldowns.remove(uuid);
    }
}
