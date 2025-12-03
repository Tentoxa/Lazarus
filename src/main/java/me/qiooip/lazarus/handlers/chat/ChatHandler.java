package me.qiooip.lazarus.handlers.chat;

import lombok.Getter;
import me.qiooip.lazarus.Lazarus;
import me.qiooip.lazarus.config.Config;
import me.qiooip.lazarus.factions.FactionsManager;
import me.qiooip.lazarus.factions.type.PlayerFaction;
import me.qiooip.lazarus.handlers.manager.Handler;
import me.qiooip.lazarus.integration.Chat_LuckPerms;
import me.qiooip.lazarus.userdata.Userdata;
import me.qiooip.lazarus.utils.Color;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.PluginManager;

public class ChatHandler extends Handler implements Listener {

    @Getter public static ChatHandler instance;

    public static void setup() {
        PluginManager pluginManager = Bukkit.getPluginManager();


        if(pluginManager.isPluginEnabled("LuckPerms")) {
            instance = new Chat_LuckPerms();
        } else {
            instance = new ChatHandler();
        }
    }

    public String getRankName(Player player) {
        return "";
    }

    protected String getTag(Player player) {
        return "";
    }

    public String getPrefix(Player player) {
        return "";
    }

    public String getNameColor(Player player) {
        return "";
    }

    protected String getSuffix(Player player) {
        return "";
    }

    protected String getChatColor(Player player) {
        return "";
    }

    private String getPlayerDisplayName(Player player) {
        if(Config.CHAT_FORMAT_USE_PLAYER_DISPLAY_NAME) {
            return player.getDisplayName();
        }

        return Color.translate(instance.getTag(player) + instance.getPrefix(player)
            + instance.getNameColor(player) +  player.getName() + instance.getSuffix(player));
    }

    private String getChatMessage(PlayerFaction playerFaction, CommandSender recipient,
                                  String displayName, String chatColor, String message) {

        if(playerFaction == null) {
            return Config.CHAT_FORMAT.replace("<displayName>", displayName) + chatColor + message;
        }

        return Config.CHAT_FORMAT_WITH_FACTION
            .replace("<faction>", playerFaction.getName(recipient))
            .replace("<displayName>", displayName) + chatColor + message;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        if(!Config.CHAT_FORMAT_ENABLED) return;

        event.setCancelled(true);

        Player player = event.getPlayer();
        PlayerFaction faction = FactionsManager.getInstance().getPlayerFaction(player);

        String displayName = this.getPlayerDisplayName(player);
        String chatColor = Color.translate(instance.getChatColor(player));

        String message = player.hasPermission("lazarus.chat.color")
            ? Color.translate(event.getMessage())
            : event.getMessage();

        Bukkit.getConsoleSender().sendMessage(this.getChatMessage(faction,
            Bukkit.getConsoleSender(), displayName, chatColor, message));

        event.getRecipients().forEach(recipient -> {
            Userdata userdata = Lazarus.getInstance().getUserdataManager().getUserdata(recipient);

            if((player != recipient && !player.hasPermission("lazarus.staff") && !userdata
                .getSettings().isPublicChat()) || userdata.isIgnoring(player)) return;

            recipient.sendMessage(this.getChatMessage(faction, recipient, displayName, chatColor, message));
        });
    }
}
