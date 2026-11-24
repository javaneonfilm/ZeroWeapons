package dev.cuda.zeroweapons.weapons.weapons.swords;

import dev.cuda.zeroweapons.ZeroWeapons;
import dev.cuda.zeroweapons.weapons.WeaponType;
import dev.cuda.zeroweapons.weapons.weapons.CustomWeapon;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * The Bolt Sword's idea in black and purple: some hits call down a spiral strike for extra
 * damage. The damage lands on the hit; the spiral is drawn over the next few ticks.
 */
public class VortexSword extends CustomWeapon {

    private static final Particle.DustOptions BLACK = new Particle.DustOptions(Color.fromRGB(0x140014), 1.4f);
    private static final Particle.DustOptions PURPLE = new Particle.DustOptions(Color.fromRGB(0xA020F0), 1.3f);

    public VortexSword(ZeroWeapons plugin) {
        super(plugin, WeaponType.VORTEX_SWORD);
    }

    @Override
    public void onHit(Player player, Entity victim, EntityDamageByEntityEvent event) {
        if (!(victim instanceof LivingEntity living)) return;
        if (!this.roll(this.cfg.getInt("strike_chance_percent", 15))) return;

        double damage = this.cfg.getDouble("strike_damage", 2.0);
        if (this.cfg.getBoolean("ignore_armour", true)) {
            trueDamage(living, damage, player);
        } else {
            dealAbilityDamage(living, damage, player);
        }
        living.getWorld().playSound(living.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.9f, 0.6f);
        living.getWorld().playSound(living.getLocation(), Sound.ENTITY_WITHER_SHOOT, 0.5f, 0.7f);
        dev.cuda.zeroweapons.fx.WeaponFx.vortex(living);
    }
}
