package me.qiooip.lazarus.games;

import lombok.Getter;
import lombok.Setter;
import me.qiooip.lazarus.utils.StringUtils;
import me.qiooip.lazarus.utils.StringUtils.FormatType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Capzone {

    private final Cuboid cuboid;

    @Setter private int time;
    private final List<String> players;

    // Track when the last second tick happened for millisecond precision
    private long lastTickTime;

    public Capzone(Cuboid cuboid, int time) {
        this.cuboid = cuboid;
        this.time = time;
        this.players = new ArrayList<>();
        this.lastTickTime = System.currentTimeMillis();
    }

    public boolean hasNoPlayers() {
        return this.players.isEmpty();
    }

    public void addPlayer(Player player) {
        this.players.add(player.getName());
        // Reset tick time when someone starts capping
        this.lastTickTime = System.currentTimeMillis();
    }

    public void removePlayer(Player player) {
        this.players.remove(player.getName());
    }

    public Player getNextCapper() {
        if(this.players.size() < 2) return null;
        return Bukkit.getPlayer(this.players.get(1));
    }

    public Player getCapper() {
        return Bukkit.getPlayer(this.getCapperName());
    }

    public String getCapperName() {
        return this.players.get(0);
    }

    public boolean isCapper(Player player) {
        return this.getCapperName().equals(player.getName());
    }

    public int decreaseTime() {
        this.lastTickTime = System.currentTimeMillis();
        return --this.time;
    }

    public String getTimeLeft() {
        return StringUtils.formatTime(this.time, FormatType.SECONDS_TO_MINUTES);
    }

    public long getTimeLeftMillis() {
        // Only apply sub-second precision when someone is capping
        if(this.players.isEmpty()) {
            return this.time * 1000L;
        }
        // Calculate actual milliseconds remaining including sub-second precision
        long millisSinceLastTick = System.currentTimeMillis() - this.lastTickTime;
        long totalMillis = (this.time * 1000L) - millisSinceLastTick;
        return Math.max(0, totalMillis);
    }
}
