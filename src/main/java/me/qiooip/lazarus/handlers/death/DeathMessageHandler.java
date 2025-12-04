package me.qiooip.lazarus.handlers.death;

import me.qiooip.lazarus.Lazarus;
import me.qiooip.lazarus.config.AdditionalConfig;
import me.qiooip.lazarus.config.Language;
import me.qiooip.lazarus.factions.Faction;
import me.qiooip.lazarus.factions.FactionsManager;
import me.qiooip.lazarus.factions.claim.ClaimManager;
import me.qiooip.lazarus.factions.type.KothFaction;
import me.qiooip.lazarus.factions.type.PlayerFaction;
import me.qiooip.lazarus.games.koth.RunningKoth;
import me.qiooip.lazarus.handlers.manager.Handler;
import me.qiooip.lazarus.userdata.Userdata;
import me.qiooip.lazarus.userdata.UserdataManager;
import me.qiooip.lazarus.utils.StringUtils;
import me.qiooip.lazarus.utils.nms.NmsUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

public class DeathMessageHandler extends Handler implements Listener {

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Player killer = player.getKiller();

        // Check if we should use faction-based name formats
        if (AdditionalConfig.FACTION_NAME_FORMATS_ENABLED) {
            this.handleFactionBasedDeathMessage(event, player, killer);
            return;
        }

        // Use original Lazarus death message system
        String deathMessage = this.getDeathMessage(player);
        event.setDeathMessage(deathMessage);

        UserdataManager userdataManager = Lazarus.getInstance().getUserdataManager();

