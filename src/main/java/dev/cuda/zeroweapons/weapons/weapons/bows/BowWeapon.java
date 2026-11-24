package dev.cuda.zeroweapons.weapons.weapons.bows;

import dev.cuda.zeroweapons.ZeroWeapons;
import dev.cuda.zeroweapons.weapons.WeaponType;
import dev.cuda.zeroweapons.weapons.weapons.CustomWeapon;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.persistence.PersistentDataType;

/**
 * A custom bow. Its tricks run when an arrow leaves it and when that arrow hits someone.
 *
 * <p>Arrows are tagged with the bow they came from (same {@code weapon_type} key as the items),
 * so switching to another bow mid-flight changes nothing. In spawn, or while Nullbladed, the
 * bow shoots a normal arrow.
 */
public abstract class BowWeapon extends CustomWeapon implements Listener {

    protected BowWeapon(ZeroWeapons plugin, WeaponType type) {
        super(plugin, type);
    }

    /** A tagged arrow just left the bow. {@code force} is the draw, 0 to 1. */
    protected abstract void onShoot(Player shooter, AbstractArrow arrow, float force);

    /** A tagged arrow hit {@code victim}. */
    protected void onArrowHit(Player shooter, AbstractArrow arrow, LivingEntity victim, EntityDamageByEntityEvent event) {
    }

    protected void tag(AbstractArrow arrow) {
        arrow.getPersistentDataContainer().set(this.weaponKey, PersistentDataType.STRING, this.type.name());
    }

    protected boolean tagged(Entity arrow) {
        return this.type.name().equals(arrow.getPersistentDataContainer().get(this.weaponKey, PersistentDataType.STRING));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onShootEvent(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player shooter)) return;
        if (!(event.getProjectile() instanceof AbstractArrow arrow)) return;
        if (this.plugin.getWeaponManager().typeOf(event.getBow()) != this.type || !this.isEnabled()) return;
        if (this.inSpawn(shooter) || this.silenced(shooter)) return;
        this.tag(arrow);
        if (this.plugin.getConfig().getBoolean("debug")) this.plugin.getLogger().info("DBG shoot " + this.type + " force=" + event.getForce());
        this.onShoot(shooter, arrow, event.getForce());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamageEvent(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof AbstractArrow arrow) || !this.tagged(arrow)) return;
        if (!(arrow.getShooter() instanceof Player shooter)) return;
        if (!(event.getEntity() instanceof LivingEntity victim) || victim.equals(shooter)) return;
        if (isApplyingAbilityDamage() || !this.isEnabled()) return;
        if (victim instanceof Player victimPlayer && this.inSpawn(victimPlayer)) return;
        this.onArrowHit(shooter, arrow, victim, event);
    }
}
