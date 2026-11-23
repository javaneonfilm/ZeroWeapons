package dev.cuda.zeroweapons.weapons.weapons.swords;

import dev.cuda.zeroweapons.ZeroWeapons;
import dev.cuda.zeroweapons.core.Palette;
import dev.cuda.zeroweapons.core.Msg;
import dev.cuda.zeroweapons.weapons.WeaponType;
import dev.cuda.zeroweapons.weapons.managers.WeaponStates;
import dev.cuda.zeroweapons.weapons.weapons.CustomWeapon;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Three hits in a row on the same player and their custom weapons go quiet for 2 seconds:
 * no shift + right click, no on-hit procs, no bow tricks. A miss or a new target starts the
 * count over. Has a cooldown so it can't be chained into a permanent lock.
 */
public class Nullblade extends CustomWeapon {

    private static final Particle.DustOptions GREY = new Particle.DustOptions(Color.fromRGB(0xC8CDD5), 1.2f);

    public Nullblade(ZeroWeapons plugin) {
        super(plugin, WeaponType.NULLBLADE);
    }

    @Override
    public void onHit(Player player, Entity victim, EntityDamageByEntityEvent event) {
        if (!(victim instanceof Player target)) return;
        if (this.plugin.getCooldownManager().isOnCooldown(player, this.type)) return;

        int needed = Math.max(1, this.cfg.getInt("hits_needed", 3));
        int count = WeaponStates.hit(player, target, this.type, this.cfg.getDouble("combo_window_seconds", 2.5));
        if (count < needed) return;
        WeaponStates.resetCombo(player);

        int ticks = (int) Math.round(this.cfg.getDouble("lock_seconds", 2.0) * 20);
        WeaponStates.silence(target, ticks);
        this.showLock(target, ticks);

        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.6f);
        target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0, 1.0, 0), 30, 0.35, 0.6, 0.35, 0.0, GREY);
        target.getWorld().spawnParticle(Particle.SMOKE, target.getLocation().add(0, 1.2, 0), 12, 0.3, 0.4, 0.3, 0.01);
        String secs = String.format("%.0f", ticks / 20.0);
        Msg.actionBar(player, Palette.b(Palette.NULL, "NULLED")
            + " <color:" + Palette.INK + ">" + target.getName() + " " + secs + "s</color>");
        Msg.actionBar(target, Palette.b(Palette.NULL, "NULLED")
            + " <color:" + Palette.INK + ">" + Palette.sc("no abilities") + " " + secs + "s</color>");
        this.applyCooldown(player);
    }

    /**
     * Puts the cooldown sweep over every custom weapon they carry for the lock, so they can see
     * it. A weapon already cooling down for longer keeps its own sweep.
     */
    private void showLock(Player target, int ticks) {
        var weapons = this.plugin.getWeaponManager();
        for (var item : target.getInventory().getContents()) {
            CustomWeapon weapon = weapons.getWeaponFromItem(item);
            if (weapon == null) continue;
            NamespacedKey key = weapon.cooldownKey();
            if (target.getCooldown(key) < ticks) target.setCooldown(key, ticks);
        }
    }
}
