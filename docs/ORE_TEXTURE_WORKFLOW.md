# Ore texture workflow

Status: living art and implementation contract. Update this document whenever the accepted source,
prompting method, reducer, host treatment, review gate, or shader/Excavated Variants contract changes.

Last accepted suite: geology-v7, 2026-09-10. It contains three textures for each of eight families.

## What the texture must communicate

An ore block is a one-block window into a deposit larger than the block. It is not a stone cube
containing a nugget, badge, gem, or centered object. Each texture must satisfy two views at once:

1. Alone, one block has a deliberate, attractive composition with enough structure to identify it.
2. In a wall of random variants, edge-crossing structures imply a larger seam, lens, lode,
   stockwork, replacement body, or breccia zone without requiring connected textures.

Geological realism here means realistic morphology translated into stylized Minecraft pixel art.
It does not mean photorealistic rendering or high-frequency mineral photography.

## Canonical architecture

- Each family has exactly three independently generated variants.
- Each variant is one square transparent mineral-overlay master under
  `art/block-masters/geology-v7/<family>/variant_<0..2>.png`.
- The same reduced mineral texture is used on all six cube faces, matching vanilla ore blocks.
  This makes every face suitable when it is the only visible face and avoids hidden weak panels.
- Vanilla random model selection supplies patch variation. Exact edge-to-edge CTM matching is not
  required; the edge crossings create a continuation illusion.
- Stone uses the canonical 1.20.1 stone host on every face. Deepslate uses its lateral host on the
  four sides and `deepslate_top` on up/down, so host sidedness remains correct.
- Runtime textures are ordinary opaque 16×16 PNGs. Excavated Variants and shaders consume normal
  block definitions; neither integration depends on the high-resolution art representation.

## Begin with geology, not a picture prompt

Write a short morphology rule before generation. It must name:

- the deposit process and analogue;
- its dominant structure;
- two or three subordinate structures;
- edge behavior;
- transparent-space target;
- material zoning and restrained accents;
- forbidden shapes, especially the nearest confusing family.

Current identity anchors:

| Family | Dominant morphology | Must not become |
| --- | --- | --- |
| Coal Measures | broad persistent carbonaceous benches with pale partings | Black Shale fissility or isolated coal chunks |
| Ironstone | flattened oolitic lenses and linked sedimentary pods | coins, bars, or one round rust blob |
| Copper Bloom | multi-generation crosscutting stockwork | a central X, radial hub, or green confetti |
| Tin Quartz | steep quartz-cassiterite lode, offsets, and splays | a lightning bolt or centered crystal |
| Brassroot | joint fill with short bedding-replacement branches | a literal root, tree, or gold outline |
| Evaporite Beds | imperfect persistent beds, mud breaks, and nodules | perfect stripes, ladders, or icicles |
| Hotstone | asymmetric crackle breccia and sparse fluid conduits | lava wallpaper, glowing rings, or stars |
| Black Shale | thin tapered fissility, cleavage, and crenulated wisps | Coal's broad continuous benches |

`art/GEOLOGICAL_MORPHOLOGY.md` owns the detailed geological rules. World-generation geometry is a
separate contract: it should echo the family process at deposit scale, but it does not determine
the pixels in one block face.

## ImageGen workflow

Use the built-in ImageGen tool, one call per variant. Generate the mineral layer only. Do not ask
for a finished ore-on-stone texture: the accepted host is composited later.

Use this prompt scaffold and replace only the bracketed family/variant material:

```text
Use case: stylized-concept
Asset type: one transparent Minecraft-style ore-overlay texture master designed to become exactly
one 16x16 all-face ore-block texture
Primary request: Create one square pixel-art overlay for [family], variant [0..2]: [specific
morphology]. Geological identity: [process and dominant structure]. It must read as a deposit
passing through a volume of rock and continuing beyond the block, never as an object placed inside
one block.
Scene/backdrop: genuinely transparent RGBA canvas; every empty region is alpha zero.
Style/medium: polished Minecraft Java Edition 16x16-style pixel art authored at high resolution
using large hard-edged square pixel clusters, deliberate stepped silhouettes, restrained flat color
groups, and minimal shading; stylized geological morphology, not photorealism.
Composition/framing: one square texture reused on every cube face; mineral coverage roughly 20% to
46% after reduction; the coherent formation touches at least two canvas edges; asymmetrical
whole-face balance; attractive and legible when this is the only exposed ore block; this variant
must remain visibly distinct from its siblings.
Materials/textures: [family palette and zoning].
Constraints: actual transparent alpha; exactly one complete standalone texture; hard pixel
boundaries; no host stone or deepslate pixels; substantial open alpha; no frame, panel, text,
label, or watermark.
Avoid: [family confusions]; presentation board; drawn checkerboard; colored or gray backdrop;
haze; vignette; external shadow; glow; gradients; anti-aliased fringe; smooth painting;
high-resolution realism; singular gemstone; centered specimen; uniform speckles.
```

