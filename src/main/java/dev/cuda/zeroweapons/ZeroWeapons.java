package dev.cuda.zeroweapons;

import dev.cuda.zeroweapons.weapons.commands.WeaponsCommand;
import dev.cuda.zeroweapons.weapons.listeners.WeaponHitListener;
import dev.cuda.zeroweapons.weapons.listeners.WeaponInteractListener;
import dev.cuda.zeroweapons.weapons.managers.CooldownManager;
import dev.cuda.zeroweapons.weapons.managers.WeaponManager;
import dev.cuda.zeroweapons.weapons.managers.WeaponStates;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

/** Custom weapons: 6 swords and axes with on-hit tricks, a dash sword, 3 bows. */
public final class ZeroWeapons extends JavaPlugin implements Listener {

    private static ZeroWeapons instance;
    private CooldownManager cooldownManager;
    private WeaponManager weaponManager;

    @Override
    public void onEnable() {
        instance = this;
        this.saveDefaultConfig();
        this.saveResource("weapons.yml", false);
        this.cooldownManager = new CooldownManager();
        this.weaponManager = new WeaponManager(this);

        var pm = this.getServer().getPluginManager();
        pm.registerEvents(this, this);
        pm.registerEvents(new WeaponInteractListener(this), this);
        pm.registerEvents(new WeaponHitListener(this), this);
        pm.registerEvents(new WeaponStates(this), this);
        // Weapons with events of their own (bows, the Cob Axe's punch, Overdrive, Voidchopper).
        for (var weapon : this.weaponManager.getWeapons().values()) {
            if (weapon instanceof Listener listener) pm.registerEvents(listener, this);
        }

        PluginCommand command = this.getCommand("weapons");
        if (command != null) {
            WeaponsCommand handler = new WeaponsCommand(this);
            command.setExecutor(handler);
            command.setTabCompleter(handler);
        }
        this.getLogger().info(this.weaponManager.getWeaponCount() + " weapons ready. /weapons give <weapon>");
    }

    @Override
    public void onDisable() {
        if (this.weaponManager != null) this.weaponManager.shutdown();
        instance = null;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        java.util.UUID playerId = event.getPlayer().getUniqueId();
        this.cooldownManager.purgeExpired();
        for (var weapon : this.weaponManager.getWeapons().values()) weapon.forgetPlayer(playerId);
    }

    public static ZeroWeapons get() {
        return instance;
    }

    public CooldownManager getCooldownManager() {
        return this.cooldownManager;
    }

    public WeaponManager getWeaponManager() {
        return this.weaponManager;
    }
}
