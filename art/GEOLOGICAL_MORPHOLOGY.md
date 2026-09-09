# Geological morphology specification

## Purpose

The high-resolution alpha masters are the source of ore geometry. The importer may remove
microscopic noise, resample coverage, quantize color, and composite host rock, but it must not
replace generated morphology with a symbolic mask. The target face is 32×32, with host rock
sampled at 16×16 and expanded 2×. Generated alpha is reduced to hard pixel clusters and snapped to
five flat family colours. That mixed-resolution composition retains meaningful partings,
intersections, and clast margins without retaining photographic surface noise.

Each variant is one 1536×1024 transparent 3×2 cubemap atlas. Cell order is north, east, south,
west, up, down. The six cells describe cuts through one coherent cubical rock sample. A feature
that reaches an edge must continue at the corresponding position on the adjacent face. Opposite
faces may differ but must remain compatible with the same three-dimensional body.

## Shared visual rules

- Render mineralization only on genuine transparent alpha. Do not render host rock, a colored
  matte, a checkerboard, shadows, bloom, labels, dividers, borders, or detached decoration.
- Keep 70–88 percent of each cell transparent. Host rock must remain dominant after compositing.
- Use mostly one-to-three-pixel structures after 32× reduction, with hairline one-pixel branches
  only where the source supports them. Never produce a centered emblem, star, X, root icon,
  nugget, or contiguous round ore patch.
- Preserve scale hierarchy: one dominant structure, two or three subordinate structures, then a
  few genetically related disseminated grains. Random speckle is not morphology.
- Mineral color follows mineral position: gangue and alteration selvages border ore-bearing
  structures; bright values occupy small internal or intersection zones rather than outlining the
  whole shape.
- Variants change dip, thickness, branching, interruption, and exposure—not merely rotation,
  mirroring, or recoloring.

## Family rules

### Coal Measures — bedded coal with rock partings

Analogue: laterally persistent coal beds subdivided into benches by shale, claystone, or siltstone
partings. Vertical faces show two low-angle, subparallel carbonaceous beds that pinch, split, or
terminate locally. Top and bottom faces show long, gently curving bed traces rather than horizontal
stripes. Include one narrow mineral-matter parting inside the thicker bed and rare dull pyrite
flecks. Avoid fracture veins, crystalline rims, glossy highlights, and crossing diagonals.

### Ironstone — oolitic and lenticular sedimentary beds

Analogue: shallow-shelf oolitic ironstone in multiple layers interbedded with clastic country rock.
Use one broad but discontinuous rusty lens aligned with bedding, a thinner parallel lens, and small
rounded oolitic concentrations concentrated inside those lenses. Siderite/goethite/hematite values
remain earthy brown, ochre, and subdued rust. Avoid metallic bars, rectangular frames, continuous
orange outlines, or a single solid pod.

### Copper Bloom — porphyry-style stockwork and supergene coatings

Analogue: fracture-controlled quartz–chalcopyrite stockwork with disseminated sulfides in altered
wall rock and green/blue copper carbonates limited to weathered fractures. Use several narrow,
irregular veinlets of different generations; later veinlets crosscut earlier ones without forming a
symmetrical X. Put brassy sulfide grains inside or beside veinlets and sparse malachite/azurite on
fracture margins and intersections. Alteration halos are broken and narrow, never filled regions.

### Tin Quartz — quartz–cassiterite fissure lode and greisen selvage

Analogue: steep quartz-cassiterite fissure fillings with local stockwork splays in greisenized wall
rock. Use one steep, thickness-varying quartz lode with parallel walls, one offset or brecciated
segment, and two or three thin branching veinlets. Dark cassiterite grains cluster along lode
margins, intersections, and vugs; pale quartz occupies the lode interior. Avoid a clean lightning
bolt, uniform white ribbon, or evenly spaced black dots.

### Brassroot — carbonate-hosted zinc/copper fracture fill

Analogue: sphalerite with lesser chalcopyrite and iron sulfides deposited in fractures, bedding
openings, porous replacement zones, and solution-collapse breccia. The name is mnemonic, not a
literal plant. Use one joint-controlled fracture that branches asymmetrically into bedding-parallel
openings, with discontinuous honey-brown sphalerite aggregates, rare brassy chalcopyrite, and small
replacement blebs beside the fracture. Avoid a root silhouette, central trunk, or luminous yellow
outline.

