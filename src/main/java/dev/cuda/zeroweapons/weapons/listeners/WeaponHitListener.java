package dev.cuda.zeroweapons.weapons.listeners;

import dev.cuda.zeroweapons.ZeroWeapons;
import dev.cuda.zeroweapons.core.Msg;
import dev.cuda.zeroweapons.weapons.utils.RegionUtils;
import dev.cuda.zeroweapons.weapons.weapons.CustomWeapon;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class WeaponHitListener implements Listener {

    private static final MiniMessage MM = dev.cuda.zeroweapons.core.Msg.MM;
    private final ZeroWeapons plugin;

    public WeaponHitListener(ZeroWeapons plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        // An ability landing its own damage must not set abilities off. One check here
        // covers every weapon, including the ones that don't know about each other.
        if (CustomWeapon.isApplyingAbilityDamage()) return;
        if (event.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) return;

        CustomWeapon weapon = this.plugin.getWeaponManager()
            .getWeaponFromItem(attacker.getInventory().getItemInMainHand());
        if (weapon == null) return;

        String region = this.plugin.getConfig().getString("spawn-region", "spawn");
        if (RegionUtils.isInRegion(attacker, region)) {
            // Custom weapons deal no damage at all in spawn, not merely no ability procs —
            // otherwise a stat-stick sword is still a free hit from safety.
            event.setCancelled(true);
            attacker.sendActionBar(MM.deserialize(this.plugin.getConfig().getString(
                "messages.spawn-region", "<color:#FF5555><color:#AAAAAA>ʏᴏᴜ ᴄᴀɴɴᴏᴛ ᴜꜱᴇ ᴛʜɪꜱ ᴡᴇᴀᴘᴏɴ ɪɴ ᴛʜᴇ ꜱᴘᴀᴡɴ ʀᴇɢɪᴏɴ.")));
            return;
        }

        if (!weapon.isEnabled()) return;
        // Nullblade lock: the swing lands, the weapon's trick doesn't.
        if (dev.cuda.zeroweapons.weapons.managers.WeaponStates.silenced(attacker)) return;
        weapon.onHit(attacker, event.getEntity(), event);
    }
}
