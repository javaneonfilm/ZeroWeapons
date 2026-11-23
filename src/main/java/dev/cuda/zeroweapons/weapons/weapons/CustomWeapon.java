package dev.cuda.zeroweapons.weapons.weapons;

import dev.cuda.zeroweapons.ZeroWeapons;
import dev.cuda.zeroweapons.weapons.WeaponType;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public abstract class CustomWeapon {
    protected static final MiniMessage MM = dev.cuda.zeroweapons.core.Msg.MM;
    protected static final Random RANDOM = new Random();
    protected final ZeroWeapons plugin;
    protected final WeaponType type;
    protected final NamespacedKey weaponKey;
    protected ConfigurationSection cfg;

    public CustomWeapon(ZeroWeapons plugin, WeaponType type) {
        this.plugin = plugin;
        this.type = type;
        this.weaponKey = new NamespacedKey(plugin, "weapon_type");
        this.reloadConfig();
    }

    public void reloadConfig() {
        File file = new File(this.plugin.getDataFolder(), "weapons.yml");
        this.cfg = YamlConfiguration.loadConfiguration(file).getConfigurationSection("weapons." + this.type.configKey());
    }

    public WeaponType getType() {
        return this.type;
    }

    public boolean isEnabled() {
        return this.cfg != null && this.cfg.getBoolean("enabled", true);
    }

    public int getCooldown() {
        return this.cfg != null ? this.cfg.getInt("cooldown", 0) : 0;
    }

    public boolean hasCooldown() {
        return this.getCooldown() > 0;
    }

    public void onInteract(Player player, PlayerInteractEvent event, boolean sneaking) {
    }

    /**
     * Last call before the server goes down.
     *
     * <p>For a weapon that edits the world and schedules the tidy-up for later: a shutdown
     * cancels the scheduler, so whatever it put down is still there when the map next loads.
     * Anything that only touches players can ignore this — they all disconnect anyway.
     */
    public void onShutdown() {
    }

    public void onHit(Player player, Entity victim, EntityDamageByEntityEvent event) {
    }

    public ItemStack createItem() {
        if (this.cfg == null) {
            return null;
        }
        Material mat;
        String materialName = this.cfg.getString("material", "NETHERITE_SWORD");
        try {
            mat = Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            this.plugin.getLogger().warning("Invalid material '" + materialName + "' for weapon " + this.type.configKey());
            mat = Material.NETHERITE_SWORD;
        }
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        String nameMM = this.cfg.getString("name", this.type.name());
        meta.displayName(MM.deserialize(nameMM).decoration(TextDecoration.ITALIC, false));
        meta.lore(this.buildLore());
        for (var e : this.enchants().entrySet()) {
            Enchantment ench = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(e.getKey()));
            if (ench != null) {
                meta.addEnchant(ench, e.getValue(), true);
            } else {
                this.plugin.getLogger().warning("Unknown enchantment: " + e.getKey());
            }
        }
        // Unbreakable makes Unbreaking and Mending decorative. Left on by default because
        // a weapon snapping mid-fight in an FFA arena is worse than a redundant enchant,
        // but it's per-weapon config so it can be turned off.
        // A cooldown group of its own, so the ability's cooldown draws the vanilla sweep over
        // this item in the hotbar (like a shield after an axe hit) without greying out every
        // other item of the same material. The component's own length has to be positive or
        // the client drops it — 0.05s is too short to notice; applyCooldown sets the real one.
        org.bukkit.inventory.meta.components.UseCooldownComponent group = meta.getUseCooldown();
        group.setCooldownGroup(this.cooldownKey());
        group.setCooldownSeconds(0.05f);
        meta.setUseCooldown(group);

        meta.setUnbreakable(this.cfg.getBoolean("unbreakable", true));
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
        meta.getPersistentDataContainer().set(this.weaponKey, PersistentDataType.STRING, this.type.name());
        item.setItemMeta(meta);
        return item;
    }

    private List<Component> buildLore() {
        return WeaponLore.build(this.cfg, this.type, this.getCooldown(), this.enchants());
    }

    /** This weapon's enchants from weapons.yml. */
    public java.util.Map<String, Integer> enchants() {
        java.util.Map<String, Integer> out = new java.util.LinkedHashMap<>();
        ConfigurationSection own = this.cfg == null ? null : this.cfg.getConfigurationSection("enchants");
        if (own != null) {
            for (String key : own.getKeys(false)) out.merge(key.toLowerCase(java.util.Locale.ROOT), own.getInt(key), Math::max);
        }
        return out;
    }

    protected boolean roll(int percent) {
        return RANDOM.nextInt(100) < percent;
    }

    /**
     * True while a weapon is dealing its own ability damage.
     *
     * <p>Read by the hit listener, which skips dispatch while it is set. Everything an
     * ability does goes through {@link org.bukkit.entity.Damageable#damage(double,
     * org.bukkit.entity.Entity)} so the kill has an owner — and that fires another
     * {@code EntityDamageByEntityEvent} with the same attacker still holding the same weapon,
     * which lands straight back in {@code onHit}. A bleed tick would re-roll the bleed, and a
     * cleave would splash off its own splash.
     *
     * <p>That is not a slow leak, it is an escalation: the Rupture Axe rolls 35% five times
     * per bleed, so on average each bleed starts more than one more, and it never stops.
     *
     * <p>Main thread only, so a plain static field is the whole mechanism.
     */
    private static boolean applyingAbilityDamage = false;

    public static boolean isApplyingAbilityDamage() {
        return applyingAbilityDamage;
    }

    /**
     * Damage from an ability rather than from a swing.
     *
     * Attributed to {@code source}, so a kill by a bolt, a bleed or a cleave still pays out,
     * still counts for the streak and still writes a death message — but it cannot set any
     * ability off a second time.
     */
    /** Runs {@code hit} as ability damage, for abilities that build their own damage source. */
    protected static void withAbilityDamage(Runnable hit) {
        applyingAbilityDamage = true;
        try {
            hit.run();
        } finally {
            applyingAbilityDamage = false;
        }
    }

    protected static void dealAbilityDamage(LivingEntity victim, double amount, Player source) {
        applyingAbilityDamage = true;
        try {
            victim.damage(amount, source);
        } finally {
            applyingAbilityDamage = false;
        }
    }

    /**
     * Ability damage that goes through armour (and fire resistance), credited to {@code source}.
     *
     * <p>Hurt cooldown is cleared for the hit and put back after, so it lands even right after a
     * swing, and it doesn't shorten or lengthen the victim's normal hit frames.
     */
    protected static void trueDamage(LivingEntity victim, double amount, Player source) {
        if (amount <= 0.0 || victim.isDead()) return;
        int frames = victim.getNoDamageTicks();
        double last = victim.getLastDamage();
        org.bukkit.damage.DamageSource damage = org.bukkit.damage.DamageSource.builder(org.bukkit.damage.DamageType.MAGIC)
            .withCausingEntity(source).withDirectEntity(source).build();
        victim.setNoDamageTicks(0);
        withAbilityDamage(() -> victim.damage(amount, damage));
        if (!victim.isDead()) {
            victim.setNoDamageTicks(frames);
            victim.setLastDamage(last);
        }
    }

    /** True while {@code player} is under a Nullblade lock: no abilities go off. */
    protected boolean silenced(Player player) {
        return dev.cuda.zeroweapons.weapons.managers.WeaponStates.silenced(player);
    }

    /** Player in the spawn region, where no ability may touch them. */
    protected boolean inSpawn(Player player) {
        return dev.cuda.zeroweapons.weapons.utils.RegionUtils.isInRegion(player,
            this.plugin.getConfig().getString("spawn-region", "spawn"));
    }

    /** The vanilla cooldown group this weapon's items belong to. */
    public NamespacedKey cooldownKey() {
        return new NamespacedKey(this.plugin, "cooldown_" + this.type.configKey());
    }

    /**
     * Starts this weapon's cooldown: the boss bar, and the vanilla sweep over the item.
     *
     * The sweep is set on the group key directly, not through a held ItemStack, which would
     * fall back to the material (and grey out every sword) if the group couldn't be resolved.
     */
    protected void applyCooldown(Player player) {
        int cooldown = this.getCooldown();
        if (cooldown > 0) {
            this.plugin.getCooldownManager().setCooldown(player, this.type, cooldown);
            player.setCooldown(this.cooldownKey(), cooldown * 20);
        }
    }


}
