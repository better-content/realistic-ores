# Realistic Ores

Pack-owned ore and deposit support mod for Forge `1.20.1`.

## Texture visual identity

This section is the normative art direction for Realistic Ores. It was derived from the
shipped 16x16 block and crushed-item textures and from the mineral overlays used to make
the original composites. New textures and texture variants must preserve these rules.

### Core language

- Depict geological deposits, not vanilla-style isolated ore spots. Each family has a
  recognizable morphology: a seam, branching vein, disseminated grains, nodules, or
  sparse crystals.
- Keep the authored stone and deepslate ores as ordinary, finished Minecraft blocks.
  Realistic Ores establishes their palette, morphology, coverage, and sided appearance;
  Hyle and Unearthed provide the broader rock strata; Excavated Variants reads those
  normal block definitions and derives the matching host-specific ore blocks.
- Express that contract through ordinary vanilla-format blockstates, models, and
  textures. Do not hand-author a matrix of Realistic Ores composites for every Unearthed
  stone, add custom integration code when normal definitions suffice, or simplify the
  canonical ore art to compensate for hypothetical generated hosts.
- Keep the host rock visually dominant. In the inspected canonical artwork, mineral
  material occupies only 20-33 of 256 pixels per face. Variants should remain within
  that observed coverage envelope unless a deliberate family-specific exception is
  documented.
- Use crisp 16x16 pixel art. Mineral pixels are fully opaque, with no smoothing or
  semitransparent edges, and use exactly five mineral colors per family.
- Make identity depend on silhouette and value structure as well as hue. A texture must
  remain recognizable in low light and under shaders; increasing saturation is not a
  substitute for preserving its deposit shape.
- Preserve unequal clustering and negative space. Do not distribute mineral pixels
  uniformly, add one-pixel confetti, or recolor a shared generic mineral layout.
- Variants are alternate arrangements of the same geology. They may move, fork, shorten,
  or thicken clusters while retaining the family's morphology, coverage, palette, and
  overall light/dark balance. Rotation or mirroring alone does not count as a texture
  variant.
- Each visual family has exactly three equally weighted sided model variants: canonical
  variant `0` and alternate variants `1` and `2`.

### Host treatment

- Both stone and deepslate variants are complete, finished block models with distinct
  `north`, `east`, `south`, `west`, `up`, and `down` textures. Author those opaque final
  textures directly. Do not introduce mineral-only masks, overlay source formats, or a
  custom compositing layer for Excavated Variants compatibility.
- Treat the six finished faces as views of one plausible deposit rather than unrelated
  drawings. When a seam or vein reaches a face edge, continue its position, width,
  palette step, and direction onto the adjacent face. A feature may end before an edge,
  but it must not be visibly cut at an edge without a matching continuation.
- Corresponding stone and deepslate variant indices must express the same family and
  comparable geological character, but they need not share pixel-identical mineral
  layouts. Each is a normal source ore definition in its own host.
- Opposite faces belong to the same imagined deposit volume but must not duplicate or
  mirror one another. Each exposed face must still communicate the family morphology;
  do not hide all identifying material on one preferred viewing side.
- Stone faces are finished opaque textures using the normal stone host, with the mineral
  embedded in the rock rather than drawn as a floating outline or decal.
- Deepslate faces are finished opaque textures retaining the directional deepslate side
  treatment on lateral faces and the appropriate top or bottom treatment vertically.
  Preserve that host directionality while allowing all six mineral arrangements to differ.
- Keep the mean mineral coverage of a six-face set within the canonical 20-33-pixel
  envelope per face. Individual faces may vary modestly to express the deposit volume,
  but none should become an empty host face or a mineral-dominated panel.
- Balance the three sided models as a set so no family develops a fixed compass signature,
  such as its brightest crystal always appearing on the north face.
- Host contrast may shift naturally between stone and deepslate, but the mineral palette
  and family silhouette stay recognizable. Do not brighten deep variants merely to make
  them as luminous as their stone versions.
- Keep all block textures fully opaque. Hotstone communicates heat through block light
  and contact behaviour, not emissive pixels painted into the texture.

### Excavated Variants contract

The authored stone and deepslate blocks are canonical reference implementations, not the
complete host catalogue. During world generation, Hyle/Unearthed establishes the local
rock type and Excavated Variants derives the corresponding ore block by inspecting its
registered stone and ore definitions. Realistic Ores must therefore expose conventional,
finished models and textures rather than a private intermediate art representation.

Keep `defaultresources/excavated_variants/excavated_variants/variants/realistic_ores.json5`
as the declarative mapping boundary. New art should require only normal Minecraft asset
definitions plus that existing mapping; it should not require bespoke generated textures
for each Unearthed stone. Validation should sample several light, dark, coarse, and
directional Unearthed hosts to confirm that all three canonical variants remain legible
after synthesis.

### Deposit families

| Family | First-contact promise | Required morphology |
| --- | --- | --- |
| Coal Measures | fuel | broad broken black strata |
| Ironstone | iron and tools | thick rusty bedded bands |
| Copper Bloom | copper | oxidized green/brassy mineralization |
| Tin Quartz | bronze and quartz | bright crystal ribbons with dark inclusions |
| Brassroot | zinc and brass | yellow-grey branching roots |
| Redbed | redstone | aggressive red fracture network |
| Evaporite Beds | salt and preservation | pale crystalline sedimentary beds |
| Gem Pipe | a rare gem jackpot | vertical pipe with indicator crystals |
| Hotstone | dangerous usable heat and heavy matter | dark energetic mineral body |
| Black Shale | supernatural material | black strata with sparse violet contamination |

