package dev.cuda.zeroweapons.weapons.weapons.axes;

import dev.cuda.zeroweapons.ZeroWeapons;
import dev.cuda.zeroweapons.weapons.WeaponType;
import dev.cuda.zeroweapons.weapons.weapons.CustomWeapon;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

/**
 * Every third hit on the same player is a heavy one: big knockback and a short slow.
 * Misses don't reset the count; a new target or a long pause does.
 */
public class Dreadcleaver extends CustomWeapon {

    private record Count(UUID target, int hits, long last) {}

    private final Map<UUID, Count> counts = new HashMap<>();

    public Dreadcleaver(ZeroWeapons plugin) {
        super(plugin, WeaponType.DREADCLEAVER);
    }

    @Override
    public void onHit(Player player, Entity victim, EntityDamageByEntityEvent event) {
        if (!(victim instanceof LivingEntity living)) return;
        long now = System.currentTimeMillis();
        long window = (long) (this.cfg.getDouble("reset_after_seconds", 5.0) * 1000);
        Count old = this.counts.get(player.getUniqueId());
        int hits = old != null && old.target().equals(victim.getUniqueId()) && now - old.last() <= window
            ? old.hits() + 1 : 1;
        int every = Math.max(1, this.cfg.getInt("every_hits", 3));
        if (hits < every) {
            this.counts.put(player.getUniqueId(), new Count(victim.getUniqueId(), hits, now));
            return;
        }
        this.counts.remove(player.getUniqueId());

        double power = this.cfg.getDouble("knockback", 1.3);
        double lift = this.cfg.getDouble("knockback_up", 0.45);
        Vector push = victim.getLocation().toVector().subtract(player.getLocation().toVector()).setY(0);
        if (push.lengthSquared() < 1.0E-4) push = player.getLocation().getDirection().setY(0);
        if (push.lengthSquared() < 1.0E-4) push = new Vector(0, 0, 1);
        Vector velocity = push.normalize().multiply(power).setY(lift);
        // Next tick, after vanilla has put its own knockback on, so ours is the one that sticks.
        this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
            if (living.isValid() && !living.isDead()) living.setVelocity(velocity);
        });

        int slowTicks = (int) Math.round(this.cfg.getDouble("slow_seconds", 1.5) * 20);
        int level = Math.max(1, this.cfg.getInt("slow_level", 2));
        living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slowTicks, level - 1, false, true, true));

        living.getWorld().playSound(living.getLocation(), Sound.ITEM_MACE_SMASH_GROUND_HEAVY, 1.0f, 0.8f);
        living.getWorld().playSound(living.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 0.6f);
        living.getWorld().spawnParticle(Particle.EXPLOSION, living.getLocation().add(0, 1.0, 0), 1);
        living.getWorld().spawnParticle(Particle.CRIT, living.getLocation().add(0, 1.0, 0), 25, 0.4, 0.5, 0.4, 0.3);
    }

    @Override
    public void forgetPlayer(UUID playerId) {
        this.counts.remove(playerId);
    }
}
