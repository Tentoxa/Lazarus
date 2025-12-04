package me.qiooip.lazarus.config;

import me.qiooip.lazarus.Lazarus;

public class AdditionalConfig {

    // Faction Name Formats
    public static boolean FACTION_NAME_FORMATS_ENABLED;
    public static String SELF_PLAYER_FORMAT;
    public static String SELF_KILLER_FORMAT;
    public static String TEAMMATE_PLAYER_FORMAT;
    public static String TEAMMATE_KILLER_FORMAT;
    public static String ENEMY_PLAYER_FORMAT;
    public static String ENEMY_KILLER_FORMAT;
    public static String FACTIONLESS_PLAYER_FORMAT;
    public static String FACTIONLESS_KILLER_FORMAT;

    // KoTH Overwrites
    public static boolean KOTH_ANONYMIZE_PLAYER_NAMES;

    public AdditionalConfig() {
        ConfigFile config = Lazarus.getInstance().getAdditionalConfigFile();

        // Faction Name Formats
        FACTION_NAME_FORMATS_ENABLED = config.getBoolean("FACTION_NAME_FORMATS.ENABLED");
        SELF_PLAYER_FORMAT = config.getString("FACTION_NAME_FORMATS.SELF_PLAYER_FORMAT");
        SELF_KILLER_FORMAT = config.getString("FACTION_NAME_FORMATS.SELF_KILLER_FORMAT");
        TEAMMATE_PLAYER_FORMAT = config.getString("FACTION_NAME_FORMATS.TEAMMATE_PLAYER_FORMAT");
        TEAMMATE_KILLER_FORMAT = config.getString("FACTION_NAME_FORMATS.TEAMMATE_KILLER_FORMAT");
        ENEMY_PLAYER_FORMAT = config.getString("FACTION_NAME_FORMATS.ENEMY_PLAYER_FORMAT");
        ENEMY_KILLER_FORMAT = config.getString("FACTION_NAME_FORMATS.ENEMY_KILLER_FORMAT");
        FACTIONLESS_PLAYER_FORMAT = config.getString("FACTION_NAME_FORMATS.FACTIONLESS_PLAYER_FORMAT");
        FACTIONLESS_KILLER_FORMAT = config.getString("FACTION_NAME_FORMATS.FACTIONLESS_KILLER_FORMAT");

        // KoTH Overwrites
        KOTH_ANONYMIZE_PLAYER_NAMES = config.getBoolean("FACTION_NAME_FORMATS.KOTH_OVERWRITES.ANONYMIZE_PLAYER_NAMES");
    }
}
