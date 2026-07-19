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
- Keep the host rock visually dominant. In the source mineral masks, only 20-33 of 256
  pixels are occupied. Variants should remain within that observed coverage envelope
  unless a deliberate family-specific exception is documented.
- Use crisp 16x16 pixel art. Source mineral masks use fully opaque pixels, no smoothing,
  no semitransparent edge pixels, and exactly five mineral colors per family.
- Make identity depend on silhouette and value structure as well as hue. A texture must
  remain recognizable in low light and under shaders; increasing saturation is not a
  substitute for preserving its deposit shape.
- Preserve unequal clustering and negative space. Do not distribute mineral pixels
  uniformly, add one-pixel confetti, or recolor a shared generic ore mask.
- Variants are alternate arrangements of the same geology. They may move, fork, shorten,
  or thicken clusters while retaining the family's morphology, coverage, palette, and
  overall light/dark balance. Rotation or mirroring alone does not count as a texture
  variant.
- Each visual family has exactly three equally weighted cubemap variants: canonical
  variant `0` and alternate variants `1` and `2`.

### Host treatment

- Both stone and deepslate variants are complete, authored cubemaps with distinct
  `north`, `east`, `south`, `west`, `up`, and `down` textures. Do not use `cube_all`,
  runtime model rotation, mirroring, or independently randomized faces for variant art.
- Treat the six mineral masks as intersections of one small three-dimensional deposit,
  not as six unrelated drawings. When a seam or vein reaches a face edge, continue its
  position, width, palette step, and direction onto the adjacent face. A feature may end
  before an edge, but it must not be visibly cut at an edge without a matching continuation.
- A variant index uses the same six mineral masks for its stone and deepslate forms; only
  the host-rock composite changes. This makes `stone/1` and `deepslate/1` two host views
  of the same deposit topology rather than unrelated art.
- Opposite faces belong to the same imagined deposit volume but must not duplicate or
  mirror one another. Each exposed face must still communicate the family morphology;
  do not hide all identifying material on one preferred viewing side.
- Stone faces composite those six masks over the normal stone host. The mineral is
  embedded in the host rather than drawn as a floating outline or decal.
- Deepslate uses the directional deepslate side host beneath all four lateral masks and
  the appropriate top or bottom host beneath the vertical masks. Preserve that host
  directionality while allowing all six mineral arrangements to differ.
- Keep the mean mineral coverage of a six-face set within the canonical 20-33-pixel
  envelope per face. Individual faces may vary modestly to express the deposit volume,
  but none should become an empty host face or a mineral-dominated panel.
- Balance the three cubemaps as a set so no family develops a fixed compass signature,
  such as its brightest crystal always appearing on the north face.
- Host contrast may shift naturally between stone and deepslate, but the mineral palette
  and family silhouette stay recognizable. Do not brighten deep variants merely to make
  them as luminous as their stone versions.
- Keep all block textures fully opaque. Emissive pixels are not part of the current
  language, including uranium, thorium, redstone-bearing redbed, and osmiridium.

### Deposit families and palettes

Palette entries are the five source mineral colors, ordered approximately dark to light.
They are anchors for variants, not a license to replace the family morphology with a
recolor.