        Bukkit.getOnlinePlayers().forEach(online -> {
            Userdata userdata = userdataManager.getUserdata(online);
            boolean deathMessages = userdata.getSettings().isDeathMessages();

            if(player == online || player.getKiller() == online || deathMessages) {
                online.sendMessage(deathMessage);
            }
        });
    }

    private void handleFactionBasedDeathMessage(PlayerDeathEvent event, Player victim, Player killer) {
        // Check if death is in a running KoTH (not just on KoTH claim)
        Faction factionAt = ClaimManager.getInstance().getFactionAt(victim.getLocation());
        boolean isInRunningKoth = this.isInRunningKoth(factionAt);

        // Get factions of killer and victim
        PlayerFaction killerFaction = killer != null ? FactionsManager.getInstance().getPlayerFaction(killer) : null;
        PlayerFaction victimFaction = FactionsManager.getInstance().getPlayerFaction(victim);

        UserdataManager userdataManager = Lazarus.getInstance().getUserdataManager();

        // Generate a base death message for the event (used by other handlers like DynamicEventHandler)
        // This uses the original format so lastDeaths/lastKills tracking works correctly
        String baseDeathMessage = this.getDeathMessage(victim);
        event.setDeathMessage(baseDeathMessage);

        // Send personalized messages to all online players
        for (Player online : Bukkit.getOnlinePlayers()) {
            Userdata userdata = userdataManager.getUserdata(online);
            boolean deathMessages = userdata.getSettings().isDeathMessages();

            // Always show to killer, victim, or players with death messages enabled
            if (online != victim && online != killer && !deathMessages) continue;

            PlayerFaction viewerFaction = FactionsManager.getInstance().getPlayerFaction(online);
            
            // Generate death message with faction-based name formatting for this viewer
            String message = this.getDeathMessageForViewer(victim, killer, viewerFaction, 
                                                           victimFaction, killerFaction, isInRunningKoth, online);

            if (message != null) {
                online.sendMessage(message);
            }
        }
        // Note: The MONITOR priority cancelDeathMessage handler will set deathMessage to null
        // to prevent Minecraft's default broadcast. We keep the message set for other handlers
        // like DynamicEventHandler that need it for stats tracking.
    }

    private boolean isInRunningKoth(Faction factionAt) {
        if (!(factionAt instanceof KothFaction)) {
            return false;
        }

        // Check if this KoTH faction corresponds to a running KoTH
        KothFaction kothFaction = (KothFaction) factionAt;
        for (RunningKoth runningKoth : Lazarus.getInstance().getKothManager().getRunningKoths()) {
            if (runningKoth.getKothData().getFactionId().equals(kothFaction.getId())) {
                return true;
            }
        }
        return false;
    }

    private String getDeathMessageForViewer(Player victim, Player killer, 
                                             PlayerFaction viewerFaction,
                                             PlayerFaction victimFaction, 
                                             PlayerFaction killerFaction,
                                             boolean isInKoth,
                                             Player viewer) {
        // Get the base death message (same logic as original getDeathMessage but with custom name formats)
        DamageCause cause = victim.getLastDamageCause() == null
            ? DamageCause.CUSTOM
            : victim.getLastDamageCause().getCause();

        String playerName = this.getFormattedPlayerName(victim, victimFaction, viewerFaction, isInKoth, viewer);
        String killerName = killer != null ? this.getFormattedKillerName(killer, killerFaction, viewerFaction, isInKoth, viewer) : null;

        String message = "";

        if(victim.getLastDamageCause() instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) victim.getLastDamageCause();

            switch(cause) {
                case ENTITY_ATTACK: {
                    message = this.getEntityAttackDeathMessageForViewer(victim, damageEvent, playerName, killerName, 
                                                                         killerFaction, viewerFaction, isInKoth, viewer);
                    break;
                }
                case PROJECTILE: {
                    message = this.getProjectileDeathMessageForViewer(victim, damageEvent, playerName, 
                                                                       viewerFaction, isInKoth, viewer);
                    break;
                }
                case ENTITY_EXPLOSION: {
                    message = this.getEntityExplosionDeathMessage(victim, damageEvent, playerName);
                    break;
                }
                case FALLING_BLOCK: {
                    message = Language.DEATHMESSAGE_REASON_FALLING_BLOCK.replace("<player>", playerName);
                    break;
                }
                case LIGHTNING: {
                    message = Language.DEATHMESSAGE_REASON_LIGHTNING.replace("<player>", playerName);
                    break;
                }
                case FALL: {
                    message = Language.DEATHMESSAGE_REASON_FALL.replace("<player>", playerName);
                    break;
                }
                default: {
                    message = Language.DEATHMESSAGE_REASON_CUSTOM.replace("<player>", playerName);
                    break;
                }
            }
        } else {
            switch(cause) {
                case BLOCK_EXPLOSION: {
                    message = Language.DEATHMESSAGE_REASON_BLOCK_EXPLOSION.replace("<player>", playerName);
                    break;
                }
                case CONTACT: {
                    message = Language.DEATHMESSAGE_REASON_CONTACT.replace("<player>", playerName);
                    break;
                }
                case DROWNING: {
                    message = Language.DEATHMESSAGE_REASON_DROWNING.replace("<player>", playerName);
                    break;
                }
                case FIRE: {
                    message = Language.DEATHMESSAGE_REASON_FIRE.replace("<player>", playerName);
                    break;
                }
                case FIRE_TICK: {
                    message = Language.DEATHMESSAGE_REASON_FIRE_TICK.replace("<player>", playerName);
                    break;
                }
                case LAVA: {
                    message = Language.DEATHMESSAGE_REASON_LAVA.replace("<player>", playerName);
                    break;
                }
                case MAGIC: {
                    message = Language.DEATHMESSAGE_REASON_MAGIC.replace("<player>", playerName);
                    break;
                }
                case MELTING: {
                    message = Language.DEATHMESSAGE_REASON_MELTING.replace("<player>", playerName);
                    break;
                }
                case POISON: {
                    message = Language.DEATHMESSAGE_REASON_POISON.replace("<player>", playerName);
                    break;
                }
                case STARVATION: {
                    message = Language.DEATHMESSAGE_REASON_STARVATION.replace("<player>", playerName);
                    break;
                }
                case SUFFOCATION: {
                    message = Language.DEATHMESSAGE_REASON_SUFFOCATION.replace("<player>", playerName);
                    break;
                }
                case SUICIDE: {
                    message = Language.DEATHMESSAGE_REASON_SUICIDE.replace("<player>", playerName);
                    break;
                }
                case THORNS: {
                    message = Language.DEATHMESSAGE_REASON_THORNS.replace("<player>", playerName);
                    break;
                }
                case WITHER: {
                    message = Language.DEATHMESSAGE_REASON_WITHER.replace("<player>", playerName);
                    break;
                }
                case FALL: {
                    message = this.getFallDeathMessageForViewer(victim, playerName, killerName);
                    break;
                }
                case VOID: {
                    message = this.getVoidDeathMessageForViewer(victim, playerName, killerName);
                    break;
                }
                default: {
                    message = Language.DEATHMESSAGE_REASON_CUSTOM.replace("<player>", playerName);
                    break;
                }
            }
        }

        return message;
    }

    private String getFormattedPlayerName(Player player, PlayerFaction playerFaction, 
                                           PlayerFaction viewerFaction, boolean isInKoth, Player viewer) {
        Userdata data = Lazarus.getInstance().getUserdataManager().getUserdata(player);
        String kills = String.valueOf(data.getKills());
        String factionName = playerFaction != null ? playerFaction.getName() : "None";
        
        // Determine name (anonymize for KoTH if enabled)
        String name = player.getName();
        if (isInKoth && AdditionalConfig.KOTH_ANONYMIZE_PLAYER_NAMES) {
            name = "Anonymous";
        }

        String format;
        // For the VICTIM (player who died): 
        // - If viewer IS the victim (self) -> use SELF format (red underlined)
        // - If victim is viewer's teammate -> use TEAMMATE format (green underlined)
        // - Otherwise -> use ENEMY/FACTIONLESS format
        boolean isSelf = player.getUniqueId().equals(viewer.getUniqueId());
        boolean isTeammate = !isSelf && viewerFaction != null && playerFaction != null 
                             && viewerFaction.getId().equals(playerFaction.getId());
        
        if (isSelf) {
            // Viewer IS the victim - show in SELF format (red underlined)
            format = AdditionalConfig.SELF_PLAYER_FORMAT;
        } else if (isTeammate) {
            // Victim is viewer's teammate (but not the viewer themselves)
            format = AdditionalConfig.TEAMMATE_PLAYER_FORMAT;
        } else if (playerFaction == null) {
            // Victim has no faction
            format = AdditionalConfig.FACTIONLESS_PLAYER_FORMAT;
        } else {
            // Victim is enemy
            format = AdditionalConfig.ENEMY_PLAYER_FORMAT;
        }

        return format
                .replace("<player>", name)
                .replace("<kills>", kills)
                .replace("<faction>", factionName);
    }

    private String getFormattedKillerName(Player killer, PlayerFaction killerFaction, 
                                           PlayerFaction viewerFaction, boolean isInKoth, Player viewer) {
        Userdata data = Lazarus.getInstance().getUserdataManager().getUserdata(killer);
        String kills = data == null ? "?" : String.valueOf(data.getKills() + 1);
        String factionName = killerFaction != null ? killerFaction.getName() : "None";
        
        // Determine name (anonymize for KoTH if enabled)
        String name = killer.getName();
        if (isInKoth && AdditionalConfig.KOTH_ANONYMIZE_PLAYER_NAMES) {
            name = "Anonymous";
        }

        String format;
        // For the KILLER:
        // - If viewer IS the killer (self) -> use SELF format (green underlined)
        // - If killer is viewer's teammate -> use TEAMMATE format (green underlined)
        // - Otherwise -> use ENEMY/FACTIONLESS format
        boolean isSelf = killer.getUniqueId().equals(viewer.getUniqueId());
        boolean isTeammate = !isSelf && viewerFaction != null && killerFaction != null 
                             && viewerFaction.getId().equals(killerFaction.getId());
        
        if (isSelf) {
            // Viewer IS the killer - show in SELF format (green underlined)
            format = AdditionalConfig.SELF_KILLER_FORMAT;
        } else if (isTeammate) {
            // Killer is viewer's teammate
            format = AdditionalConfig.TEAMMATE_KILLER_FORMAT;
        } else if (killerFaction == null) {
            // Killer has no faction
            format = AdditionalConfig.FACTIONLESS_KILLER_FORMAT;
        } else {
            // Killer is enemy (different faction)
            format = AdditionalConfig.ENEMY_KILLER_FORMAT;
        }

        return format
                .replace("<killer>", name)
                .replace("<kills>", kills)
                .replace("<faction>", factionName);
    }

    private String getEntityAttackDeathMessageForViewer(Player player, EntityDamageByEntityEvent damageEvent,
                                                         String playerName, String preformattedKillerName,
                                                         PlayerFaction killerFaction, PlayerFaction viewerFaction,
                                                         boolean isInKoth, Player viewer) {
        Entity damager = damageEvent.getDamager();

        if(damager instanceof Player) {
            Player playerDamager = (Player) damager;
            ItemStack handItem = playerDamager.getItemInHand();

            // Use preformatted killer name if available, otherwise format it
            String killerName = preformattedKillerName != null ? preformattedKillerName 
                : this.getFormattedKillerName(playerDamager, killerFaction, viewerFaction, isInKoth, viewer);

            if(handItem != null && handItem.getType() != Material.AIR) {
                return Language.DEATHMESSAGE_REASON_ENTITY_ATTACK_PLAYER_ITEM
                    .replace("<player>", playerName)
                    .replace("<killer>", killerName)
                    .replace("<item>", NmsUtils.getInstance().getItemName(handItem));
            } else {
                return Language.DEATHMESSAGE_REASON_ENTITY_ATTACK_PLAYER_NO_ITEM
                    .replace("<player>", playerName)
                    .replace("<killer>", killerName);
            }
        } else {
            return Language.DEATHMESSAGE_REASON_ENTITY_ATTACK_ENTITY
                .replace("<player>", playerName)
                .replace("<entity>", StringUtils.getEntityName(damager.getType().name()));
        }
    }

    private String getProjectileDeathMessageForViewer(Player player, EntityDamageByEntityEvent damageEvent,
                                                       String playerName, PlayerFaction viewerFaction,
                                                       boolean isInKoth, Player viewer) {
        Projectile projectile = (Projectile) damageEvent.getDamager();

        if(projectile.getShooter() instanceof Player) {
            Player playerShooter = (Player) projectile.getShooter();
            PlayerFaction shooterFaction = FactionsManager.getInstance().getPlayerFaction(playerShooter);
            String killerName = this.getFormattedKillerName(playerShooter, shooterFaction, viewerFaction, isInKoth, viewer);
            ItemStack handItem = playerShooter.getItemInHand();

            if(handItem != null && handItem.getType() != Material.AIR) {
                return Language.DEATHMESSAGE_REASON_PROJECTILE_PLAYER_ITEM
                    .replace("<player>", playerName)
                    .replace("<killer>", killerName)
                    .replace("<item>", NmsUtils.getInstance().getItemName(handItem));
            } else {
                return Language.DEATHMESSAGE_REASON_PROJECTILE_PLAYER_NO_ITEM
                    .replace("<player>", playerName)
                    .replace("<killer>", killerName);
            }
        } else {
            Entity entityShooter = (Entity) projectile.getShooter();

            return Language.DEATHMESSAGE_REASON_PROJECTILE_ENTITY
                .replace("<player>", playerName)
                .replace("<entity>", StringUtils.getEntityName(entityShooter.getType().name()));
        }
    }

    private String getEntityExplosionDeathMessage(Player player, EntityDamageByEntityEvent damageEvent, String playerName) {
        Entity damager = damageEvent.getDamager();

        if(damager instanceof TNTPrimed) {
            return Language.DEATHMESSAGE_REASON_BLOCK_EXPLOSION
                .replace("<player>", playerName);
        } else {
            return Language.DEATHMESSAGE_REASON_ENTITY_EXPLOSION
                .replace("<player>", playerName);
        }
    }

    private String getFallDeathMessageForViewer(Player player, String playerName, String killerName) {
        Player killer = player.getKiller();

        if(killer != null && player != killer && killerName != null) {
            return Language.DEATHMESSAGE_REASON_FALL_KILLER
                .replace("<player>", playerName)
                .replace("<killer>", killerName);
        } else {
            return Language.DEATHMESSAGE_REASON_FALL
                .replace("<player>", playerName);
        }
    }

    private String getVoidDeathMessageForViewer(Player player, String playerName, String killerName) {
        Player killer = player.getKiller();

        if(killer != null && player != killer && killerName != null) {
            return Language.DEATHMESSAGE_REASON_VOID_KILLER
                .replace("<player>", playerName)
                .replace("<killer>", killerName);
        } else {
            return Language.DEATHMESSAGE_REASON_VOID
                .replace("<player>", playerName);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void cancelDeathMessage(PlayerDeathEvent event) {
        event.setDeathMessage(null);
    }

    private String getDeathMessage(Player player) {
        DamageCause cause = player.getLastDamageCause() == null
            ? DamageCause.CUSTOM
            : player.getLastDamageCause().getCause();

        String message = "";

        if(player.getLastDamageCause() instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) player.getLastDamageCause();

            switch(cause) {
                case ENTITY_ATTACK: {
                    message = this.getEntityAttackDeathMessage(player, damageEvent);
                    break;
                }
                case PROJECTILE: {
                    message = this.getProjectileDeathMessage(player, damageEvent);
                    break;
                }
                case ENTITY_EXPLOSION: {
                    message = this.getEntityExplosionDeathMessage(player, damageEvent);
                    break;
                }
                case FALLING_BLOCK: {
                    message = Language.DEATHMESSAGE_REASON_FALLING_BLOCK.replace("<player>", this.getPlayerName(player));
                    break;
                }
                case LIGHTNING: {
                    message = Language.DEATHMESSAGE_REASON_LIGHTNING.replace("<player>", this.getPlayerName(player));
                    break;
                }
                case FALL: {
                    message = Language.DEATHMESSAGE_REASON_FALL.replace("<player>", this.getPlayerName(player));
                    break;
                }
                default: {
                    message = Language.DEATHMESSAGE_REASON_CUSTOM.replace("<player>", this.getPlayerName(player));
                    break;
                }
            }
        } else {
            switch(cause) {
                case BLOCK_EXPLOSION: {
                    message = Language.DEATHMESSAGE_REASON_BLOCK_EXPLOSION.replace("<player>", this.getPlayerName(player));
                    break;
                }
                case CONTACT: {
                    message = Language.DEATHMESSAGE_REASON_CONTACT.replace("<player>", this.getPlayerName(player));
                    break;
                }
                case DROWNING: {
                    message = Language.DEATHMESSAGE_REASON_DROWNING.replace("<player>", this.getPlayerName(player));
                    break;
                }
                case FIRE: {
                    message = Language.DEATHMESSAGE_REASON_FIRE.replace("<player>", this.getPlayerName(player));
                    break;
                }
                case FIRE_TICK: {
                    message = Language.DEATHMESSAGE_REASON_FIRE_TICK.replace("<player>", this.getPlayerName(player));
                    break;
                }
                case LAVA: {
                    message = Language.DEATHMESSAGE_REASON_LAVA.replace("<player>", this.getPlayerName(player));
                    break;
                }
                case MAGIC: {
                    message = Language.DEATHMESSAGE_REASON_MAGIC.replace("<player>", this.getPlayerName(player));
                    break;
                }
                case MELTING: {
                    message = Language.DEATHMESSAGE_REASON_MELTING.replace("<player>", this.getPlayerName(player));
                    break;
                }
                case POISON: {
                    message = Language.DEATHMESSAGE_REASON_POISON.replace("<player>", this.getPlayerName(player));
                    break;
                }
                case STARVATION: {
                    message = Language.DEATHMESSAGE_REASON_STARVATION.replace("<player>", this.getPlayerName(player));
                    break;
                }
                case SUFFOCATION: {
                    message = Language.DEATHMESSAGE_REASON_SUFFOCATION.replace("<player>", this.getPlayerName(player));
                    break;
                }
                case SUICIDE: {
                    message = Language.DEATHMESSAGE_REASON_SUICIDE.replace("<player>", this.getPlayerName(player));
                    break;
                }
                case THORNS: {
                    message = Language.DEATHMESSAGE_REASON_THORNS.replace("<player>", this.getPlayerName(player));
                    break;
                }
                case WITHER: {
                    message = Language.DEATHMESSAGE_REASON_WITHER.replace("<player>", this.getPlayerName(player));
                    break;
                }
                case FALL: {
                    message = this.getFallDeathMessage(player);
                    break;
                }
                case VOID: {
                    message = this.getVoidDeathMessage(player);
                    break;
                }
                default: {
                    message = Language.DEATHMESSAGE_REASON_CUSTOM.replace("<player>", this.getPlayerName(player));
                    break;
                }
            }
        }

        return message;
    }

    public String getPlayerName(OfflinePlayer player) {
        Userdata data = Lazarus.getInstance().getUserdataManager().getUserdata(player);

        return Language.DEATHMESSAGE_PLAYER_NAME_FORMAT
            .replace("<player>", player.getName())
            .replace("<kills>", String.valueOf(data.getKills()));
    }

    public String getKillerName(Player killer) {
        Userdata data = Lazarus.getInstance().getUserdataManager().getUserdata(killer);

        return Language.DEATHMESSAGE_KILLER_NAME_FORMAT
            .replace("<killer>", killer.getName())
            .replace("<kills>", data == null ? "?" : String.valueOf(data.getKills() + 1));
    }

    private String getEntityAttackDeathMessage(Player player, EntityDamageByEntityEvent damageEvent) {
        Entity damager = damageEvent.getDamager();

        if(damager instanceof Player) {
            Player playerDamager = (Player) damager;
            ItemStack handItem = playerDamager.getItemInHand();

            if(handItem != null && handItem.getType() != Material.AIR) {
                return Language.DEATHMESSAGE_REASON_ENTITY_ATTACK_PLAYER_ITEM
                    .replace("<player>", this.getPlayerName(player))
                    .replace("<killer>", this.getKillerName(playerDamager))
                    .replace("<item>", NmsUtils.getInstance().getItemName(handItem));
            } else {
                return Language.DEATHMESSAGE_REASON_ENTITY_ATTACK_PLAYER_NO_ITEM
                    .replace("<player>", this.getPlayerName(player))
                    .replace("<killer>", this.getKillerName(playerDamager));
            }
        } else {
            return Language.DEATHMESSAGE_REASON_ENTITY_ATTACK_ENTITY
                .replace("<player>", this.getPlayerName(player))
                .replace("<entity>", StringUtils.getEntityName(damager.getType().name()));
        }
    }

    private String getProjectileDeathMessage(Player player, EntityDamageByEntityEvent damageEvent) {
        Projectile projectile = (Projectile) damageEvent.getDamager();

        if(projectile.getShooter() instanceof Player) {
            Player playerShooter = (Player) projectile.getShooter();
            ItemStack handItem = playerShooter.getItemInHand();

            if(handItem != null && handItem.getType() != Material.AIR) {
                return Language.DEATHMESSAGE_REASON_PROJECTILE_PLAYER_ITEM
                    .replace("<player>", this.getPlayerName(player))
                    .replace("<killer>", this.getKillerName(playerShooter))
                    .replace("<item>", NmsUtils.getInstance().getItemName(handItem));
            } else {
                return Language.DEATHMESSAGE_REASON_PROJECTILE_PLAYER_NO_ITEM
                    .replace("<player>", this.getPlayerName(player))
                    .replace("<killer>", this.getKillerName(playerShooter));
            }
        } else {
            Entity entityShooter = (Entity) projectile.getShooter();

            return Language.DEATHMESSAGE_REASON_PROJECTILE_ENTITY
                .replace("<player>", this.getPlayerName(player))
                .replace("<entity>", StringUtils.getEntityName(entityShooter.getType().name()));
        }
    }

    private String getEntityExplosionDeathMessage(Player player, EntityDamageByEntityEvent damageEvent) {
        Entity damager = damageEvent.getDamager();

        if(damager instanceof TNTPrimed) {
            return Language.DEATHMESSAGE_REASON_BLOCK_EXPLOSION
                .replace("<player>", this.getPlayerName(player));
        } else {
            return Language.DEATHMESSAGE_REASON_ENTITY_EXPLOSION
                .replace("<player>", this.getPlayerName(player));
        }
    }

    private String getFallDeathMessage(Player player) {
        Player killer = player.getKiller();

        if(killer != null && player != killer) {
            return Language.DEATHMESSAGE_REASON_FALL_KILLER
                .replace("<player>", this.getPlayerName(player))
                .replace("<killer>", this.getKillerName(killer));
        } else {
            return Language.DEATHMESSAGE_REASON_FALL
            .replace("<player>", this.getPlayerName(player));
        }
    }

    private String getVoidDeathMessage(Player player) {
        Player killer = player.getKiller();

        if(killer != null && player != killer) {
            return Language.DEATHMESSAGE_REASON_VOID_KILLER
                .replace("<player>", this.getPlayerName(player))
                .replace("<killer>", this.getKillerName(killer));
        } else {
            return Language.DEATHMESSAGE_REASON_VOID
                .replace("<player>", this.getPlayerName(player));
        }
    }
}
