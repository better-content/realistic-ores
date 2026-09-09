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

`tools/GenerateDepositTextures.java` currently reproduces all 288 shipped 16×16 faces and also
owns the geology-v4 candidate gate. A v4 candidate is one 1536×1024 transparent 3×2 cubemap atlas
per variant. Its alpha—not a hand-authored symbol or morphology mask—is the source geometry.
The preview reducer area-samples that alpha and its material zones into a 32×32 ore layer over
a deliberately 16×16-scaled host. It then snaps every mineral pixel to a five-colour, family-specific
pixel-art ramp with hard edges and no blended photographic shading. The extra logical resolution
preserves partings, intersections, and crackle-breccia boundaries while the result still reads as
authored Minecraft pixel art.

The complete 24-atlas suite lives under
`art/block-master-candidates/geology-v4/<family>/variant_<n>.png`. The validator requires every
family and all three variants; partial suites cannot pass. The existing 16×16 runtime remains
stable while the visual gate is open and is never silently replaced by candidates.

```sh
java tools/GenerateDepositTextures.java --write
java tools/GenerateDepositTextures.java --check
java tools/GenerateDepositTextures.java --preview coal_measures 0 /path/to/atlas.png build/preview 32
java tools/GenerateDepositTextures.java --validate-candidates art/block-master-candidates/geology-v4
```

Source masters are generated one atlas at a time with built-in ImageGen on transparent alpha. A
candidate supplies geological topology and the locations of meaningful material accents—not the
finished rendering style. The reducer removes hi-fi surface noise, preserves alpha connectivity,
and produces flat clustered pixel art. It may not replace generated geometry with an emblem or
hand-authored mask. Runtime PNGs are generated artifacts and are never hand-painted.

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
