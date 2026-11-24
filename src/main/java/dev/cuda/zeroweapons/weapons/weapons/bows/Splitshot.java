package dev.cuda.zeroweapons.weapons.weapons.bows;

import dev.cuda.zeroweapons.ZeroWeapons;
import dev.cuda.zeroweapons.weapons.WeaponType;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * A fully drawn arrow splits into three a few ticks after it leaves the bow. All three hit
 * harder than a normal arrow. Only the middle one can be picked back up.
 */
public class Splitshot extends BowWeapon {

    private static final Particle.DustOptions PINK = new Particle.DustOptions(Color.fromRGB(0xFF3DA8), 1.1f);

    public Splitshot(ZeroWeapons plugin) {
        super(plugin, WeaponType.SPLITSHOT);
    }

    @Override
    protected void onShoot(Player shooter, AbstractArrow arrow, float force) {
        double multiplier = this.cfg.getDouble("damage_multiplier", 1.25);
        if (force < this.cfg.getDouble("min_draw", 0.95)) {
            return;
        }
        dev.cuda.zeroweapons.fx.WeaponFx.splitTrail(arrow, false);
        int delay = Math.max(1, this.cfg.getInt("split_after_ticks", 3));
        this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> this.split(shooter, arrow, multiplier), delay);
    }

    private void split(Player shooter, AbstractArrow arrow, double multiplier) {
        if (!arrow.isValid() || arrow.isInBlock() || arrow.isOnGround()) return;
        Location at = arrow.getLocation();
        Vector velocity = arrow.getVelocity();
        double spread = Math.toRadians(this.cfg.getDouble("spread_degrees", 7.0));
        double damage = arrow.getDamage() * multiplier;

        java.util.List<Vector> dirs = new java.util.ArrayList<>();
        for (int i = -1; i <= 1; i++) {
            Vector dir = velocity.clone().rotateAroundY(spread * i);
            dirs.add(dir);
            boolean middle = i == 0;
            Arrow spawned = at.getWorld().spawn(at, Arrow.class, child -> {
                child.setShooter(shooter);
                child.setVelocity(dir);
                child.setDamage(damage);
                child.setCritical(arrow.isCritical());
                child.setFireTicks(arrow.getFireTicks());
                child.setPierceLevel(arrow.getPierceLevel());
                child.setPickupStatus(middle ? arrow.getPickupStatus() : AbstractArrow.PickupStatus.DISALLOWED);
                // Tagged as this bow's, but split children never split again: only onShoot splits.
                this.tag(child);
            });
            dev.cuda.zeroweapons.fx.WeaponFx.splitTrail(spawned, true);
        }
        arrow.remove();
        if (this.plugin.getConfig().getBoolean("debug")) this.plugin.getLogger().info("DBG split into 3");
        dev.cuda.zeroweapons.fx.WeaponFx.splitFlash(at, velocity, dirs);
        at.getWorld().playSound(at, Sound.ENTITY_ARROW_SHOOT, 0.8f, 1.6f);
    }
}
