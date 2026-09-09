# Processing item art

## Geological morphology contract

The eight player-facing families use recognizable geology rather than symbols. Coal Measures
forms broken uneven carbon seams; Ironstone forms rusty lenticular and oolitic beds; Copper
Bloom forms crosscutting stockwork with oxidation halos; Tin Quartz forms steep quartz lodes
with cassiterite splays; Brassroot follows an asymmetric dendritic fracture; Evaporite Beds
stacks salt and gypsum beds with crystalline pockets; Hotstone forms a lopsided breccia pipe
with sparse radial fissures; Black Shale carries dark laminations with violet stringers.

The canonical 144×18 badge strip is rendered only on interaction surfaces. World blocks remain host-rock dominant and communicate through morphology, value, and physical behavior before their tooltip names the aspect.

## Deposit block art

`tools/GenerateDepositTextures.java` is the canonical importer/renderer for all 288
stone/deepslate, three-variant, six-face deposit textures. For curated families it reads one
1536×1024 transparent 3×2 cubemap atlas per variant from
`art/block-masters/<family>/variant_<n>.png`, normalizes generated alpha, samples color and
material variation along a deterministic family-specific geological mask, quantizes it to
the exact five-color family ramp, and composites 20–33 mineral pixels over directional host
rock. The mask—not the source master's opaque center—owns seam, lode, bed, stockwork, root,
and breccia topology so aggressive reduction cannot collapse into an ore blob. Families
awaiting curation retain the deterministic fallback until their master set passes the same
review gate.

```sh
java tools/GenerateDepositTextures.java --write
java tools/GenerateDepositTextures.java --check
```

The source masters are generated one atlas at a time with built-in ImageGen, then normalized
and reduced algorithmically. Runtime PNGs are generated artifacts and are never hand-painted.

The PNGs under `item-masters/` are accepted 1024x1024 transparent masters generated
with the built-in ImageGen workflow documented below. Runtime sprites are deterministic
16x16 reductions; do not paint or regenerate them directly.

## Rebuild and verify

```sh
java tools/DownsampleItemTextures.java --write
java tools/DownsampleItemTextures.java --check
```

Import an accepted ImageGen candidate through the same alpha-aware normalization path:

```sh
java tools/DownsampleItemTextures.java --import small_chunks copper_bloom /path/to/candidate.png
```

Valid kinds are `small_chunks`, `crushed_feeds`, and `concentrates`. Import rejects
images without a real alpha channel, crops only materially opaque pixels, fits the
subject onto a transparent square, and writes the canonical 1024x1024 master. The
source generator must not overwrite curated masters or runtime sprites.

## Prompt language

Use one built-in ImageGen call per asset. Use the repository ore-chunk preview and the
matching full-chunk sprite as references for family forms. Ask for an isolated game-item
cutout on genuinely transparent alpha with generous padding, upper-left lighting,
strong grouped values, no cast shadow, no text, no container, no watermark, and no
detached particles.

- Small chunks are single compact ore-bearing fragments, never piles or cubes.
- Crushed feeds are low, coarse, irregular multi-fragment heaps, never fine powder.
- Concentrates are clean low mounds of fine powder, never rocks, bags, or bowls.

Family prompts must name the morphology and five colors in `tools/ore_art_manifest.json`.
Concentrate prompts must name the output material and use the frozen five-color palette
in `tools/concentrate_art_manifest.json`. Accepted prompts follow this exact scaffold,
with only the item name, subject description, and palette substituted:

```text
Use case: stylized-concept
Asset type: high-resolution source master for a Minecraft 16x16 inventory icon
Primary request: <one small fragment | a coarse crushed-feed heap | a fine concentrate mound>
Scene/backdrop: genuinely transparent alpha background, not a drawn checkerboard
Subject: <material-specific morphology>
Style/medium: polished hand-painted game item icon designed to survive deterministic reduction to crisp 16x16 pixel art
Composition/framing: centered isolated subject, slight three-quarter top view, square composition, generous transparent padding
Lighting/mood: simple upper-left light, strong grouped values, no cast shadow or glow outside the subject
Color palette: anchored to <five manifest colors>
Constraints: real transparent alpha; clean silhouette; opaque subject; no text; no label; no border; no watermark
Avoid: wrong material form, background, floor, checkerboard, glow, dust cloud, fuzzy edge, detached particles
```

Nearest-neighbor inspection previews are written under ignored `build/texture-previews/`.
