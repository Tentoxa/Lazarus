package me.qiooip.lazarus.handlers.event;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

@Getter
public class LazarusKickEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private final KickType kickReason;
    private final String kickMessage;
    @Setter private boolean cancelled;

    public LazarusKickEvent(Player player, KickType kickReason, String kickMessage) {
        super(player);
        this.kickReason = kickReason;
        this.kickMessage = kickMessage;

        Bukkit.getPluginManager().callEvent(this);
    }

    @Override
    public HandlerList getHandlers() { return handlers; }

    public static HandlerList getHandlerList() { return handlers; }

    public enum KickType {
        DEATHBAN, LOGOUT, KICKALL, REBOOT, USERDATA_FAILED_TO_LOAD
    }
}
