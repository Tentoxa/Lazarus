# Lazarus HCF Core - Build-Ready Fork

> ⚠️ **DISCLAIMER: This is NOT my project!**  
> This is a fork of the original [Lazarus HCF Core](https://github.com/LazarusDevelopment/Lazarus) by **LazarusDevelopment**.  
> All credits for the original code go to the original authors and contributors.

---

## About This Fork

This repository is a **build-ready fork** of the original Lazarus HCF Core. The only changes made are updates to the `pom.xml` file to fix dependency issues and make the project compile successfully with modern Maven repositories.

**No code changes have been made to the actual plugin logic.**

---

## Original Project

- **Original Repository:** [https://github.com/LazarusDevelopment/Lazarus](https://github.com/LazarusDevelopment/Lazarus)
- **MC-Market:** [https://www.mc-market.org/resources/11362/](https://www.mc-market.org/resources/11362/)

---

## What Was Changed?

- Minor code modifications (staff mode, KoTH improvements)
- Updated `pom.xml` to fix build issues and dependencies

See [CHANGELOG.md](CHANGELOG.md) for details.

> **Note:** The entire original codebase belongs to [LazarusDevelopment](https://github.com/LazarusDevelopment/Lazarus).

---

## Building

```bash
mvn clean package
```

The compiled JAR will be located in the `target/` directory.

---

## Credits

All credit goes to:
- **[LazarusDevelopment](https://github.com/LazarusDevelopment)** - Original authors of Lazarus HCF Core
- **[qiooip](https://github.com/qiooip)** - Original developer

This fork exists solely to provide a working build configuration. I do not claim ownership of any code in this repository.
