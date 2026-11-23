package dev.cuda.zeroweapons.weapons.weapons;

import dev.cuda.zeroweapons.ZeroWeapons;
import dev.cuda.zeroweapons.weapons.WeaponType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.Vector;

/**
 * Shift + right-click throws you along your crosshair. The Dash Sword, Dash Mace and Dash Spear
 * are all this class with a different item; the Venom Sword builds on it.
 *
 * <p>The sound is the sculk shrieker's place1. Vanilla's block.sculk_shrieker.place has five
 * variants, and the client picks one with a random seeded from the sound packet. Seed 0 always
 * lands on place1 (java.util.Random(0).nextInt(5) == 0), so everyone hears place1, no pack.
 */
public class DashWeapon extends CustomWeapon {

    /** block.sculk_shrieker.place with seed 0, which is always its place1 variant. */
    private static final net.kyori.adventure.sound.Sound DASH_SOUND = net.kyori.adventure.sound.Sound.sound()
        .type(org.bukkit.Sound.BLOCK_SCULK_SHRIEKER_PLACE)
        .source(net.kyori.adventure.sound.Sound.Source.PLAYER)
        .volume(1.0f).pitch(1.0f).seed(0L)
        .build();

    public DashWeapon(ZeroWeapons plugin, WeaponType type) {
        super(plugin, type);
    }

    @Override
    public void onInteract(Player player, PlayerInteractEvent event, boolean sneaking) {
        if (!sneaking) return;
        // Cancel first: a spear would otherwise start charging, a mace does nothing either way.
        event.setUseItemInHand(Event.Result.DENY);
        event.setCancelled(true);
        if (!this.dash(player)) return;
        this.afterDash(player);
        this.applyCooldown(player);
    }

    /** Throws the player forward. False only when there's no direction to throw them in. */
    protected boolean dash(Player player) {
        double power = this.cfg.getDouble("dash_power", 1.9);
        double maxPitch = this.cfg.getDouble("max_vertical", 0.6);

        // Straight down the crosshair, pitch included. The vertical part is clamped so looking
        // at the sky is a dash, not a launch into orbit.
        Vector direction = player.getLocation().getDirection().normalize();
        direction.setY(Math.max(-maxPitch, Math.min(maxPitch, direction.getY())));
        if (direction.lengthSquared() < 1.0E-4) return false;
        player.setVelocity(direction.normalize().multiply(power));

        playDashSound(this.plugin, player.getLocation());
        player.getWorld().spawnParticle(Particle.SCULK_SOUL, player.getLocation().add(0.0, 0.6, 0.0),
            10, 0.25, 0.2, 0.25, 0.02);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0.0, 0.4, 0.0),
            14, 0.25, 0.15, 0.25, 0.03);
        return true;
    }

    /** Hook for weapons that do something during or after the dash. */
    protected void afterDash(Player player) {
    }

    public static void playDashSound(ZeroWeapons plugin, Location at) {
        at.getWorld().playSound(DASH_SOUND, at.getX(), at.getY(), at.getZ());
    }
}
