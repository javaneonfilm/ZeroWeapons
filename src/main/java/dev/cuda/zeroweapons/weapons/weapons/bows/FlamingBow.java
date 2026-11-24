package dev.cuda.zeroweapons.weapons.weapons.bows;

import dev.cuda.zeroweapons.ZeroWeapons;
import dev.cuda.zeroweapons.weapons.WeaponType;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * A hit sets them burning harder than normal fire, and fire resistance doesn't help.
 *
 * <p>On top of vanilla fire, the burn deals its own damage every second as magic damage (so
 * armour and fire resistance both let it through), credited to the shooter. They look on fire
 * the whole time, even with fire resistance. Water puts it out. A new hit restarts the timer.
 */
public class FlamingBow extends BowWeapon {

    /** Burning players and when their burn ends. */
    private final Map<UUID, Long> burning = new HashMap<>();

    public FlamingBow(ZeroWeapons plugin) {
        super(plugin, WeaponType.FLAMING_BOW);
    }

    @Override
    protected void onShoot(Player shooter, AbstractArrow arrow, float force) {
        arrow.setFireTicks(2000);
        dev.cuda.zeroweapons.fx.WeaponFx.flameTrail(arrow);
    }

    @Override
    protected void onArrowHit(Player shooter, AbstractArrow arrow, LivingEntity victim, EntityDamageByEntityEvent event) {
        int seconds = Math.max(1, this.cfg.getInt("burn_seconds", 4));
        double perSecond = this.cfg.getDouble("burn_damage", 1.0);
        boolean hellfire = victim.hasPotionEffect(org.bukkit.potion.PotionEffectType.FIRE_RESISTANCE);
        victim.setFireTicks(Math.max(victim.getFireTicks(), seconds * 20));
        victim.setVisualFire(net.kyori.adventure.util.TriState.TRUE);
        victim.getWorld().playSound(victim.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1.0f, 0.9f);
        dev.cuda.zeroweapons.fx.WeaponFx.flameIgnite(victim, hellfire);

        boolean running = this.burning.containsKey(victim.getUniqueId());
        this.burning.put(victim.getUniqueId(), System.currentTimeMillis() + seconds * 1000L);
        if (running) return;
        dev.cuda.zeroweapons.fx.WeaponFx.flameBurn(victim, hellfire, () -> this.burning.containsKey(victim.getUniqueId()));

        new BukkitRunnable() {
            @Override
            public void run() {
                Long until = FlamingBow.this.burning.get(victim.getUniqueId());
                boolean over = until == null || System.currentTimeMillis() >= until + 50
                    || !victim.isValid() || victim.isDead() || victim.isInWater()
                    || (victim instanceof Player p && (!p.isOnline() || FlamingBow.this.inSpawn(p)));
                if (over) {
                    FlamingBow.this.burning.remove(victim.getUniqueId());
                    victim.setVisualFire(net.kyori.adventure.util.TriState.NOT_SET);
                    this.cancel();
                    return;
                }
                trueDamage(victim, perSecond, shooter);
            }
        }.runTaskTimer(this.plugin, 20L, 20L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (this.burning.remove(event.getPlayer().getUniqueId()) != null) {
            event.getPlayer().setVisualFire(net.kyori.adventure.util.TriState.NOT_SET);
        }
    }

    @Override
    public void onShutdown() {
        for (UUID id : this.burning.keySet()) {
            Entity entity = this.plugin.getServer().getEntity(id);
            if (entity != null) entity.setVisualFire(net.kyori.adventure.util.TriState.NOT_SET);
        }
        this.burning.clear();
    }
}
