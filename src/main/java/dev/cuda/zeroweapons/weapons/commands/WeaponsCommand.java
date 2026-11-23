package dev.cuda.zeroweapons.weapons.commands;

import dev.cuda.zeroweapons.ZeroWeapons;
import dev.cuda.zeroweapons.core.Palette;
import dev.cuda.zeroweapons.core.Msg;
import dev.cuda.zeroweapons.weapons.WeaponType;
import dev.cuda.zeroweapons.weapons.weapons.CustomWeapon;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/** /weapons — hand out, list and reload the custom arsenal. */
public class WeaponsCommand implements CommandExecutor, TabCompleter {

    private final ZeroWeapons plugin;

    public WeaponsCommand(ZeroWeapons plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("zeroweapons.admin")) {
            Msg.reply(sender, Palette.l(Palette.BAD, "You don't have permission."));
            return true;
        }
        if (args.length == 0) {
            this.usage(sender);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "list" -> {
                sender.sendMessage(Msg.parse(""));
                sender.sendMessage(Msg.parse(Palette.b(Palette.BRAND, "arsenal")));
                for (CustomWeapon weapon : this.plugin.getWeaponManager().getWeapons().values()) {
                    WeaponType type = weapon.getType();
                    sender.sendMessage(Msg.parse("  " + type.coloredName()
                        + "  " + Palette.c(Palette.DIM, type.configKey())
                        + (weapon.isEnabled() ? "" : " " + Palette.l(Palette.BAD, "off"))
                        + (weapon.getCooldown() > 0
                            ? "  " + Palette.c(Palette.DIM, weapon.getCooldown() + "s") : "")));
                }
                sender.sendMessage(Msg.parse(""));
            }
            case "reload" -> {
                this.plugin.getWeaponManager().reload();
                Msg.replySuccess(sender, Palette.sc("Weapon configuration reloaded."));
            }
            case "give" -> {
                if (args.length < 2) {
                    this.usage(sender);
                    return true;
                }
                WeaponType type;
                try {
                    type = WeaponType.valueOf(args[1].toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    Msg.reply(sender, "<color:" + Palette.BAD + ">Unknown weapon '" + args[1] + "'.</color>");
                    return true;
                }
                Player target = args.length >= 3
                    ? this.plugin.getServer().getPlayerExact(args[2])
                    : (sender instanceof Player self ? self : null);
                if (target == null) {
                    Msg.reply(sender, Palette.l(Palette.BAD, "Specify an online player."));
                    return true;
                }
                ItemStack item = this.plugin.getWeaponManager().createWeaponItem(type);
                if (item == null) {
                    Msg.reply(sender, Palette.l(Palette.BAD, "That weapon has no config entry."));
                    return true;
                }
                target.getInventory().addItem(item);
                Msg.replySuccess(sender, "Gave " + type.displayName() + " to " + target.getName() + ".");
                Msg.send(target, Palette.l(Palette.INK, "You received ") + type.coloredName());
            }
            default -> this.usage(sender);
        }
        return true;
    }

    private void usage(CommandSender sender) {
        Msg.reply(sender, Palette.l(Palette.INK, "Usage: ")
            + Palette.c(Palette.GLOW, "/weapons <give|list|reload> [weapon] [player]"));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) return List.of("give", "list", "reload");
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            List<String> names = new ArrayList<>();
            for (WeaponType type : WeaponType.values()) names.add(type.configKey());
            names.removeIf(name -> !name.startsWith(args[1].toLowerCase(Locale.ROOT)));
            return names;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            List<String> names = new ArrayList<>();
            for (Player online : this.plugin.getServer().getOnlinePlayers()) names.add(online.getName());
            return names;
        }
        return List.of();
    }
}
