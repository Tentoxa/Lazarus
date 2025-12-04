package me.qiooip.lazarus.lunarclient.task;

import com.lunarclient.bukkitapi.LunarClientAPI;
import com.lunarclient.bukkitapi.nethandler.LCPacket;
import com.lunarclient.bukkitapi.nethandler.client.LCPacketTeammates;
import lombok.Getter;
import me.qiooip.lazarus.Lazarus;
import me.qiooip.lazarus.factions.FactionsManager;
import me.qiooip.lazarus.factions.event.FactionDisbandEvent;
import me.qiooip.lazarus.factions.event.PlayerJoinFactionEvent;
import me.qiooip.lazarus.factions.event.PlayerLeaveFactionEvent;
import me.qiooip.lazarus.factions.type.PlayerFaction;
import me.qiooip.lazarus.lunarclient.LunarClientManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeamViewTask extends BukkitRunnable implements Listener {

    private final LCPacket emptyTeammatesPacket;

    public TeamViewTask() {
        this.emptyTeammatesPacket = new LCPacketTeammates(null, 1L, Collections.emptyMap());
        this.runTaskTimerAsynchronously(Lazarus.getInstance(), 0L, 20L);
        Bukkit.getPluginManager().registerEvents(this, Lazarus.getInstance());
    }

    private Map<String, Double> positionMap(Location location) {
        Map<String, Double> position = new HashMap<>();

        position.put("x", location.getX());
        position.put("y", location.getY());
        position.put("z", location.getZ());

        return position;
    }

    private Map<Environment, LCPacket> createTeammatePackets(PlayerFaction faction) {
        Map<Environment, PositionMap> positions = new HashMap<>();

        faction.getOnlinePlayers().forEach(member -> {
            Environment env = member.getWorld().getEnvironment();
            UUID uuid = member.getUniqueId();

            positions.computeIfAbsent(env, t -> new PositionMap())
                .addPosition(uuid, this.positionMap(member.getLocation()));
        });

        Map<Environment, LCPacket> packets = new HashMap<>();

        positions.forEach((env, positionMap) -> packets.put(env,
            new LCPacketTeammates(null, 2000L, positionMap.getPositions())));

        return packets;
    }

    private void sendPerWorldPackets(PlayerFaction faction, Map<Environment, LCPacket> packets) {
        LunarClientManager lcManager = Lazarus.getInstance().getLunarClientManager();
        
        faction.getOnlinePlayers().forEach(member -> {
            if(!lcManager.isOnLunarClient(member)) return;
            
            LCPacket packet = packets.get(member.getWorld().getEnvironment());
            if(packet != null) {
                LunarClientAPI.getInstance().sendPacket(member, packet);
            }
        });
    }

    private void sendTeamViewPackets() {
        LunarClientManager lcManager = Lazarus.getInstance().getLunarClientManager();
        FactionsManager factionsManager = FactionsManager.getInstance();

        Map<PlayerFaction, Map<Environment, LCPacket>> factions = new HashMap<>();

        for(Player player : Bukkit.getOnlinePlayers()) {
            if(!lcManager.isOnLunarClient(player)) continue;

            PlayerFaction faction = factionsManager.getPlayerFaction(player);

            if(faction != null && !factions.containsKey(faction)) {
                factions.put(faction, this.createTeammatePackets(faction));
            }
        }

        factions.forEach((faction, packets) -> this.sendPerWorldPackets(faction, packets));
    }

    private void clearTeammatesForPlayer(Player player) {
        LunarClientManager lcManager = Lazarus.getInstance().getLunarClientManager();
        if(lcManager.isOnLunarClient(player)) {
            LunarClientAPI.getInstance().sendPacket(player, this.emptyTeammatesPacket);
        }
    }

    private void updateFactionTeammates(PlayerFaction faction, Player excludePlayer) {
        if(faction == null || faction.getOnlinePlayers().isEmpty()) return;

        LunarClientManager lcManager = Lazarus.getInstance().getLunarClientManager();
        
        // Create packets excluding the player who is leaving/joining
        Map<Environment, PositionMap> positions = new HashMap<>();
        
        faction.getOnlinePlayers().forEach(member -> {
            if(member.equals(excludePlayer)) return;
            
            Environment env = member.getWorld().getEnvironment();
            UUID uuid = member.getUniqueId();

            positions.computeIfAbsent(env, t -> new PositionMap())
                .addPosition(uuid, this.positionMap(member.getLocation()));
        });

        Map<Environment, LCPacket> packets = new HashMap<>();
        positions.forEach((env, positionMap) -> packets.put(env,
            new LCPacketTeammates(null, 2000L, positionMap.getPositions())));

        faction.getOnlinePlayers().forEach(member -> {
            if(member.equals(excludePlayer)) return;
            if(!lcManager.isOnLunarClient(member)) return;

            LCPacket packet = packets.get(member.getWorld().getEnvironment());
            if(packet != null) {
                LunarClientAPI.getInstance().sendPacket(member, packet);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoinFaction(PlayerJoinFactionEvent event) {
        Player player = event.getFactionPlayer().getPlayer();
        if(player == null) return;

        // Update all faction members (including the new player) to see each other
        PlayerFaction faction = event.getFaction();
        LunarClientManager lcManager = Lazarus.getInstance().getLunarClientManager();
        Map<Environment, LCPacket> packets = this.createTeammatePackets(faction);

        faction.getOnlinePlayers().forEach(member -> {
            if(!lcManager.isOnLunarClient(member)) return;

            LCPacket packet = packets.get(member.getWorld().getEnvironment());
            if(packet != null) {
                LunarClientAPI.getInstance().sendPacket(member, packet);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerLeaveFaction(PlayerLeaveFactionEvent event) {
        Player player = event.getFactionPlayer().getPlayer();
        if(player == null) return;

        // Clear teammates view for the player who left
        this.clearTeammatesForPlayer(player);

        // Update remaining faction members to remove the leaving player's marker
        this.updateFactionTeammates(event.getFaction(), player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFactionDisband(FactionDisbandEvent event) {
        if(!(event.getFaction() instanceof PlayerFaction)) return;
        PlayerFaction faction = (PlayerFaction) event.getFaction();

        // Clear teammates view for all faction members
        for(Player player : faction.getOnlinePlayers()) {
            this.clearTeammatesForPlayer(player);
        }
    }

    @Override
    public void run() {
        try {
            this.sendTeamViewPackets();
        } catch(Throwable t) {
            t.printStackTrace();
        }
    }

    @Getter
    private static class PositionMap {

        private final Map<UUID, Map<String, Double>> positions;

        public PositionMap() {
            this.positions = new HashMap<>();
        }

        public void addPosition(UUID uuid, Map<String, Double> position) {
            this.positions.put(uuid, position);
        }
    }
}