### Evaporite Beds — halite, gypsum, and anhydrite strata

Analogue: repeated bedded rock salt with mudstone partings, fibrous gypsum veins, and nodular
anhydrite. Use several thin, laterally persistent beds separated by transparent host intervals;
interrupt one bed with mud-rich inclusions and another with lenticular or nodular crystalline
aggregates. A sparse secondary fibrous vein may cross the bedding. Keep colors dirty white, pale
gray, faint pink, or muted blue-white. Avoid icicles, glowing cyan pools, perfect ladders, and a
single massive white slab.

### Hotstone — hydrothermal crackle breccia

Analogue: angular wall-rock clasts separated by a thin hydrothermal matrix in a crackle-breccia
zone, with late fracture veinlets. Use multiple irregular polygonal clast-margin fragments that
share a loose pipe-like trend but remain separated by transparent host. Only a few narrow matrix
segments carry hot red-orange values; the brightest pixels occur at two or three intersections.
Add one late offset fissure. Avoid lava cracks, a glowing star, branching lightning, or a solid red
body. Shader emission must remain a restrained mineral highlight.

### Black Shale — fissile organic laminations with sulfide enrichment

Analogue: thinly laminated organic-rich shale containing pyrite/marcasite and trace metals bound to
organic matter or sulfides. Use many very thin, subparallel dark laminations with small offsets,
soft folds, and occasional carbon-rich lenses. Add sparse framboidal pyrite specks and only a trace
of subdued violet-gray metal enrichment following a lamination; purple is an accent, not a vein.
Avoid bright purple ribbons, crossing fractures, thick black bars, or magical contamination clouds.

## Alpha-master acceptance

An atlas is rejected before import when it lacks a real alpha channel, has a materially opaque
outer border, uses a colored matte, merges cells across atlas boundaries, contains detached visual
noise, or violates its family's forbidden forms. Review the high-resolution alpha over light and
dark checkerboards before downsampling.

The reduced result is reviewed at native 32×32 and nearest-neighbor enlargement, with a separate
16× stress preview. It must look intentionally pixel-authored: hard square pixels, flat grouped
values, no antialiasing, gradients, photographic texture, or subpixel noise. Every face must retain
the master's geologically meaningful connected structures, keep at least 64% host visible for
breccia and 70% for other families, and remain recognizable without color alone. If topology does
not survive, regenerate the master or revise the alpha-preserving resampler; do not hand-paint the
runtime PNG.

## Geological references

- [USGS coal-bed field description](https://pubs.usgs.gov/bul/1111b/report.pdf): coal benches and
  discrete mineral-sediment partings.
- [USGS oolitic ironstone model](https://pubs.usgs.gov/bul/b2004/html/bull2004oolitic_ironstones.htm):
  layered oolitic iron-rich beds in shallow clastic shelf sequences.
- [USGS porphyry copper model](https://pubs.usgs.gov/of/2008/1321/pdf/OF081321_508.pdf):
  fracture-controlled stockwork veinlets, disseminated sulfides, and alteration zones.
- [USGS tin greisen model](https://pubs.usgs.gov/bul/b1693/html/bull217y.htm): cassiterite-bearing
  veinlets, stockworks, lenses, pipes, and breccia in greisenized granite.
- [USGS Mississippi Valley zinc-lead model](https://pubs.usgs.gov/sir/2010/5070/a/): sphalerite,
  galena, iron sulfides, fracture fill, replacement, and dissolution-collapse breccia.
- [British Geological Survey evaporite descriptions](https://webapps.bgs.ac.uk/Memoirs/docs/B01555.html):
  bedded rock salt, mudstone inclusions, and fibrous gypsum veins.
- [USGS porphyry-system breccia discussion](https://www.usgs.gov/publications/pressure-gradients-and-boiling-mechanisms-localizing-ore-porphyry-systems):
  crackle breccia hosting gangue and ore veinlets.
- [USGS metalliferous black-shale summary](https://pubs.usgs.gov/bul/2049a/report.pdf): thin
  laminations, organic matter, iron sulfides, and metal enrichment.
