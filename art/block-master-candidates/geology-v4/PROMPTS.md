# Geology-v4 ImageGen prompt set

All 24 atlases were generated with the built-in ImageGen tool, one call per family/variant and no
reference images. The accepted calls used the common contract below plus the matching family rule
from `../../GEOLOGICAL_MORPHOLOGY.md` and one variant directive from this file. Failed dense or
non-transparent generations were not copied into this directory.

## Common contract

```text
Use case: stylized-concept
Asset type: geological alpha-source cubemap atlas for deterministic Minecraft 32×32 pixel-art reduction
Canvas/layout: output exactly one 1536×1024 RGBA PNG arranged as a strict 3 columns × 2 rows of six equal 512×512 cells, with zero gutters, borders, dividers, labels, or text. Cell order is north, east, south / west, up, down.
Transparency: draw ONLY deposit material on genuine transparent alpha. Keep every cell predominantly transparent. Do not draw host rock, a matte, checkerboard, studio background, cast shadow, glow, frame, or tile.
Rendering role: this is a clean morphology-and-material-zone source, not a photograph and not the final block. Use crisp bounded shapes and grouped mineral colors suitable for deterministic flat pixel-art reduction. Avoid photographic surface noise, soft haze, antialias fringe, and detached decoration.
Cubemap coherence: all six cells depict cuts through one coherent cubic deposit. Features reaching an edge continue plausibly at the corresponding adjacent-face position. Opposite faces differ naturally.
Composition: host must remain dominant after compositing. Never make a centered emblem, circular ore blob, nugget constellation, symbol, or filled square.
```

Retries strengthened transparency to 82–90% and explicitly limited up/down faces to short or
narrow geological intersections. This is why a steep lode or thin bed may have a small end-section
without being treated as a missing texture.

## Variant directives

### Coal Measures

- Variant 0: nearly horizontal regional bedding; thin persistent upper bench; thicker lower bench
  with a discontinuous claystone parting, one pinch, and one small normal-fault step.
- Variant 1: gently east-dipping beds; upper bench briefly splits and rejoins; lower bench is cut by
  a transparent sandstone channel and resumes offset.
- Variant 2: shallow synclinal curvature; one lenticular bench terminates while the other remains
  continuous with variable thickness, a short parting, and two tiny pyrite sites.

### Ironstone

- Variant 0: paired near-horizontal lenses; broad oolitic lower lens and thin upper lens with one
  break and early termination.
- Variant 1: gently dipping lenses with erosional truncation; main lens splits around a transparent
  host tongue and oolites concentrate down-dip.
- Variant 2: two separate shallow-dipping lenses; main lens pinches out across a transparent
  channel; one small attached oolitic pocket; up/down contain only narrow lenticular traces.

### Copper Bloom

- Variant 0: off-center southwest–northeast older vein, three younger crosscuts, one small offset,
  and sparse weathered intersections.
- Variant 1: uneven sheeted subparallel veinlets cut by one steep younger fracture, with sulfides
  concentrated along one margin.
- Variant 2: arcuate stockwork around a transparent low-density center, two short crosscuts, and one
  truncated segment; intersections remain distributed rather than forming a hub.

### Tin Quartz

- Variant 0: narrow north-dipping lode with one small widening, one fault offset, and two splays;
  up/down show a short diagonal trace and tiny attached splay.
- Variant 1: near-vertical lode with a transparent interruption and three short one-sided splays;
  up/down show two separated narrow line segments.
- Variant 2: two narrow en-echelon segments linked by one oblique veinlet and one short greisen
  branch; up/down show abbreviated offset traces without a central crossing.

### Brassroot

- Variant 0: off-center steep joint, two short bedding openings on one side and one opposite, with
  attached lower replacement blebs.
- Variant 1: oblique joint with two steps, one tiny incomplete collapse pocket, and two brief
  bedding openings.
- Variant 2: two narrow en-echelon joint segments linked by one short bedding opening and one tiny
  attached edge pocket.

### Evaporite Beds

- Variant 0: three narrow subhorizontal beds; lower bed has one attached nodule, middle bed has two
  mud interruptions, plus one very short diagonal vein.
- Variant 1: three gently dipping rhythmic beds of unequal length, one broken gypsum bed, and one
  tiny lens continuing around an edge.
- Variant 2: two shallow wavy beds and one shorter parallel bed, one pinch-out, one attached nodule,
  and a late thin vein on only two adjacent faces.

### Hotstone

- Variant 0: narrow off-center breccia corridor trending up-right, three incomplete angular clast
  loops, sparse outer fractures, and one offsetting late fissure.
- Variant 1: two en-echelon crackle pockets connected by a thin matrix bridge, with large transparent
  clasts and only two bright junctions.
- Variant 2: curved asymmetric pipe trend, dense along one margin and sparse on the other, cut by a
  near-vertical younger fissure.

### Black Shale

- Variant 0: two near-horizontal lamination packages separated by host, a gentle lower sag with an
  attached carbon lens, and a tiny fault step above.
- Variant 1: gently folded laminations with one asymmetric anticline, two offsets, one short subdued
  violet-gray trace, and two pyrite sites.
- Variant 2: shallow-dipping laminae cut by a transparent erosional gap and resumed offset, with one
  attached carbon lens and sparse pyrite along one lamination.
