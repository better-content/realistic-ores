# Geological morphology specification

## Purpose

The high-resolution transparent alpha masters are the source of ore geometry and mineral color.
The reducer performs one center-point nearest-neighbor sample per 16×16 output pixel, preserves the
sampled RGBA, and composites the result over canonical host rock. It must not replace generated
morphology with a symbolic mask, threshold or reconstruct alpha, quantize color, or synthesize
missing detail.

Each variant is one square standalone texture master reused on all cube faces, matching vanilla ore
blocks. A feature that reaches an edge implies continuation into adjacent randomly varied blocks;
exact connected-texture alignment is not required.

## Shared visual rules

- Render mineralization only on genuine transparent alpha. Do not render host rock, a colored
  matte, a checkerboard, shadows, bloom, labels, dividers, borders, or detached decoration.
- Keep enough transparent space for host rock to remain dominant after compositing.
- Use mostly one-to-three-pixel structures after 16× reduction, with hairline one-pixel branches
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

Identity anchor: Coal Measures owns the thick, laterally persistent, low-angle bench silhouette.
No other dark family may use two broad parallel seams or an uninterrupted mineable band.

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
organic matter or sulfides. The visible ore layer represents enriched fissility and cleavage
surfaces within shale, not the entire shale bed. Use hair-thin, discontinuous, tapered films that
anastomose, step en echelon, climb as steep slaty cleavage, or crenulate around compacted voids.
Large transparent gaps must separate individual traces. Add sparse framboidal pyrite specks and
only a trace of subdued violet-gray enrichment following a film; purple is an accent, not a vein.
Avoid coal's thick low-angle benches, repeated parallel seams, uninterrupted bands, bright purple
ribbons, crossing fracture networks, thick black bars, or magical contamination clouds.

Identity anchor: Black Shale owns fine fissility, oblique cleavage, and folded carbonaceous wisps;
Coal owns stratiform benches. At 16×16, Black Shale must still show tapered broken traces or steep
fabric rather than reading as a thinner recolor of Coal Measures.

## Alpha-master acceptance

A master is rejected before import when it lacks real alpha, uses a colored matte or drawn
checkerboard, contains detached visual noise, reduces outside the accepted 50–120 visible-pixel
guard rail, misses the two-edge continuation rule, or violates its family's forbidden forms.
Review the high-resolution alpha over checkerboard plus isolated stone and deepslate before import.

The reduced result is reviewed at native 16×16 and nearest-neighbor enlargement. It must look
intentionally pixel-authored and retain the master's meaningful partings, intersections, lenses,
and clast margins. If topology does not survive, regenerate the master; do not hand-paint the
runtime PNG or repair the alpha algorithmically. The full gate is maintained in
`docs/ORE_TEXTURE_WORKFLOW.md`.

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
