package dev.cuda.zeroweapons.weapons.weapons.axes;

import dev.cuda.zeroweapons.ZeroWeapons;
import dev.cuda.zeroweapons.core.Palette;
import dev.cuda.zeroweapons.core.Msg;
import dev.cuda.zeroweapons.weapons.WeaponType;
import dev.cuda.zeroweapons.weapons.weapons.CustomWeapon;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;

/**
 * A hit stops them eating golden apples for 3 seconds. It's the vanilla item cooldown, so they
 * see the sweep over their gaps and the client won't even start eating. The consume event is
 * blocked too in case anything slips through. Cooldown after each lock.
 */
public class Voidchopper extends CustomWeapon implements Listener {

    private static final Particle.DustOptions VOID = new Particle.DustOptions(Color.fromRGB(0x1B2A80), 1.3f);

    private final Map<UUID, Long> locked = new HashMap<>();

    public Voidchopper(ZeroWeapons plugin) {
        super(plugin, WeaponType.VOIDCHOPPER);
    }

    @Override
    public void onHit(Player player, Entity victim, EntityDamageByEntityEvent event) {
        if (!(victim instanceof Player target)) return;
        if (this.plugin.getCooldownManager().isOnCooldown(player, this.type)) return;

        int ticks = (int) Math.round(this.cfg.getDouble("lock_seconds", 3.0) * 20);
        this.locked.merge(target.getUniqueId(), System.currentTimeMillis() + ticks * 50L, Math::max);
        for (Material gap : new Material[] {Material.GOLDEN_APPLE, Material.ENCHANTED_GOLDEN_APPLE}) {
            if (target.getCooldown(gap) < ticks) target.setCooldown(gap, ticks);
        }
        // Stops one that's already being eaten.
        if (target.getActiveItem().getType() == Material.GOLDEN_APPLE
            || target.getActiveItem().getType() == Material.ENCHANTED_GOLDEN_APPLE) {
            target.clearActiveItem();
        }

        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 1.0f, 0.6f);
        target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0, 1.0, 0), 24, 0.35, 0.6, 0.35, 0.0, VOID);
        target.getWorld().spawnParticle(Particle.PORTAL, target.getLocation().add(0, 1.0, 0), 30, 0.3, 0.5, 0.3, 0.4);
        String secs = String.format("%.0f", ticks / 20.0);
        Msg.actionBar(player, Palette.b(Palette.VOID, "NO GAPS")
            + " <color:" + Palette.INK + ">" + target.getName() + " " + secs + "s</color>");
        Msg.actionBar(target, Palette.b(Palette.VOID, "NO GAPS")
            + " <color:" + Palette.INK + ">" + secs + "s</color>");
        this.applyCooldown(player);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEat(PlayerItemConsumeEvent event) {
        Material type = event.getItem().getType();
        if (type != Material.GOLDEN_APPLE && type != Material.ENCHANTED_GOLDEN_APPLE) return;
        Long until = this.locked.get(event.getPlayer().getUniqueId());
        if (until == null) return;
        if (System.currentTimeMillis() >= until) {
            this.locked.remove(event.getPlayer().getUniqueId());
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        this.locked.remove(event.getEntity().getUniqueId());
    }

    @Override
    public void forgetPlayer(UUID playerId) {
        this.locked.remove(playerId);
    }
}
