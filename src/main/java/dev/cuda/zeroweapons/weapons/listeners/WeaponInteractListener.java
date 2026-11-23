package dev.cuda.zeroweapons.weapons.listeners;

import dev.cuda.zeroweapons.ZeroWeapons;
import dev.cuda.zeroweapons.core.Msg;
import dev.cuda.zeroweapons.weapons.WeaponType;
import dev.cuda.zeroweapons.weapons.managers.CooldownManager;
import dev.cuda.zeroweapons.weapons.utils.RegionUtils;
import dev.cuda.zeroweapons.weapons.weapons.CustomWeapon;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class WeaponInteractListener implements Listener {
    private final ZeroWeapons plugin;
    private static final MiniMessage MM = dev.cuda.zeroweapons.core.Msg.MM;

    public WeaponInteractListener(ZeroWeapons plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Action action = event.getAction();
        boolean isRightClick = action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
        if (!isRightClick) return;

        Player player = event.getPlayer();
        CustomWeapon weapon = this.plugin.getWeaponManager().getWeaponFromItem(player.getInventory().getItemInMainHand());
        if (weapon == null || !weapon.isEnabled()) return;

        String region = this.plugin.getConfig().getString("spawn-region", "spawn");
        if (RegionUtils.isInRegion(player, region)) {
            String msg = this.plugin.getConfig().getString("messages.spawn-region", "<color:#FF5555><color:#AAAAAA>ʏᴏᴜ ᴄᴀɴɴᴏᴛ ᴜꜱᴇ ᴛʜɪꜱ ᴡᴇᴀᴘᴏɴ ɪɴ ᴛʜᴇ ꜱᴘᴀᴡɴ ʀᴇɢɪᴏɴ.");
        player.sendActionBar(MM.deserialize(msg));
            event.setCancelled(true);
            return;
        }

        boolean sneaking = player.isSneaking();
        if (sneaking && !(weapon instanceof dev.cuda.zeroweapons.weapons.weapons.bows.BowWeapon)
            && dev.cuda.zeroweapons.weapons.managers.WeaponStates.silenced(player)) {
            // Nullblade lock: the ability is held back. Plain right clicks (blocking, charging
            // a spear, drawing a bow) still work.
            event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
            event.setCancelled(true);
            Msg.actionBar(player, dev.cuda.zeroweapons.core.Palette.b(dev.cuda.zeroweapons.core.Palette.NULL, "NULLED")
                + " <color:" + dev.cuda.zeroweapons.core.Palette.INK + ">" + dev.cuda.zeroweapons.core.Palette.sc("no abilities") + " "
                + String.format("%.1f", dev.cuda.zeroweapons.weapons.managers.WeaponStates.silenceLeft(player)) + "s</color>");
            return;
        }
        CooldownManager cm = this.plugin.getCooldownManager();
        WeaponType wt = weapon.getType();
        if (weapon.hasCooldown() && cm.isOnCooldown(player, wt)) {
            double remaining = cm.getRemainingSeconds(player, wt);
            String msg = this.plugin.getConfig().getString("messages.on-cooldown", "<color:#FF5555><color:#AAAAAA>This weapon is on cooldown for <color:#FFC93D>{time}s</color>!")
                .replace("{time}", String.format("%.1f", remaining));
        player.sendActionBar(MM.deserialize(msg));
            event.setCancelled(true);
            return;
        }
        weapon.onInteract(player, event, sneaking);
    }
}
