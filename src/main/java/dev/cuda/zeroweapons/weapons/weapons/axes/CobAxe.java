package dev.cuda.zeroweapons.weapons.weapons.axes;

import dev.cuda.zeroweapons.ZeroWeapons;
import dev.cuda.zeroweapons.core.Palette;
import dev.cuda.zeroweapons.core.Msg;
import dev.cuda.zeroweapons.weapons.WeaponType;
import dev.cuda.zeroweapons.weapons.weapons.CustomWeapon;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.BoundingBox;

/**
 * Cuts through cobwebs. Hitting a web pops it instantly; shift + right click clears every web
 * around you.
 *
 * <p>Nobody can break blocks outside build mode, so the webs are removed directly. Webs that are
 * part of the map grow back after a while (never inside a player), and any still waiting are put
 * back on shutdown, so the axe can't wear the map down.
 */
public class CobAxe extends CustomWeapon implements Listener {

    /** Cleared webs waiting to grow back, and when. */
    private final Map<Location, Long> regrow = new LinkedHashMap<>();

    public CobAxe(ZeroWeapons plugin) {
        super(plugin, WeaponType.COB_AXE);
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::regrowTick, 20L, 20L);
    }

    @Override
    public void onInteract(Player player, PlayerInteractEvent event, boolean sneaking) {
        if (!sneaking) return;
        event.setUseItemInHand(Event.Result.DENY);
        event.setCancelled(true);

        int radius = Math.max(1, this.cfg.getInt("clear_radius", 4));
        Block centre = player.getLocation().getBlock();
        int cleared = 0;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + y * y + z * z > radius * radius) continue;
                    Block block = centre.getRelative(x, y, z);
                    if (block.getType() == Material.COBWEB) {
                        this.cut(block, cleared < 12);
                        cleared++;
                    }
                }
            }
        }
        if (cleared == 0) {
            Msg.actionBar(player, Palette.b(Palette.COB, "COB AXE")
                + " <color:" + Palette.INK + ">" + Palette.sc("no webs near") + "</color>");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.4f, 1.8f);
            return;
        }
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SHEEP_SHEAR, 1.0f, 0.8f);
        Msg.actionBar(player, Palette.b(Palette.COB, "CLEARED")
            + " <color:" + Palette.INK + ">" + cleared + " " + Palette.sc(cleared == 1 ? "web" : "webs") + "</color>");
        this.applyCooldown(player);
    }

    /** Left click on a web with the axe: gone at once. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPunch(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) return;
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.COBWEB) return;
        Player player = event.getPlayer();
        if (this.plugin.getWeaponManager().typeOf(player.getInventory().getItemInMainHand()) != this.type) return;
        if (!this.isEnabled() || this.inSpawn(player) || this.silenced(player)) return;
        event.setCancelled(true);
        this.cut(block, true);
        block.getWorld().playSound(block.getLocation(), Sound.ENTITY_SHEEP_SHEAR, 0.8f, 1.2f);
    }

    private void cut(Block block, boolean effects) {
        Location at = block.getLocation();
        block.setType(Material.AIR);
        if (effects) {
            block.getWorld().spawnParticle(Particle.BLOCK, at.clone().add(0.5, 0.5, 0.5), 12, 0.3, 0.3, 0.3, 0.0,
                Material.COBWEB.createBlockData());
        }
        int seconds = this.cfg.getInt("regrow_seconds", 20);
        if (seconds <= 0) return;
        this.regrow.putIfAbsent(at, System.currentTimeMillis() + seconds * 1000L);
    }

    private void regrowTick() {
        if (this.regrow.isEmpty()) return;
        long now = System.currentTimeMillis();
        var it = this.regrow.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            if (now < entry.getValue()) continue;
            Location at = entry.getKey();
            if (!at.isChunkLoaded()) continue;
            Block block = at.getBlock();
            if (!block.getType().isAir()) {
                it.remove();
                continue;
            }
            // Never grow a web inside someone; wait until they move.
            BoundingBox box = BoundingBox.of(block);
            boolean occupied = false;
            for (Entity near : block.getWorld().getNearbyEntities(box)) {
                if (near instanceof Player) {
                    occupied = true;
                    break;
                }
            }
            if (occupied) continue;
            block.setType(Material.COBWEB);
            it.remove();
        }
    }

    @Override
    public void onShutdown() {
        for (Location at : this.regrow.keySet()) {
            if (at.getBlock().getType().isAir()) at.getBlock().setType(Material.COBWEB);
        }
        this.regrow.clear();
    }
}