| Family | Required morphology | Mineral palette |
| --- | --- | --- |
| Bauxite laterite | scattered rusty angular nodules | `#7b3e30` `#975a3a` `#cd8553` `#efb687` `#d7c6b0` |
| Coal measures | broken dark carbon seams with restrained ochre inclusions | `#202024` `#3c3c42` `#666670` `#7b6a30` `#b09a52` |
| Copper sulfide | small clustered brown/brassy disseminations | `#5c3a21` `#5a693d` `#9f612f` `#7f8d59` `#d8974b` |
| Corundum-beryl gem vein | sparse diagonal purple-blue crystal vein | `#6c2b90` `#3d74cf` `#d277f0` `#78b8ff` `#cfd7dc` |
| Cupriferous redbed-redstone | sharp, locally clustered red fracture seam | `#5d1116` `#a31c25` `#7e3d2c` `#ff4f4f` `#b47240` |
| Emerald schist-beryl | branching green crystal stringers | `#216d39` `#697850` `#38b35c` `#8fbfa0` `#9bf4ad` |
| Ironstone | rusty, laterally biased bedded seam | `#5d3123` `#6b4d56` `#8d4e35` `#8d6f78` `#c37754` |
| Kimberlite pipe | very sparse muted fragments with icy indicator crystals | `#384436` `#5a6b58` `#67737b` `#86d7e0` `#d1fbff` |
| Lazurite vein | narrow branching blue vein with a small gangue accent | `#244291` `#3b66d4` `#7d90a5` `#79adff` `#d5c986` |
| Lead-zinc vein | cool-grey branching metallic vein with warm gangue | `#4c4c59` `#767687` `#8d7a61` `#a9a9b9` `#d5c39b` |
| Nickel sulfide | muted olive angular disseminations with metallic flecks | `#516049` `#81906b` `#8d8d98` `#bccd88` `#c9c9d3` |
| Phosphate rock | soft pale-olive nodules with subtle pink gangue | `#82906d` `#d4aab5` `#afc28e` `#d1cabb` `#dde6c6` |
| Quartz vein | thick, high-value branching white fracture fill | `#d7b97b` `#bcc7ce` `#d8d2ca` `#ece8e0` `#f7f0e7` |
| Soul-bearing black shale | thin, dark irregular seam with sparse violet energy | `#19161d` `#4e3272` `#394038` `#a173ff` `#cbc4db` |
| Sulfur-bearing pyrite | sparse chained brassy grains with neutral gangue | `#5a554f` `#706028` `#9b9388` `#b49936` `#e2ca63` |
| Thorium | sparse subdued olive splinters with warm pale gangue | `#4a5a3b` `#7c9358` `#b9b38d` `#bbd68f` `#dfd8ae` |
| Tin | sparse cool-white fracture fragments | `#74777d` `#a5aab1` `#d9d2c4` `#dde1e7` `#f1ece4` |
| Tin-tungsten greisen | denser angular grey-white vein network | `#434349` `#71737a` `#afb4bb` `#ddd9d1` `#f2efe8` |
| Titanium-iron oxide | dark angular bands with muted green and metallic highlights | `#4d403a` `#776863` `#6c7147` `#97a160` `#b7c4cc` |
| Uranium | clustered acidic-green grains, bright but non-emissive | `#2d4c20` `#718145` `#4d8f2c` `#a7ae86` `#a2ff49` |
| Zinc | sparse warm-grey/tan disseminated grains | `#75614b` `#8c705e` `#9a866d` `#cbbca2` `#d8cfb2` |

Osmiridium lava sulfide intentionally aliases the nickel-sulfide block textures at
present. Treat that alias as current compatibility behavior, not as a distinct visual
identity invented without new art direction.

### Crushed material and surface samples

- Crushed items are compact lower-center piles, not miniature ore blocks. Existing
  sprites occupy roughly 30-40 visible pixels inside an `x=3..12`, `y=6..11` envelope
  (the narrowest families use `x=4..11`).
- The crushed sprites use seven colors and limited partial alpha only around pile edges.
  Their opaque interior carries the parent family's hue and value hierarchy.
- Placeable surface samples reuse the crushed-item texture on ore-specific scatter
  geometry. Preserve the guaranteed opaque UV patch and the five deterministic geometry
  variants; texture variants must not make those sampled UVs transparent.

### Variant acceptance checklist

Before accepting a block texture variant, review it at native size, nearest-neighbor
upscale, beside every other family, and in a representative stone/deepslate cave under
normal lighting and shaders. Confirm that:

1. The family is identifiable without its tooltip.
2. Mineral coverage and value balance remain close to the canonical texture.
3. The arrangement is genuinely new and does not create obvious tiling or face-edge
   artifacts in a cluster of blocks.
4. All six stone faces and all six deepslate faces form coherent cubemaps, with adjacent
   edge crossings aligned and no duplicated or mirrored opposite faces.
5. No variant can be confused with another family, vanilla ore, or exposed surface-sample
   rubble.
6. Inventory/JEI continues to use one stable canonical model while world blocks select
   complete cubemap variants through equally weighted static blockstate models. Faces
   are never selected or randomized independently.

## Common commands

```bash
./gradlew verifyFast
./gradlew verifyFull
./gradlew stageRuntimeJar
```

`verifyFull` currently matches the fast lane because this repo does not yet ship Forge GameTests.

## Release artifact

Deploy the reobfuscated runtime jar from:

- `build/libs/realisticores-<version>.jar`
