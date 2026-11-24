package dev.cuda.zeroweapons.weapons.weapons.axes;

import dev.cuda.zeroweapons.ZeroWeapons;
import dev.cuda.zeroweapons.core.Palette;
import dev.cuda.zeroweapons.core.Msg;
import dev.cuda.zeroweapons.weapons.WeaponType;
import dev.cuda.zeroweapons.weapons.weapons.CustomWeapon;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Every second crit takes hearts off them and gives them to you. The stolen part goes through
 * armour, and you only get back what they actually lost.
 */
public class LifestealAxe extends CustomWeapon {

    private final Map<UUID, Integer> crits = new HashMap<>();

    public LifestealAxe(ZeroWeapons plugin) {
        super(plugin, WeaponType.LIFESTEAL_AXE);
    }

    @Override
    public void onHit(Player player, Entity victim, EntityDamageByEntityEvent event) {
        if (!(victim instanceof LivingEntity living) || !event.isCritical()) return;
        int needed = Math.max(1, this.cfg.getInt("crits_needed", 2));
        int count = this.crits.merge(player.getUniqueId(), 1, Integer::sum);
        if (count < needed) return;
        this.crits.remove(player.getUniqueId());

        double steal = this.cfg.getDouble("steal_hearts", 1.5) * 2.0;
        double before = living.getHealth();
        trueDamage(living, steal, player);
        double taken = Math.max(0.0, before - (living.isDead() ? 0.0 : living.getHealth()));
        taken = Math.min(taken, steal);
        if (taken <= 0.0) return;

        var max = player.getAttribute(Attribute.MAX_HEALTH);
        double cap = max != null ? max.getValue() : 20.0;
        player.setHealth(Math.min(cap, player.getHealth() + taken));

        living.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, living.getLocation().add(0, 1.2, 0), 6, 0.3, 0.3, 0.3, 0.1);
        player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1.9, 0), 3, 0.3, 0.2, 0.3, 0.0);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITCH_DRINK, 0.7f, 1.3f);
        Msg.actionBar(player, Palette.b(Palette.LEECH, "LIFESTEAL")
            + " <color:" + Palette.INK + ">+" + String.format("%.1f", taken / 2.0) + " " + Palette.sc("hearts") + "</color>");
    }

    @Override
    public void forgetPlayer(UUID playerId) {
        this.crits.remove(playerId);
    }
}
