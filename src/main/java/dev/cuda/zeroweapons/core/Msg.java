package dev.cuda.zeroweapons.core;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** MiniMessage in, chat and action bar out. */
public final class Msg {

    public static final MiniMessage MM = MiniMessage.miniMessage();
    private static final String PREFIX = "<color:" + Palette.BRAND + ">" + Palette.sc("weapons") + "</color> <color:" + Palette.DIM + ">»</color> ";

    private Msg() {}

    public static Component parse(String miniMessage) {
        return MM.deserialize(miniMessage);
    }

    public static void actionBar(Player player, String miniMessage) {
        player.sendActionBar(parse(miniMessage));
    }

    public static void send(CommandSender to, String miniMessage) {
        to.sendMessage(parse(PREFIX + miniMessage));
    }

    public static void reply(CommandSender to, String miniMessage) {
        send(to, miniMessage);
    }

    public static void replySuccess(CommandSender to, String text) {
        send(to, "<color:" + Palette.GOOD + ">" + text + "</color>");
    }
}
