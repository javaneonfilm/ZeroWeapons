package dev.cuda.zeroweapons.weapons.managers;

import dev.cuda.zeroweapons.ZeroWeapons;
import dev.cuda.zeroweapons.weapons.WeaponType;
import dev.cuda.zeroweapons.weapons.weapons.CustomWeapon;
import dev.cuda.zeroweapons.weapons.weapons.DashWeapon;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class WeaponManager {

    private final ZeroWeapons plugin;
    private final NamespacedKey weaponKey;
    private final Map<WeaponType, CustomWeapon> weapons = new EnumMap<>(WeaponType.class);
    private FileConfiguration weaponsConfig;

    public WeaponManager(ZeroWeapons plugin) {
        this.plugin = plugin;
        this.weaponKey = new NamespacedKey(plugin, "weapon_type");
        this.addMissingWeapons();
        this.loadWeaponsConfig();
        this.registerWeapons();
    }

    private void loadWeaponsConfig() {
        this.weaponsConfig = YamlConfiguration.loadConfiguration(this.weaponsFile());
    }

    private File weaponsFile() {
        return new File(this.plugin.getDataFolder(), "weapons.yml");
    }

    /**
     * Writes any weapon the live {@code weapons.yml} has never heard of into it.
     *
     * <p>The file is saved with overwrite off, so a server that was running before a weapon
     * existed keeps a copy with no section for it — and a weapon with no section is not merely
     * unconfigured, it is <i>gone</i>: {@code isEnabled()} is false and {@code createItem()}
     * hands back null. Shipping a new weapon would otherwise mean asking every admin to
     * hand-merge a config before it appeared, so it is merged here instead.
     *
     * <p>Only ever adds. A section that already exists is left exactly as it was tuned,
     * whatever the jar's copy now says about it.
     */
    private void addMissingWeapons() {
        File file = this.weaponsFile();
        // No file yet means saveResource is about to write the jar's copy out in full.
        if (!file.exists()) return;

        YamlConfiguration live = YamlConfiguration.loadConfiguration(file);
        try (InputStream in = this.plugin.getResource("weapons.yml")) {
            if (in == null) return;
            YamlConfiguration bundled = YamlConfiguration.loadConfiguration(
                new InputStreamReader(in, StandardCharsets.UTF_8));

            List<String> added = new ArrayList<>();
            for (WeaponType type : WeaponType.values()) {
                String path = "weapons." + type.configKey();
                if (live.isConfigurationSection(path)) continue;
                ConfigurationSection source = bundled.getConfigurationSection(path);
                if (source == null) continue;

                copySection(source, live.createSection(path));
                live.setComments(path, bundled.getComments(path));
                added.add(type.configKey());
            }
            if (added.isEmpty()) return;

            // A copy before the rewrite. Saving round-trips the entire file, and this one
            // holds tuning that exists nowhere else.
            Files.copy(file.toPath(), new File(file.getParentFile(), "weapons.yml.bak").toPath(),
                StandardCopyOption.REPLACE_EXISTING);
            live.save(file);
            this.plugin.getLogger().info("Added " + added.size() + " new weapon(s) to weapons.yml: "
                + String.join(", ", added) + " - previous file kept as weapons.yml.bak.");
        } catch (IOException e) {
            this.plugin.getLogger().severe("Could not merge new weapons into weapons.yml: " + e.getMessage());
        }
    }

    /**
     * Deep copy, one key at a time, comments included.
     *
     * {@code set(path, section)} would hand the other file's section object straight over and
     * leave two configs sharing it. Walking it is the version with no surprises, and it is
     * what carries the documentation across — a merged-in weapon arrives explained.
     */
    private static void copySection(ConfigurationSection from, ConfigurationSection to) {
        for (String key : from.getKeys(false)) {
            Object value = from.get(key);
            if (value instanceof ConfigurationSection child) {
                copySection(child, to.createSection(key));
            } else {
                to.set(key, value);
            }
            to.setComments(key, from.getComments(key));
            to.setInlineComments(key, from.getInlineComments(key));
        }
    }

    private void registerWeapons() {
        this.weapons.clear();
        this.register(new DashWeapon(this.plugin, WeaponType.DASH_SWORD));
        this.register(new dev.cuda.zeroweapons.weapons.weapons.swords.VortexSword(this.plugin));
        this.register(new dev.cuda.zeroweapons.weapons.weapons.swords.Nullblade(this.plugin));
        this.register(new dev.cuda.zeroweapons.weapons.weapons.swords.Overdrive(this.plugin));
        this.register(new dev.cuda.zeroweapons.weapons.weapons.axes.Dreadcleaver(this.plugin));
        this.register(new dev.cuda.zeroweapons.weapons.weapons.axes.LifestealAxe(this.plugin));
        this.register(new dev.cuda.zeroweapons.weapons.weapons.axes.CobAxe(this.plugin));
        this.register(new dev.cuda.zeroweapons.weapons.weapons.axes.Voidchopper(this.plugin));
        this.register(new dev.cuda.zeroweapons.weapons.weapons.bows.Splitshot(this.plugin));
        this.register(new dev.cuda.zeroweapons.weapons.weapons.bows.FlamingBow(this.plugin));
        this.register(new dev.cuda.zeroweapons.weapons.weapons.bows.CurveShot(this.plugin));
    }

    private void register(CustomWeapon weapon) {
        weapon.reloadConfig(this.getWeaponConfig(weapon.getType()));
        this.weapons.put(weapon.getType(), weapon);
    }

    public void reload() {
        this.addMissingWeapons();
        this.loadWeaponsConfig();
        for (CustomWeapon weapon : this.weapons.values()) weapon.reloadConfig(this.getWeaponConfig(weapon.getType()));
    }

    /** Tells every weapon the server is going down, so nothing it built outlives it. */
    public void shutdown() {
        for (CustomWeapon weapon : this.weapons.values()) {
            try {
                weapon.onShutdown();
            } catch (Exception e) {
                // One weapon failing to tidy up must not stop the next one from trying.
                this.plugin.getLogger().severe("Weapon shutdown error in "
                    + weapon.getType().configKey() + ": " + e.getMessage());
            }
        }
    }

    public CustomWeapon getWeapon(WeaponType type) {
        return this.weapons.get(type);
    }

    /**
     * Identity comes from persistent data, never from the display name. A player who
     * renames a plain sword to "Luck Sword" in an anvil gets a plain sword.
     */
    public CustomWeapon getWeaponFromItem(ItemStack item) {
        WeaponType type = this.typeOf(item);
        return type == null ? null : this.weapons.get(type);
    }

    public WeaponType typeOf(ItemStack item) {
        if (item == null || item.isEmpty()) return null;
        String raw = item.getPersistentDataContainer().get(this.weaponKey, PersistentDataType.STRING);
        if (raw == null) return null;
        try {
            return WeaponType.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public ItemStack createWeaponItem(WeaponType type) {
        CustomWeapon weapon = this.weapons.get(type);
        return weapon != null ? weapon.createItem() : null;
    }

    public ConfigurationSection getWeaponConfig(WeaponType type) {
        return this.weaponsConfig.getConfigurationSection("weapons." + type.configKey());
    }

    public int getWeaponCount() {
        return (int) this.weapons.values().stream().filter(CustomWeapon::isEnabled).count();
    }

    public Map<WeaponType, CustomWeapon> getWeapons() {
        return this.weapons;
    }
}
