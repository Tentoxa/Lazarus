package me.qiooip.lazarus.games.koth;

import lombok.Getter;
import me.qiooip.lazarus.Lazarus;
import me.qiooip.lazarus.config.Config;
import me.qiooip.lazarus.config.Language;
import me.qiooip.lazarus.factions.FactionsManager;
import me.qiooip.lazarus.factions.type.PlayerFaction;
import me.qiooip.lazarus.games.Capzone;
import me.qiooip.lazarus.games.Placeholder;
import me.qiooip.lazarus.games.koth.event.KothCappedEvent;
import me.qiooip.lazarus.games.koth.event.KothKnockedEvent;
import me.qiooip.lazarus.games.koth.event.KothStartedCappingEvent;
import me.qiooip.lazarus.utils.Messages;
import me.qiooip.lazarus.utils.StringUtils;
import me.qiooip.lazarus.utils.Tasks;
import org.bukkit.entity.Player;

public class RunningKoth {

    @Getter private final KothData kothData;
    @Getter private final Capzone capzone;

    @Getter private int initialCapTime;
    private int pendingCapTime = -1; // Time change waiting to be applied when capzone is empty

    private final long startTime;
    private long capperChange;

    private long antiSpamDelay;

    RunningKoth(KothData kothData, int time) {
        this.kothData = kothData;
        this.capzone = new Capzone(kothData.getCuboid(), time);

        this.initialCapTime = time;
        this.startTime = this.capperChange = System.currentTimeMillis();

        this.capzone.getCuboid().getPlayers().forEach(this::onPlayerEnterCapzone);
    }

    /**
     * Changes the base cap time for this KOTH permanently. This affects what time
     * the KOTH resets to when a player is knocked. If no players are in the capzone,
     * the time is applied immediately. Otherwise, it will be applied once all players
     * leave the capzone to avoid interrupting active cap progress.
     * Use this for admin commands like /koth settime.
     *
     * @param newTime the new base cap time in seconds
     */
    public void changeCapTime(int newTime) {
        this.initialCapTime = newTime;

        if(this.capzone.hasNoPlayers()) {
            // Apply immediately when no one is capping
            this.capzone.setTime(newTime);
            this.pendingCapTime = -1;
        } else {
            // Queue the time change to apply when capzone becomes empty
            this.pendingCapTime = newTime;
        }
    }

    /**
     * Temporarily sets the current cap time without changing the base initialCapTime.
     * When the player is knocked, the time will reset to initialCapTime, not this value.
     * Use this for temporary boosts/reductions that should only affect the current cap attempt.
     *
     * @param newTime the temporary cap time in seconds for this cap attempt
     */
    public void setCurrentCapTime(int newTime) {
        this.capzone.setTime(newTime);
    }

    /**
     * Checks if there's a pending cap time change and applies it.
     * Called when the capzone becomes empty (player knocked or left).
     */
    private void applyPendingCapTime() {
        if(this.pendingCapTime > 0 && this.capzone.hasNoPlayers()) {
            this.capzone.setTime(this.pendingCapTime);
            this.initialCapTime = this.pendingCapTime;
            this.pendingCapTime = -1;
        }
    }

    private void handleWin() {
        Player capper = this.capzone.getCapper();
        PlayerFaction faction = FactionsManager.getInstance().getPlayerFaction(capper);

        String uptime = StringUtils.formatMillis(System.currentTimeMillis() - this.startTime);

        if(faction == null) {
            Language.KOTH_CAPPED_NO_FACTION.forEach(line -> Messages.sendMessage(Placeholder.RunningKothReplacer
            .parse(this, line.replace("<uptime>", uptime))));
        } else {
            Language.KOTH_CAPPED_WITH_FACTION.forEach(line -> Messages.sendMessage(Placeholder.RunningKothReplacer
            .parse(this, line
                .replace("<faction>", faction.getDisplayName(capper))
                .replace("<uptime>", uptime))));
        }

        Tasks.sync(() -> {
            new KothCappedEvent(this.kothData, capper);
            Lazarus.getInstance().getKothManager().stopKoth(this);
        });
    }

