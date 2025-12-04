# Changelog

> ⚠️ **Note:** The original codebase belongs to [LazarusDevelopment](https://github.com/LazarusDevelopment/Lazarus).  
> This changelog documents my modifications to this fork.

---

## [1.2.0] - 2025-12-04

### Added
- **Lunar Client TeamView Event Handling** - TeamViewTask now listens to faction events for instant teammate marker updates
  - Teammates are instantly updated when a player joins a faction
  - Teammate markers are cleared immediately when a player leaves a faction
  - All teammate markers are cleared when a faction disbands
  - Added Lunar Client check to prevent sending packets to non-LC players

- **Faction-Based Death Messages** - New configurable death message system with faction relationship awareness
  - Players see personalized death messages based on their relationship to killer/victim
  - Self (you), teammate, enemy, and factionless formats are fully customizable
  - KoTH-specific anonymization option to hide player names during active KoTH events
  - New `AdditionalConfig` class with dedicated `additional_config.yml`

### Changed
- **Gson Version** - Downgraded from `2.10.1` to `2.8.9` for better Java 8 compatibility
- **Maven Shade Plugin** - Updated from `3.4.1` to `3.5.1`
  - Changed `createDependencyReducedPom` to `true`
  - Added `minimizeJar` set to `false`

### Fixed
- **TeamViewTask Null Safety** - Added null checks before sending Lunar Client packets

---

## [1.1.0] - 2025-12-03

### Added
- **KoTH Cap Time Management** - Enhanced cap time control with new methods
  - `setCurrentCapTime(int)` - Temporarily modify cap time for current attempt only
  - `getInitialCapTime()` - Retrieve the base cap time (now exposed via getter)
  - Pending cap time system - Time changes are queued when players are capping and applied when capzone empties
  - Improved `changeCapTime(int)` logic with proper handling of active caps

### Fixed
- **Faction Save Race Condition** - Prevented potential data corruption during plugin disable
  - Async save task is now cancelled before saving factions
  - Added 100ms delay to ensure running async tasks complete
  - Task cancellation moved to beginning of `disable()` method

- **Null Userdata Crashes** - Added null safety checks
  - `SettingsHandler.loadSettingsInventory()` - Returns early if userdata is null
  - `NotesHandler.onPlayerJoin()` - Added null check before accessing notes

---

## [1.0.0] - Initial Fork

### Code Changes

#### Staff Mode - Invisible Player Nametags
- Invisible players now have their nametags visible when a staff member is in staff mode
- Helps staff identify vanished/invisible players more easily

#### KoTH - Cap Time in Milliseconds
- Added functionality to retrieve the remaining cap time in milliseconds
- Provides more precise timing data for integrations and displays

---

## Build Configuration Updates

### Repositories Added/Updated

- **JitPack** (`https://jitpack.io`) - For GitHub-hosted dependencies
- **Spigot Repository** (`https://hub.spigotmc.org/nexus/content/repositories/snapshots/`)
- **ViaVersion Repository** (`https://repo.viaversion.com`)
- **Maven Central** (`https://repo.maven.apache.org/maven2`)
- **CodeMC** (`https://repo.codemc.io/repository/maven-public/`)
- **Lunar Client Repository** (`https://repo.lunarclient.dev`)
- **Imanity Repository** (`https://repo.imanity.dev/imanity-libraries/`)

### Dependencies Added

- **ImanitySpigot API** `2025.3.3` - Added for ImanitySpigot server support
- **ImanitySpigot PaperSpigot** `1.8.8` - Added for Paper 1.8.8 compatibility

### Dependencies Updated

| Dependency | Old Version | New Version | Notes |
|------------|-------------|-------------|-------|
| Gson | (unknown) | `2.8.9` | Java 8 compatible version |
| MongoDB Driver | (unknown) | `3.12.14` (sync) | Java 8 compatible version |
| Lombok | (unknown) | `1.18.30` | Updated to latest |
| LuckPerms API | (unknown) | `5.4` | Updated to latest |
| VaultAPI | (broken) | `1.7.1` via JitPack | Fixed source repository |

### Build Configuration

- **Maven Shade Plugin** `3.5.1` configured with:
  - Gson shading to `me.qiooip.lazarus.shade.gson`
  - MongoDB shading to `me.qiooip.lazarus.shade.mongodb`
  - BSON shading to `me.qiooip.lazarus.shade.bson`
  - META-INF signature file exclusions

- **Maven Compiler Plugin** `3.8.1` configured for Java 8

### System Dependencies (libs/ folder)

The following JARs are required in the `libs/` folder:
- `spigot-1.7.10-SNAPSHOT-b1657.jar`
- `ProtocolSupport.jar`
- `ViaVersion-4.8.1.jar`

---

## Original Project

For the original project and its full history, please visit:  
[https://github.com/LazarusDevelopment/Lazarus](https://github.com/LazarusDevelopment/Lazarus)
