# Changelog

> ⚠️ **Note:** The original codebase belongs to [LazarusDevelopment](https://github.com/LazarusDevelopment/Lazarus).  
> This changelog documents my modifications to this fork.

---

## [Fork] - Code Changes

These are the only code modifications I made to the original source:

### Staff Mode - Invisible Player Nametags
- Invisible players now have their nametags visible when a staff member is in staff mode
- Helps staff identify vanished/invisible players more easily

### KoTH - Cap Time in Milliseconds
- Added functionality to retrieve the remaining cap time in milliseconds
- Provides more precise timing data for integrations and displays

---

## [Fork] - Build Configuration Updates

### Repositories Added/Updated

- **JitPack** (`https://jitpack.io`) - For GitHub-hosted dependencies
- **Spigot Repository** (`https://hub.spigotmc.org/nexus/content/repositories/snapshots/`)
- **ViaVersion Repository** (`https://repo.viaversion.com`)
- **Maven Central** (`https://repo.maven.apache.org/maven2`)
- **CodeMC** (`https://repo.codemc.io/repository/maven-public/`)
- **Lunar Client Repository** (`https://repo.lunarclient.dev`)
- **Imanity Repository** (`https://repo.imanity.dev/imanity-libraries/`) - *NEW*

### Dependencies Added

- **ImanitySpigot API** `2025.3.3` - Added for ImanitySpigot server support
- **ImanitySpigot PaperSpigot** `1.8.8` - Added for Paper 1.8.8 compatibility

### Dependencies Updated

| Dependency | Old Version | New Version | Notes |
|------------|-------------|-------------|-------|
| Gson | (unknown) | `2.10.1` | Updated to latest stable |
| MongoDB Driver | (unknown) | `3.12.14` (sync) | Java 8 compatible version |
| Lombok | (unknown) | `1.18.30` | Updated to latest |
| LuckPerms API | (unknown) | `5.4` | Updated to latest |
| VaultAPI | (broken) | `1.7.1` via JitPack | Fixed source repository |

### Build Configuration

- **Maven Shade Plugin** `3.4.1` configured with:
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
