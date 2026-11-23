package dev.cuda.zeroweapons.weapons.managers;

import dev.cuda.zeroweapons.ZeroWeapons;
import dev.cuda.zeroweapons.weapons.WeaponType;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Small bits of fight state the newer weapons share.
 *
 * <ul>
 *   <li><b>Silence</b> - the Nullblade's lock. A silenced player's weapons do nothing special:
 *       no shift + right click, no on-hit procs, no bow tricks. The hits themselves still land.</li>
 *   <li><b>Combos</b> - hits in a row on the same player with the same weapon. A miss (a swing
 *       that hit nobody), a new target, a different weapon or a long pause starts it over.</li>
 * </ul>
 *
 * <p>Misses: the client sends the attack and then the swing, so a swing with no hit on the same
 * tick is a swing at air. The check runs a tick late so packet order can't fool it.
 *
 * <p>Main thread only, so plain static maps.
 */
public final class WeaponStates implements Listener {

    private record Combo(UUID target, WeaponType weapon, int count, long lastHit, int lastHitTick) {}

    private static final Map<UUID, Long> SILENCED = new HashMap<>();
    private static final Map<UUID, Combo> COMBOS = new HashMap<>();

    private final ZeroWeapons plugin;

    public WeaponStates(ZeroWeapons plugin) {
        this.plugin = plugin;
    }

    // ── Silence ─────────────────────────────────────────────────────────────

    public static void silence(Player player, int ticks) {
        SILENCED.merge(player.getUniqueId(), System.currentTimeMillis() + ticks * 50L, Math::max);
    }

    public static boolean silenced(Player player) {
        Long until = SILENCED.get(player.getUniqueId());
        if (until == null) return false;
        if (System.currentTimeMillis() >= until) {
            SILENCED.remove(player.getUniqueId());
            return false;
        }
        return true;
    }

    public static double silenceLeft(Player player) {
        Long until = SILENCED.get(player.getUniqueId());
        return until == null ? 0.0 : Math.max(0.0, (until - System.currentTimeMillis()) / 1000.0);
    }

    // ── Combos ──────────────────────────────────────────────────────────────

    /**
     * Counts a hit and returns how many in a row that makes (1 for a fresh start).
     * {@code windowSeconds} is how long a pause can be before it starts over.
     */
    public static int hit(Player attacker, Entity target, WeaponType weapon, double windowSeconds) {
        long now = System.currentTimeMillis();
        Combo old = COMBOS.get(attacker.getUniqueId());
        int count = 1;
        if (old != null && old.target().equals(target.getUniqueId()) && old.weapon() == weapon
            && now - old.lastHit() <= (long) (windowSeconds * 1000)) {
            count = old.count() + 1;
        }
        COMBOS.put(attacker.getUniqueId(), new Combo(target.getUniqueId(), weapon, count, now, Bukkit.getCurrentTick()));
        return count;
    }

    /** Hits in a row right now with {@code weapon}, 0 if none or the window ran out. */
    public static int combo(Player attacker, WeaponType weapon, double windowSeconds) {
        Combo c = COMBOS.get(attacker.getUniqueId());
        if (c == null || c.weapon() != weapon) return 0;
        if (System.currentTimeMillis() - c.lastHit() > (long) (windowSeconds * 1000)) return 0;
        return c.count();
    }

    public static void resetCombo(Player attacker) {
        COMBOS.remove(attacker.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSwing(io.papermc.paper.event.player.PlayerArmSwingEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        if (!COMBOS.containsKey(player.getUniqueId())) return;
        int swingTick = Bukkit.getCurrentTick();
        Bukkit.getScheduler().runTask(this.plugin, () -> {
            Combo c = COMBOS.get(player.getUniqueId());
            if (c != null && c.lastHitTick() < swingTick) COMBOS.remove(player.getUniqueId());
        });
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        UUID id = event.getEntity().getUniqueId();
        SILENCED.remove(id);
        COMBOS.remove(id);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        SILENCED.remove(id);
        COMBOS.remove(id);
    }
}