These ten are the complete player-facing worldgen families. Oil Seep remains a separate
fluid surface feature. Exact primary and coproduct materials belong to processing depth,
not additional worldgen identities. The retained assay catalogue is the 24 useful outputs
plus rock salt, sodium chloride, and saltpeter; inert technical concentrates are removed.

`tools/ore_art_manifest.json` is the canonical five-colour palette and morphology list.
Gem Pipe and Hotstone express multiple assay fantasies through route-specific coproduct
profiles without multiplying player-facing blocks.

### Crushed material and surface samples

- Ore chunks are the host-independent mining form of a deposit. They are crisp 16x16
  non-placeable items whose silhouette and internal morphology preserve the parent
  family's geological identity; their transparent background must remain truly clear.
- Ordinary mining yields exactly one ore chunk and does not inspect Fortune. Silk Touch
  preserves the exact placeable host-rock ore block instead.
- Ore blocks separate reversibly into one chunk plus their host stone, and recombine from
  those same two ingredients. Processing a chunk is irreversible: an early-game millstone
  produces two crushed feeds, while crushing wheels produce three.
- Crushed items are compact lower-center piles, not miniature ore blocks. Existing
  sprites occupy roughly 30-40 visible pixels inside an `x=3..12`, `y=6..11` envelope
  (the narrowest families use `x=4..11`).
- Crushed sprites use the same five family mineral colors as chunks and deposits.
  Visible pixels are fully opaque against a transparent background.
- Small chunks use dedicated flat 16x16 inventory sprites rather than rendering the
  surface-sample block model. Each is one compact ore-bearing fragment occupying roughly
  16-30 visible pixels inside an `x=4..11`, `y=5..12` envelope. It retains the family
  morphology and five-color palette while remaining visibly smaller than a full chunk.
- Concentrates are clean low mounds of fine powder, not recolored chunks or coarse crushed
  feed. They use five frozen output-material colors and a compact `x=3..12`, `y=7..12`
  silhouette so shared coproducts remain identifiable independently of their source deposit.
- Crushed material and surface samples have separate processing-item and block identities.
  `crushed_*` is never placeable. Each `surface_sample_*` block deliberately has no item
  identity of its own; its BlockItem is registered only as `small_ore_chunk_<family>`, so
  EMI/JEI exposes the small chunk while the placed surface block remains hidden.
- Nine identical small chunks irreversibly combine into one full chunk. A full chunk cooks
  to two primary nuggets, gem chips, or bulk items; each crushed feed cooks to one. Four matching
  crushed feeds, one route-specific grinding ball, and exactly 500 mB of the declared
  water/acid route produce four primary concentrates plus independently rolled coproducts.
  Separation never emits washed forms or generic tailings.
- TConstruct melting and Foundry exits exist only for metal primaries and metal coproducts.
  Quartz and gems keep item-form recovery and never become molten Realistic Ores outputs.
- Coal Measures chunks are directly combustible. Evaporite chunks produce Rock Salt,
  Black Shale supports soul fire and yields soul sand, Gem Pipe yields rough gem chips,
  and Hotstone is luminous, painful to cross, and can be consolidated into magma.
- Coal measures and ironstone deliberately share a sedimentary seam vocabulary, but coal's
  broken dark carbon seam and ironstone's rusty bedded band remain distinct. Their overlapping
  height bands are preserved; their worldgen features and ADLODS deposits remain independent.
- Oil seep uses the same right-click collect/place behavior as surface samples, returning its
  own BlockItem without entering any ore processing tag or conversion.
- Surface samples use the ore family's opaque raw-deposit texture on five deterministic,
  ore-specific fragment arrangements. They must read as host-rock fragments carrying
  mineralization, not as a crushed processing pile laid on the ground.

Curated processing-item art is generated as one unique transparent high-resolution master
per item and reduced deterministically; see `art/README.md`. `tools/generate_phase3_resources.py`
owns models and recipes but must never synthesize or replace curated item PNGs.

### Variant acceptance checklist

Before accepting a block texture variant, review it at native size, nearest-neighbor
upscale, beside every other family, and in a representative stone/deepslate cave under
normal lighting and shaders. Confirm that:

1. The family is identifiable without its tooltip.
2. Mineral coverage and value balance remain close to the canonical texture.
3. The arrangement is genuinely new and does not create obvious tiling or face-edge
   artifacts in a cluster of blocks.
4. All six stone faces and all six deepslate faces form coherent sided models, with adjacent
   edge crossings aligned and no duplicated or mirrored opposite faces.
5. No variant can be confused with another family, vanilla ore, or exposed surface-sample
   rubble.
6. Inventory/JEI continues to use one stable canonical model while world blocks select
   complete sided variants through equally weighted static blockstate models. Faces
   are never selected or randomized independently.

## Common commands

```bash
./gradlew verifyFast
./gradlew verifyFull
./gradlew stageRuntimeJar
```

`verifyFull` adds `verifyItemTextures` to the fast lane, checking curated item sprites against their 1024px masters. The repository does not currently ship Forge GameTests.

## Release artifact

Deploy the reobfuscated runtime jar from:

- `build/libs/realistic-ores-<version>.jar`

## Community and support

For modpack and mod discussion, playtest feedback, and bug reports, join the [Better Content Discord](https://discord.gg/EkRnZbzqS9).

## Canonical identity

- Repository and release artifact: `realistic-ores`
- Mod ID and resource namespace: `realistic_ores`
- Java package: `com.bettercontent.realisticores`
- Validation: `./gradlew verifyFull`

This normalization is a clean break. Worlds, configuration files, and integrations created for earlier identities are not migrated or aliased.
