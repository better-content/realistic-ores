# Changelog

## 0.2.0 - 2026-08-25

### Changed

- Replaced the 23 taxonomic processing and worldgen families with ten player-facing
  deposits: Coal Measures, Ironstone, Copper Bloom, Tin Quartz, Brassroot, Redbed,
  Evaporite Beds, Gem Pipe, Hotstone, and Black Shale. Oil Seep remains independent.
- Rebuilt processing around 24 proven assay outputs plus rock salt, sodium chloride,
  and saltpeter. Gem Pipe and Hotstone expose multiple assay profiles through routes.
- Made first-contact promises mechanical: coal chunks burn, Evaporite yields salt,
  Gem Pipe yields rough chips, Black Shale supports soul fire, and Hotstone warms,
  hurts, and consolidates into magma.

### Removed

- Removed inert beryl, beryllium, calcium, carbon, chromium, gallium, iridium,
  magnesium, phosphate, platinum, silicon, sodium, tantalum, and tungsten concentrates.
- Removed the superseded family IDs and their blocks, items, worldgen, processing,
  Excavated Variants entries, language keys, and art without compatibility aliases.

## 0.1.0

### Changed

- Rebalanced every family to two primary units per full chunk and one per crushed feed;
  millstones now produce two crushed feeds and crushing wheels produce three.
- Added immediate low-yield bauxite recovery and restricted TConstruct ore processing to
  metal primary products and metal coproducts.
- Made oil seep collectible by right-click like the other surface indicators.
- Added a canonical family art manifest for palette- and morphology-consistent chunk and
  crushed-feed sprites.
- Surface samples now place and collect through the matching `small_ore_chunk_<family>` BlockItem.
- Moved the complete 23-family chunk, crushing, separation, concentrate, thermal-exit,
  grinding-media, gem-chip, and Excavated Variants lifecycle into Realistic Ores.
- Renamed the pre-normalization amethyst family to Amethyst-Beryl Pegmatite and added the
  uncommon Gold-Quartz Vein family without compatibility aliases.
- Standardized the project as **Realistic Ores** with mod ID `realistic_ores`, artifact `realistic-ores`, and package `com.bettercontent.realisticores`.
- Adopted Java 17 and Forge 1.20.1-47.4.13 as the build baseline without changing the project version.
- This is a clean break; legacy worlds, configurations, and integrations are not migrated.