Do not generate a 3×2 panel atlas. In practice, multi-panel prompts encouraged ImageGen to paint a
smoky presentation board and made individual panels weaker. Background-extraction retries also
painted checkerboards instead of reliable alpha. Reject these outputs; do not threshold or repair
them algorithmically.

## Alpha-master acceptance gate

Inspect the generated master before copying it into the repository:

- real alpha channel, with genuinely clear regions;
- square and at least 1024×1024;
- no matte, checkerboard pixels, host rock, haze, vignette, external shadow, or halo;
- one coherent formation rather than disconnected decorative fragments;
- at least two edge contacts after reduction;
- asymmetrical but balanced composition when isolated;
- variant changes structure, not only rotation, mirroring, or color;
- no family-specific forbidden form.

The current reducer accepts 50–120 materially visible pixels of 256 (19.5–46.9%). That is a guard
rail around the accepted suite, not a target to fill. Negative space and recognizable structure
matter more than maximizing coverage.

## Reduction and host compositing

The high-resolution master is authoritative. `tools/GenerateDepositTextures.java` performs one
center-point nearest-neighbor sample per output pixel and preserves that sampled RGBA. It does not:

- threshold or reconstruct alpha;
- trace paths or force edge contacts;
- quantize the mineral palette;
- prune details;
- paint missing pixels;
- generate new morphology.

The sampled overlay is then composited onto the checked-in canonical Minecraft 1.20.1 hosts under
`art/host-textures/minecraft-1.20.1/`. Runtime PNGs are fully opaque.

Commands:

```sh
java tools/GenerateDepositTextures.java --validate-masters
java tools/GenerateDepositTextures.java --preview FAMILY VARIANT /path/to/master.png build/texture-preview
java tools/GenerateDepositTextures.java --write
java tools/GenerateDepositTextures.java --check
./gradlew verifyFull
```

`--preview` never promotes a candidate. Copy an approved master into geology-v7, run `--write`,
update canonical hashes, and run the full gate only after visual approval.

## Review gate

Review every variant in its own tab. Each sheet must show:

- the 16×16 alpha over a checkerboard;
- the isolated stone face;
- the isolated deepslate face;
- a correctly aligned isometric block using the same texture on all faces;
- a wall made from square, unstretched blocks using all three family variants.

Inspect at native size and with nearest-neighbor enlargement. Reject stretched block previews,
misaligned cube projections, bilinear filtering, or a wall that hides the single-block result.

Ask, in order:

1. Does a single block look intentionally composed?
2. Does it read as a cut through a larger deposit rather than an object?
3. Is the family identifiable without hue alone?
4. Do three variants produce an immersive patch without obvious repeated stamps?
5. Do stone and deepslate retain their host identity?
6. Are Black Shale and Coal unmistakably different?

## Excavated Variants and shaders

Every family must remain listed in the Excavated Variants provider and modifier resources. Derived
host ores should inherit the normal block/model identity; do not create a hand-authored texture
matrix for every rock mod.

Shader support is block classification, not painted emission. Keep runtime textures opaque and do
not add full-face emissive masks. Every canonical and derived ore block must remain in the modded-ore
classification used by the pack's shader integration. Hotstone may have restrained bright mineral
junctions and block light, but it must not become a glowing lava web.

## Maintaining this document

When the workflow changes, update this file in the same commit as the generator or art change.
Record why the old method failed, what visual invariant the replacement protects, and which test or
review catches regression. Do not rewrite history by deleting superseded candidate directories;
label them as superseded and keep the accepted suite unambiguous.
