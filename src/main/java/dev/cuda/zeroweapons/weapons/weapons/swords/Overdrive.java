package dev.cuda.zeroweapons.weapons.weapons.swords;

import dev.cuda.zeroweapons.ZeroWeapons;
import dev.cuda.zeroweapons.weapons.WeaponType;
import dev.cuda.zeroweapons.weapons.managers.WeaponStates;
import dev.cuda.zeroweapons.weapons.weapons.CustomWeapon;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Every hit in a row speeds you up a little. A miss, a new target, putting the sword away or a
 * pause ends it and the speed goes straight back to normal.
 *
 * <p>The speed is a transient attribute modifier, so it is never saved into player data even if
 * the server dies mid-fight. It's also cleared on join just in case.
 */
public class Overdrive extends CustomWeapon implements Listener {

    private static final Particle.DustOptions CYAN = new Particle.DustOptions(Color.fromRGB(0x6BFFF2), 0.9f);

    private final NamespacedKey speedKey;
    /** Players with the speed on, and how many stacks it was built for. */
    private final Map<UUID, Integer> boosted = new HashMap<>();

    public Overdrive(ZeroWeapons plugin) {
        super(plugin, WeaponType.OVERDRIVE);
        this.speedKey = new NamespacedKey(plugin, "overdrive_speed");
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 2L, 2L);
    }

    private double window() {
        return this.cfg.getDouble("combo_window_seconds", 3.0);
    }

    @Override
    public void onHit(Player player, Entity victim, EntityDamageByEntityEvent event) {
        if (!(victim instanceof Player)) return;
        int count = WeaponStates.hit(player, victim, this.type, this.window());
        int stacks = Math.min(count, Math.max(1, this.cfg.getInt("max_stacks", 5)));
        this.apply(player, stacks);
        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 0.2, 0), 4 + stacks * 2, 0.3, 0.05, 0.3, 0.0, CYAN);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 0.8f + stacks * 0.15f);
    }

    private void apply(Player player, int stacks) {
        Integer had = this.boosted.get(player.getUniqueId());
        if (had != null && had == stacks) return;
        AttributeInstance speed = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed == null) return;
        speed.removeModifier(this.speedKey);
        double per = this.cfg.getDouble("speed_per_hit", 0.04);
        speed.addTransientModifier(new AttributeModifier(this.speedKey, per * stacks, AttributeModifier.Operation.ADD_SCALAR));
        this.boosted.put(player.getUniqueId(), stacks);
    }

    private void clear(Player player) {
        this.boosted.remove(player.getUniqueId());
        AttributeInstance speed = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(this.speedKey);
    }

    /** Takes the speed off anyone whose streak ended or who isn't holding the sword now. */
    private void tick() {
        if (this.boosted.isEmpty()) return;
        Iterator<UUID> it = this.boosted.keySet().iterator();
        while (it.hasNext()) {
            Player player = this.plugin.getServer().getPlayer(it.next());
            if (player == null) {
                it.remove();
                continue;
            }
            boolean holding = this.plugin.getWeaponManager().typeOf(player.getInventory().getItemInMainHand()) == this.type;
            if (!holding || player.isDead() || WeaponStates.combo(player, this.type, this.window()) == 0) {
                it.remove();
                AttributeInstance speed = player.getAttribute(Attribute.MOVEMENT_SPEED);
                if (speed != null) speed.removeModifier(this.speedKey);
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        this.clear(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.clear(event.getPlayer());
    }

    @Override
    public void onShutdown() {
        for (UUID id : this.boosted.keySet().toArray(new UUID[0])) {
            Player player = this.plugin.getServer().getPlayer(id);
            if (player != null) this.clear(player);
        }
    }
}
