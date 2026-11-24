package dev.cuda.zeroweapons.weapons.weapons.bows;

import dev.cuda.zeroweapons.ZeroWeapons;
import dev.cuda.zeroweapons.weapons.WeaponType;
import dev.cuda.zeroweapons.weapons.utils.RegionUtils;
import dev.cuda.zeroweapons.weapons.utils.TargetUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Arrows hit harder and bend toward the closest player in front of them.
 *
 * <p>Each tick the arrow looks for the nearest player within range and ahead of it, locks on,
 * and turns a little toward their chest. It keeps its launch speed so the curve doesn't make
 * it drop short. Weak draws fly straight.
 */
public class CurveShot extends BowWeapon {

    private static final Particle.DustOptions GREEN = new Particle.DustOptions(Color.fromRGB(0xC4FF7A), 0.8f);

    public CurveShot(ZeroWeapons plugin) {
        super(plugin, WeaponType.CURVE_SHOT);
    }

    @Override
    protected void onShoot(Player shooter, AbstractArrow arrow, float force) {
        arrow.setDamage(arrow.getDamage() * this.cfg.getDouble("damage_multiplier", 1.2));
        if (force < this.cfg.getDouble("min_draw", 0.6)) return;

        double range = this.cfg.getDouble("homing_range", 20.0);
        double turn = this.cfg.getDouble("turn_strength", 0.35);
        int maxTicks = this.cfg.getInt("homing_ticks", 60);
        double cone = Math.cos(Math.toRadians(this.cfg.getDouble("cone_degrees", 70.0)));
        double speed = arrow.getVelocity().length();
        String spawn = this.plugin.getConfig().getString("spawn-region", "spawn");

        new BukkitRunnable() {
            int age = 0;
            Player target = null;

            @Override
            public void run() {
                this.age++;
                if (this.age > maxTicks || !arrow.isValid() || arrow.isInBlock() || arrow.isOnGround()) {
                    this.cancel();
                    return;
                }
                Location at = arrow.getLocation();
                Vector velocity = arrow.getVelocity();
                if (this.target != null && (!this.target.isValid() || this.target.isDead()
                    || !this.target.getWorld().equals(at.getWorld())
                    || this.target.getLocation().distanceSquared(at) > range * range * 2.25)) {
                    this.target = null;
                }
                if (this.target == null) {
                    this.target = this.pick(at, velocity);
                    if (this.target != null) dev.cuda.zeroweapons.fx.WeaponFx.curveLock(this.target, shooter);
                    if (this.target != null && CurveShot.this.plugin.getConfig().getBoolean("debug")) CurveShot.this.plugin.getLogger().info("DBG curve lock " + this.target.getName() + " age=" + this.age);
                }
                dev.cuda.zeroweapons.fx.WeaponFx.curveTick(arrow, this.target, this.age);
                if (this.target == null) return;

                Vector aim = this.target.getLocation().add(0, this.target.getHeight() * 0.6, 0).toVector()
                    .subtract(at.toVector());
                if (aim.lengthSquared() < 1.0E-4) return;
                Vector next = velocity.clone().normalize().multiply(1.0 - turn).add(aim.normalize().multiply(turn));
                arrow.setVelocity(next.normalize().multiply(Math.max(speed, velocity.length())));
            }

            private Player pick(Location at, Vector velocity) {
                Vector forward = velocity.clone();
                if (forward.lengthSquared() < 1.0E-4) return null;
                forward.normalize();
                Player best = null;
                double bestDist = range * range;
                for (Player p : at.getWorld().getNearbyPlayers(at, range)) {
                    if (p.equals(arrow.getShooter())) continue;
                    if (!TargetUtils.targetable(p) || RegionUtils.isInRegion(p, spawn)) continue;
                    Vector to = p.getLocation().add(0, p.getHeight() * 0.6, 0).toVector().subtract(at.toVector());
                    double dist = to.lengthSquared();
                    if (dist >= bestDist || dist < 1.0E-4) continue;
                    if (to.clone().normalize().dot(forward) < cone) continue;
                    best = p;
                    bestDist = dist;
                }
                return best;
            }
        }.runTaskTimer(this.plugin, 1L, 1L);
    }

    @Override
    protected void onArrowHit(Player shooter, AbstractArrow arrow, LivingEntity victim, EntityDamageByEntityEvent event) {
        dev.cuda.zeroweapons.fx.WeaponFx.curveHit(victim);
    }
}
