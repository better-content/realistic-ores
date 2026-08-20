# Changelog

## Unreleased

### Changed

- Surface samples now place and collect through the matching `small_ore_chunk_<family>` BlockItem.
- Moved the complete 23-family chunk, crushing, separation, concentrate, thermal-exit,
  grinding-media, gem-chip, and Excavated Variants lifecycle into Realistic Ores.
- Renamed the pre-normalization amethyst family to Amethyst-Beryl Pegmatite and added the
  uncommon Gold-Quartz Vein family without compatibility aliases.
- Standardized the project as **Realistic Ores** with mod ID `realistic_ores`, artifact `realistic-ores`, and package `com.bettercontent.realisticores`.
- Adopted Java 17 and Forge 1.20.1-47.4.13 as the build baseline without changing the project version.
- This is a clean break; legacy worlds, configurations, and integrations are not migrated.
