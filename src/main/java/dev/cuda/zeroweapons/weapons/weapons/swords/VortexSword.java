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
        this.spiral(living);
    }

    /** Two twisting strands, black and purple, coming down onto them. */
    private void spiral(LivingEntity target) {
        World world = target.getWorld();
        world.playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.9f, 0.6f);
        world.playSound(target.getLocation(), Sound.ENTITY_WITHER_SHOOT, 0.5f, 0.7f);
        new BukkitRunnable() {
            int step = 0;
            final int steps = 5;
            final double top = 4.0;

            @Override
            public void run() {
                if (this.step >= this.steps || !target.isValid()) {
                    Location feet = target.getLocation();
                    world.spawnParticle(Particle.SQUID_INK, feet.clone().add(0, 0.2, 0), 16, 0.4, 0.05, 0.4, 0.06);
                    world.spawnParticle(Particle.REVERSE_PORTAL, feet.clone().add(0, 1.0, 0), 30, 0.3, 0.6, 0.3, 0.08);
                    world.spawnParticle(Particle.DUST, feet.clone().add(0, 0.1, 0), 24, 0.6, 0.02, 0.6, 0.0, PURPLE);
                    this.cancel();
                    return;
                }
                Location base = target.getLocation();
                // Each tick draws one slice of the spiral, top to bottom.
                double from = this.top * (1.0 - (double) this.step / this.steps);
                double to = this.top * (1.0 - (double) (this.step + 1) / this.steps);
                for (double y = from; y >= to; y -= 0.12) {
                    double angle = y * 3.2;
                    double radius = 0.25 + 0.15 * y;
                    for (int strand = 0; strand < 2; strand++) {
                        double a = angle + strand * Math.PI;
                        world.spawnParticle(Particle.DUST,
                            base.getX() + Math.cos(a) * radius, base.getY() + y, base.getZ() + Math.sin(a) * radius,
                            1, 0, 0, 0, 0, strand == 0 ? BLACK : PURPLE);
                    }
                }
                this.step++;
            }
        }.runTaskTimer(this.plugin, 0L, 1L);
    }
}