    private void handlePlayerStartedCapping(Player player) {
        if(player == null) return;

        player.sendMessage(Language.KOTH_PREFIX + Placeholder.RunningKothReplacer
            .parse(this, Language.KOTH_YOU_STARTED_CAPPING));

        if((this.antiSpamDelay - System.currentTimeMillis()) <= 0) {
            Messages.sendMessageWithoutPlayer(player, Language.KOTH_PREFIX + Placeholder.RunningKothReplacer
                .parse(this, Language.KOTH_SOMEONE_STARTED_CAPPING));

            this.antiSpamDelay = System.currentTimeMillis() + Config.KOTH_ANTI_SPAM_MESSAGE_DELAY;
        }

        new KothStartedCappingEvent(this, player);
    }

    private void handlePlayerKnocked(Player player) {
        this.capzone.setTime(this.initialCapTime);

        long capTimeMillis = System.currentTimeMillis() - this.capperChange;
        this.capperChange = System.currentTimeMillis();

        if((this.antiSpamDelay - System.currentTimeMillis()) <= 0) {
            Messages.sendMessage(Language.KOTH_PREFIX + Placeholder.RunningKothReplacer
                .parse(this, Language.KOTH_KNOCKED));

            this.antiSpamDelay = System.currentTimeMillis() + Config.KOTH_ANTI_SPAM_MESSAGE_DELAY;
        }

        new KothKnockedEvent(this, player, capTimeMillis);
    }

    void onPlayerEnterCapzone(Player player) {
        // Apply any pending cap time change before a new player starts capping
        this.applyPendingCapTime();
        if(Lazarus.getInstance().getStaffModeManager().isInStaffModeOrVanished(player)) return;

        if(this.capzone.hasNoPlayers()) {
            this.handlePlayerStartedCapping(player);
        }

        this.capzone.addPlayer(player);
    }

    void onPlayerLeaveCapzone(Player player) {
        if(!this.capzone.getPlayers().contains(player.getName())) return;

        if(this.capzone.isCapper(player)) {
            this.handlePlayerKnocked(player);

            if(this.capzone.getPlayers().size() > 1) {
                this.handlePlayerStartedCapping(this.capzone.getNextCapper());
            }
        }

        this.capzone.removePlayer(player);
    }

    void tick() {
        if(this.capzone.hasNoPlayers()) {
            int timePassed = (int) ((System.currentTimeMillis() - this.capperChange) / 1000);
            int nobodyCappingInterval = Config.KOTH_NOBODY_CAPPING_MESSAGE_INTERVAL;

            if(nobodyCappingInterval != 0 && timePassed > 0 && timePassed % nobodyCappingInterval == 0) {
                Messages.sendMessage(Language.KOTH_PREFIX + Placeholder
                    .RunningKothReplacer.parse(this, Language.KOTH_NO_ONE_CAPPING));
            }

            return;
        }

        if(this.capzone.decreaseTime() <= 0) {
            this.handleWin();
            return;
        }

        int cappingInterval = Config.KOTH_CAPPING_MESSAGE_INTERVAL;

        if(cappingInterval != 0 && this.capzone.getTime() % cappingInterval == 0) {
            Player capper = this.capzone.getCapper();

            capper.sendMessage(Language.KOTH_PREFIX + Placeholder
                .RunningKothReplacer.parse(this, Language.KOTH_YOU_ARE_CAPPING));

            Messages.sendMessageWithoutPlayer(capper, Language.KOTH_PREFIX + Placeholder
                .RunningKothReplacer.parse(this, Language.KOTH_SOMEONE_IS_CAPPING));
        }
    }

    public String getScoreboardEntry() {
        return this.capzone.getTimeLeft();
    }

    public long getTimeLeftMillis() {
        return this.capzone.getTimeLeftMillis();
    }
}
