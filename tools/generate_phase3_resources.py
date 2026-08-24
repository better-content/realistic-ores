"""Generate the Phase 3 ownership resources from the canonical ore definitions.

This intentionally emits ordinary datapack recipes, so optional Create and Tinkers'
Construct integration remains data-driven and disappears cleanly when either mod is absent.
"""
from __future__ import annotations

import colorsys
import json
import shutil
import struct
import zlib
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src/main/resources"
NS = "realistic_ores"
ORE_DIR = RES / "data/realistic_ores/realistic_ores"
ASSETS = RES / "assets/realistic_ores"
DATA = RES / "data/realistic_ores"
FAMILIES = [
    "coal_measures", "ironstone", "copper_bloom", "tin_quartz", "brassroot",
    "redbed", "evaporite_beds", "gem_pipe", "hotstone", "black_shale",
]

MATERIALS = {
    "coal": ("bulk", "minecraft:coal", None, None),
    "iron": ("metal", "minecraft:iron_ingot", "minecraft:iron_nugget", "forge:molten_iron"),
    "nickel": ("metal", "chemlib:nickel_ingot", "chemlib:nickel_nugget", "forge:molten_nickel"),
    "copper": ("metal", "minecraft:copper_ingot", "tconstruct:copper_nugget", "forge:molten_copper"),
    "sulfur": ("bulk", "chemlib:sulfur", None, None),
    "gold": ("metal", "minecraft:gold_ingot", "minecraft:gold_nugget", "forge:molten_gold"),
    "tin": ("metal", "chemlib:tin_ingot", "chemlib:tin_nugget", "forge:molten_tin"),
    "quartz": ("bulk", "minecraft:quartz", None, None),
    "zinc": ("metal", "chemlib:zinc_ingot", "chemlib:zinc_nugget", "forge:molten_zinc"),
    "lead": ("metal", "chemlib:lead_ingot", "chemlib:lead_nugget", "forge:molten_lead"),
    "cadmium": ("metal", "chemlib:cadmium_ingot", "chemlib:cadmium_nugget", "forge:molten_cadmium"),
    "silver": ("metal", "chemlib:silver_ingot", "chemlib:silver_nugget", "forge:molten_silver"),
    "aluminum": ("metal", "chemlib:aluminum_ingot", "chemlib:aluminum_nugget", "forge:molten_aluminum"),
    "titanium": ("metal", "chemlib:titanium_ingot", "chemlib:titanium_nugget", "forge:molten_titanium"),
    "cobalt": ("metal", "chemlib:cobalt_ingot", "chemlib:cobalt_nugget", "forge:molten_cobalt"),
    "osmium": ("metal", "chemlib:osmium_ingot", "chemlib:osmium_nugget", "forge:molten_osmium"),
    "diamond": ("gem", "minecraft:diamond", "realistic_ores:diamond_chip", None),
    "emerald": ("gem", "minecraft:emerald", "realistic_ores:emerald_chip", None),
    "amethyst": ("gem", "minecraft:amethyst_shard", "realistic_ores:amethyst_chip", None),
    "uranium": ("metal", "chemlib:uranium_ingot", "chemlib:uranium_nugget", "forge:molten_uranium"),
    "thorium": ("metal", "chemlib:thorium_ingot", "chemlib:thorium_nugget", "forge:molten_thorium"),
    "redstone": ("bulk", "minecraft:redstone", None, None),
    "lapis": ("bulk", "minecraft:lapis_lazuli", None, None),
    "soul_sand": ("bulk", "minecraft:soul_sand", None, None),
    "rock_salt": ("bulk", "realistic_ores:rock_salt", None, None),
    "sodium_chloride": ("bulk", "chemlib:sodium_chloride", None, None),
    "saltpeter": ("bulk", "bloodmagic:saltpeter", None, None),
}

# (medium, acid or None, [(coproduct, grade)])
ROUTES = {
    "coal_measures": ("coal", [("andesite", None, []), ("blood_infused", "hydrochloric", [("iron", "trace")])]),
    "ironstone": ("iron", [("iron", None, [("nickel", "minor")]), ("steel", "hydrochloric", [("nickel", "minor")])]),
    "copper_bloom": ("copper", [("brass", None, [("sulfur", "major"), ("iron", "minor")]), ("steel", "sulfuric", [("sulfur", "major"), ("iron", "minor")]), ("nickel", "mixed", [("sulfur", "major"), ("iron", "minor"), ("gold", "precious")])]),
    "tin_quartz": ("tin", [("brass", None, [("quartz", "major")]), ("steel", "hydrochloric", [("quartz", "major")])]),
    "brassroot": ("zinc", [("brass", None, [("lead", "minor")]), ("steel", "sulfuric", [("lead", "minor"), ("cadmium", "trace")]), ("nickel", "nitric", [("lead", "minor"), ("cadmium", "trace"), ("silver", "precious")])]),
    "redbed": ("redstone", [("brass", None, [("copper", "major"), ("iron", "minor")]), ("steel", "hydrochloric", [("copper", "major"), ("iron", "minor")]), ("nickel", "mixed", [("copper", "major"), ("gold", "precious")])]),
    "evaporite_beds": ("rock_salt", [("andesite", None, [("sodium_chloride", "major")]), ("steel", "hydrochloric", [("sodium_chloride", "major"), ("saltpeter", "minor")])]),
    "gem_pipe": ("diamond", [("steel", None, [("emerald", "minor"), ("aluminum", "minor")]), ("titanium", "hydrochloric", [("amethyst", "major"), ("quartz", "minor"), ("aluminum", "minor")]), ("fluix", "hydrochloric", [("lapis", "major"), ("aluminum", "minor")])]),
    "hotstone": ("uranium", [("titanium", "sulfuric", [("thorium", "minor"), ("lead", "minor")]), ("nickel", "nitric", [("titanium", "major"), ("nickel", "minor"), ("cobalt", "trace"), ("iron", "minor")]), ("titanium", "mixed", [("osmium", "minor"), ("sulfur", "major")])]),
    "black_shale": ("soul_sand", [("blood_infused", None, [("sulfur", "minor")]), ("blood_infused", "hydrochloric", [("sulfur", "minor")]), ("fluix", "hydrochloric", [("redstone", "trace")])]),
}

BALLS = {"andesite": .80, "iron": .84, "brass": .87, "steel": .91, "nickel": .93, "titanium": .95, "blood_infused": .97, "fluix": .98}
GRADE = {"major": 1.0, "minor": .5, "trace": .2, "precious": .05}
ACIDS = {"sulfuric": "chemlib:sulfuric_acid_fluid", "hydrochloric": "chemlib:hydrochloric_acid_fluid", "nitric": "chemlib:nitric_acid_fluid"}
ASSAY_VARIANTS = {
    "gem_pipe": [
        {"name": "diamond", "materials": ["diamond"]},
        {"name": "emerald", "materials": ["emerald", "aluminum"]},
        {"name": "amethyst", "materials": ["amethyst", "quartz", "aluminum"]},
        {"name": "lazurite", "materials": ["lapis", "aluminum"]},
    ],
    "hotstone": [
        {"name": "fissile", "materials": ["uranium", "thorium", "lead"]},
        {"name": "structural", "materials": ["titanium", "nickel", "cobalt", "iron"]},
        {"name": "abyssal", "materials": ["osmium", "sulfur"]},
    ],
}


def write(path: Path, value: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + "\n", encoding="utf-8")


def reset(path: Path) -> None:
    if path.exists():
        shutil.rmtree(path)
    path.mkdir(parents=True)


def require_curated_item_texture(directory: Path, item: str) -> None:
    texture = directory / f"{item}.png"
    if not texture.is_file():
        raise RuntimeError(
            f"missing curated item texture {texture}; run "
            "java tools/DownsampleItemTextures.java --write")


def png(
        path: Path,
        palette: list[str] | float,
        morphology: str | int,
        shape: int | None = None,
        crushed: bool = False) -> None:
    if shape is None:
        shape = int(morphology)
        morphology = "disseminated"
    colors = (
        [tuple(int(value[index:index + 2], 16) for index in (1, 3, 5)) for value in palette]
        if isinstance(palette, list)
        else [tuple(round(channel * 255) for channel in colorsys.hsv_to_rgb(palette, .58, .9))]
    )
    morphology_seed = zlib.crc32(morphology.encode("utf-8"))
    rows = []
    for y in range(16):
        row = bytearray()
        for x in range(16):
            if crushed:
                envelope = 3 <= x <= 12 and 7 <= y <= 12
                visible = envelope and y >= 8 + abs(x - 7) // 4 and ((x * 5 + y * 3 + shape) % 7 != 0)
            elif morphology in ("seam", "banded"):
                center = 7 + ((x * 3 + morphology_seed + shape) % 5 - 2) // 2
                visible = 2 <= x <= 13 and abs(y - center) <= 2 and ((x + y + shape) % 6 != 0)
            elif morphology in ("vein", "branching"):
                center = 3 + ((x * 5 + morphology_seed + shape) % 9)
                visible = 2 <= x <= 13 and (abs(y - center) <= 1 or (x + y + shape) % 11 == 0)
            elif morphology == "crystalline":
                visible = 3 <= x <= 12 and 3 <= y <= 12 and ((x * 7 + y * 11 + morphology_seed + shape) % 5 <= 1)
            else:
                visible = 2 <= x <= 13 and 3 <= y <= 12 and ((x * 7 + y * 11 + morphology_seed + shape) % 5 != 0)
            color = colors[(x * 3 + y * 5 + shape) % len(colors)]
            row.extend((*color, 255 if visible else 0))
        rows.append(b"\0" + bytes(row))
    raw = b"".join(rows)
    def chunk(kind: bytes, payload: bytes) -> bytes:
        return struct.pack(">I", len(payload)) + kind + payload + struct.pack(">I", zlib.crc32(kind + payload))
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0)) + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))


def main() -> None:
    definitions = [json.loads(path.read_text()) for path in sorted(ORE_DIR.glob("*.json"))]
    by_family = {definition["id"]: definition for definition in definitions}
    if set(by_family) != set(FAMILIES):
        raise RuntimeError(f"family mismatch: missing={set(FAMILIES)-set(by_family)}, extra={set(by_family)-set(FAMILIES)}")

    lang_path = ASSETS / "lang/en_us.json"
    lang = json.loads(lang_path.read_text())
    for key in list(lang):
        if any(token in key for token in ("ore_chunk_", "small_ore_chunk_", "crushed_", "_concentrate", "_grinding_ball", "_chip")):
            del lang[key]
        elif key.startswith(f"block.{NS}.") and not any(
                preserved in key for preserved in ("oil_seep", "molten_titanium", "molten_thorium")):
            del lang[key]

    item_models = ASSETS / "models/item"
    item_textures = ASSETS / "textures/item"
    for pattern in ("ore_chunk_*.json", "crushed_*.json", "small_ore_chunk_*.json", "*_concentrate.json", "*_grinding_ball.json", "*_chip.json"):
        for path in item_models.glob(pattern): path.unlink()
    # Curated chunk, small-chunk, crushed-feed, and concentrate sprites are reduced
    # from committed high-resolution masters and must never be replaced here.
    for pattern in ("*_grinding_ball.png", "*_chip.png"):
        for path in item_textures.glob(pattern): path.unlink()

    recipes = DATA / "recipes"
    for directory in (recipes / "crafting/small_chunks", recipes / "crafting/ore_reassembly", recipes / "compat/create/crushing", recipes / "compat/create/milling/ore_chunks", recipes / "compat/create/separation", recipes / "compat/create/grinding_balls", recipes / "thermal/furnace", recipes / "thermal/blasting", recipes / "compat/tconstruct/melting", recipes / "compat/tconstruct/foundry", recipes / "crafting/gem_chips"):
        reset(directory)
    processing_dir = DATA / "processing_definitions"
    reset(processing_dir)
    reset(DATA / "tags/blocks/deposit_ore_blocks")
    reset(DATA / "loot_tables/blocks")
    ev_modifiers = RES / "defaultresources/excavated_variants/excavated_variants/modifiers/realistic_ores"
    reset(ev_modifiers)
    ev_modifier_root = ev_modifiers.parent
    # Replace EV's broad default: hosted ore blocks retain mining classification, but
    # processing-facing ore item tags exclude the Realistic Ores family set.
    write(ev_modifier_root / "tag_attachment.json5", {
        "filter": "*",
        "tags": ["minecraft:blocks/mineable/pickaxe", "forge:blocks/ores", "c:blocks/ores"],
    })
    write(ev_modifier_root / "non_realistic_ore_item_tags.json5", {
        "filter": {
            "type": "not",
            "filter": {
                "type": "or",
                "filters": [f"ore:{family}" for family in FAMILIES],
            },
        },
        "tags": ["forge:items/ores", "c:items/ores"],
    })

    all_chunks, all_small, all_crushed = [], [], []
    for index, family in enumerate(FAMILIES):
        definition = by_family[family]
        chunk, small, crushed = f"ore_chunk_{family}", f"small_ore_chunk_{family}", f"crushed_{family}"
        all_chunks.append(f"{NS}:{chunk}"); all_small.append(f"{NS}:{small}"); all_crushed.append(f"{NS}:{crushed}")
        for item, suffix, label in ((chunk, "chunk", "Ore Chunk"), (crushed, "crushed", "Crushed Feed")):
            write(item_models / f"{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{item}"}})
            require_curated_item_texture(item_textures, item)
            lang[f"item.{NS}.{item}"] = f"{definition['display_name'].removesuffix(' Deposit')} {label}"
        sample = f"surface_sample_{family}"
        family_name = definition["display_name"].removesuffix(" Deposit")
        lang[f"block.{NS}.{sample}"] = f"Surface Sample: {family_name}"
        write(item_models / f"{small}.json", {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": f"{NS}:item/{small}"},
        })
        require_curated_item_texture(item_textures, small)
        lang[f"item.{NS}.{small}"] = f"Small {definition['display_name'].removesuffix(' Deposit')} Chunk"
        write(DATA / f"loot_tables/blocks/{sample}.json", {
            "type": "minecraft:block",
            "pools": [{
                "rolls": 1.0,
                "entries": [{"type": "minecraft:item", "name": f"{NS}:{small}"}],
                "conditions": [{"condition": "minecraft:survives_explosion"}],
            }],
        })
        write(recipes / f"crafting/small_chunks/{family}.json", {"type": "minecraft:crafting_shapeless", "ingredients": [{"item": f"{NS}:{small}"}] * 9, "result": {"item": f"{NS}:{chunk}"}})
        write(recipes / f"compat/create/crushing/ore_chunks/{family}.json", {"type": "create:crushing", "conditions": [{"type": "forge:mod_loaded", "modid": "create"}], "ingredients": [{"item": f"{NS}:{chunk}"}], "processingTime": 400, "results": [{"item": f"{NS}:{crushed}", "count": 3}]})
        write(recipes / f"compat/create/milling/ore_chunks/{family}.json", {"type": "create:milling", "conditions": [{"type": "forge:mod_loaded", "modid": "create"}], "ingredients": [{"item": f"{NS}:{chunk}"}], "processingTime": 400, "results": [{"item": f"{NS}:{crushed}", "count": 2}]})
        hosted = []
        for variant in definition["variants"]:
            block, host = variant["block_id"], variant["copy_properties_from"]
            lang[f"block.{NS}.{block}"] = (
                f"Deepslate {family_name}" if variant["host"] == "deepslate" else family_name)
            hosted.append(f"{NS}:{block}")
            silk = {"condition": "minecraft:match_tool", "predicate": {"enchantments": [{"enchantment": "minecraft:silk_touch", "levels": {"min": 1}}]}}
            write(DATA / f"loot_tables/blocks/{block}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": [{"type": "minecraft:item", "name": f"{NS}:{block}", "conditions": [silk]}, {"type": "minecraft:item", "name": f"{NS}:{chunk}"}]}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
            write(recipes / f"compat/create/crushing/{block}.json", {"type": "create:crushing", "conditions": [{"type": "forge:mod_loaded", "modid": "create"}], "ingredients": [{"type": "forge:nbt", "item": f"{NS}:{block}"}], "processingTime": 250, "results": [{"item": f"{NS}:{chunk}"}, {"item": host}]})
            write(recipes / f"crafting/ore_reassembly/{block}.json", {"type": "minecraft:crafting_shapeless", "ingredients": [{"item": f"{NS}:{chunk}"}, {"item": host}], "result": {"item": f"{NS}:{block}"}})
        write(DATA / f"tags/blocks/deposit_ore_blocks/{family}.json", {"replace": False, "values": hosted})
        write(ev_modifiers / f"{family}.json5", {
            "filter": f"ore:{family}",
            "tags": ["minecraft:blocks/mineable/pickaxe", f"realistic_ores:blocks/deposit_ore_blocks/{family}", "realistic_ores:blocks/deposit_ore_blocks"],
        })

        primary, routes = ROUTES[family]
        processing = {"family": family, "primary": primary, "input_count": 4, "routes": []}
        if family in ASSAY_VARIANTS:
            processing["assay_variants"] = ASSAY_VARIANTS[family]
        for route_index, (ball, acid, coproducts) in enumerate(routes, 1):
            fluid_spec = [{"fluid": "minecraft:water", "amount": 500}] if acid is None else (
                [{"fluid": ACIDS[acid], "amount": 250}, {"fluid": "minecraft:water", "amount": 250}] if acid != "mixed" else
                [{"fluid": ACIDS["hydrochloric"], "amount": 250}, {"fluid": ACIDS["nitric"], "amount": 250}]
            )
            processing["routes"].append({"medium": ball, "fluids": fluid_spec, "coproducts": [{"material": material, "grade": grade, "chance": GRADE[grade]} for material, grade in coproducts], "ball_return_chance": BALLS[ball]})
            results = [{"item": f"{NS}:{primary}_concentrate", "count": 4}]
            results += [{"item": f"{NS}:{material}_concentrate", "chance": GRADE[grade]} for material, grade in coproducts]
            results.append({"item": f"{NS}:{ball}_grinding_ball", "chance": BALLS[ball]})
            ingredients = [{"item": f"{NS}:{crushed}"}] * 4 + [{"item": f"{NS}:{ball}_grinding_ball"}] + fluid_spec
            write(recipes / f"compat/create/separation/{family}_{route_index}_{ball}.json", {"type": "create:mixing", "conditions": [{"type": "forge:mod_loaded", "modid": "create"}], "ingredients": ingredients, "results": results, "processingTime": 300})
        write(processing_dir / f"{family}.json", processing)

        kind, output, fraction, fluid = MATERIALS[primary]
        if kind in ("metal", "gem", "bulk"):
            cooked = fraction if fraction is not None else output
            for stage, item, count in (("chunk", chunk, 2), ("crushed", crushed, 1)):
                write(recipes / f"thermal/furnace/{family}_{stage}.json", {"type": "minecraft:smelting", "ingredient": {"item": f"{NS}:{item}"}, "result": {"item": cooked, "count": count}, "experience": .1, "cookingtime": 240})
                write(recipes / f"thermal/blasting/{family}_{stage}.json", {"type": "minecraft:blasting", "ingredient": {"item": f"{NS}:{item}"}, "result": {"item": cooked, "count": count}, "experience": .1, "cookingtime": 120})
        if kind == "metal" and fluid:
            fluid_result = {"tag": fluid, "amount": 20} if fluid.startswith("forge:") else {"fluid": fluid, "amount": 20}
            foundry_result = {**fluid_result, "amount": 30}
            crushed_result = {**fluid_result, "amount": 10}
            crushed_foundry_result = {**fluid_result, "amount": 15}
            grade_amounts = {"major": 10, "minor": 5, "trace": 2, "precious": 1}
            byproducts = []
            seen_byproducts = set()
            for _, _, coproducts in routes:
                for coproduct, grade in coproducts:
                    coproduct_fluid = MATERIALS[coproduct][3]
                    if MATERIALS[coproduct][0] != "metal" or coproduct_fluid is None or coproduct in seen_byproducts: continue
                    seen_byproducts.add(coproduct)
                    byproducts.append(({"tag": coproduct_fluid, "amount": grade_amounts[grade], "rate": "metal"}
                                       if coproduct_fluid.startswith("forge:") else
                                       {"fluid": coproduct_fluid, "amount": grade_amounts[grade], "rate": "metal"}))
            write(recipes / f"compat/tconstruct/melting/{family}_chunk.json", {"type": "tconstruct:melting", "conditions": [{"type": "forge:mod_loaded", "modid": "tconstruct"}], "ingredient": {"item": f"{NS}:{chunk}"}, "result": fluid_result, "temperature": 950, "time": 120})
            write(recipes / f"compat/tconstruct/melting/{family}_crushed.json", {"type": "tconstruct:melting", "conditions": [{"type": "forge:mod_loaded", "modid": "tconstruct"}], "ingredient": {"item": f"{NS}:{crushed}"}, "result": crushed_result, "temperature": 950, "time": 120})
            chunk_foundry = {"type": "tconstruct:ore_melting", "conditions": [{"type": "forge:mod_loaded", "modid": "tconstruct"}], "ingredient": {"item": f"{NS}:{chunk}"}, "result": foundry_result, "rate": "metal", "temperature": 950, "time": 120}
            crushed_foundry = {"type": "tconstruct:ore_melting", "conditions": [{"type": "forge:mod_loaded", "modid": "tconstruct"}], "ingredient": {"item": f"{NS}:{crushed}"}, "result": crushed_foundry_result, "rate": "metal", "temperature": 950, "time": 120}
            if byproducts:
                chunk_foundry["byproducts"] = byproducts
                crushed_foundry["byproducts"] = byproducts
            write(recipes / f"compat/tconstruct/foundry/{family}_chunk.json", chunk_foundry)
            write(recipes / f"compat/tconstruct/foundry/{family}_crushed.json", crushed_foundry)

    write(DATA / "loot_tables/blocks/oil_seep.json", {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1.0,
            "entries": [{"type": "minecraft:item", "name": f"{NS}:oil_seep"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}],
        }],
    })
    write(DATA / "tags/items/deposit_chunks.json", {"replace": False, "values": all_chunks})
    obsolete_chunk_tag = DATA / "tags/items/ore_chunks.json"
    if obsolete_chunk_tag.exists(): obsolete_chunk_tag.unlink()
    write(DATA / "tags/items/small_ore_chunks.json", {"replace": False, "values": all_small})
    write(DATA / "tags/items/crushed_feeds.json", {"replace": False, "values": all_crushed})
    reset(DATA / "tags/items/deposit_chunks")
    for family in FAMILIES:
        write(DATA / f"tags/items/deposit_chunks/{family}.json", {"replace": False, "values": [f"{NS}:ore_chunk_{family}"]})
    write(DATA / "tags/blocks/deposit_ore_blocks.json", {"replace": False, "values": [f"#{NS}:deposit_ore_blocks/{family}" for family in FAMILIES]})

    for index, material in enumerate(MATERIALS):
        item = f"{material}_concentrate"
        write(item_models / f"{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{item}"}})
        require_curated_item_texture(item_textures, item)
        lang[f"item.{NS}.{item}"] = f"{material.replace('_', ' ').title()} Concentrate"
        kind, output, fraction, fluid = MATERIALS[material]
        if kind in ("metal", "gem", "bulk"):
            cooked = fraction if fraction is not None else output
            write(recipes / f"thermal/furnace/concentrate_{material}.json", {"type": "minecraft:smelting", "ingredient": {"item": f"{NS}:{item}"}, "result": {"item": cooked, "count": 2}, "experience": .2, "cookingtime": 240})
            write(recipes / f"thermal/blasting/concentrate_{material}.json", {"type": "minecraft:blasting", "ingredient": {"item": f"{NS}:{item}"}, "result": {"item": cooked, "count": 2}, "experience": .2, "cookingtime": 120})
        if kind == "metal" and fluid:
            result = ({"tag": fluid, "amount": 30} if fluid.startswith("forge:") else {"fluid": fluid, "amount": 30})
            melting_result = ({"tag": fluid, "amount": 20} if fluid.startswith("forge:") else {"fluid": fluid, "amount": 20})
            write(recipes / f"compat/tconstruct/melting/concentrate_{material}.json", {"type": "tconstruct:melting", "conditions": [{"type": "forge:mod_loaded", "modid": "tconstruct"}], "ingredient": {"item": f"{NS}:{item}"}, "result": melting_result, "temperature": 950, "time": 120})
            write(recipes / f"compat/tconstruct/foundry/concentrate_{material}.json", {"type": "tconstruct:ore_melting", "conditions": [{"type": "forge:mod_loaded", "modid": "tconstruct"}], "ingredient": {"item": f"{NS}:{item}"}, "result": result, "rate": "metal", "temperature": 950, "time": 120})

    ball_inputs = {"andesite": "create:andesite_alloy", "iron": "#forge:ingots/iron", "brass": "#forge:ingots/brass", "steel": "#forge:ingots/steel", "nickel": "#forge:ingots/nickel", "titanium": "chemlib:titanium_ingot"}
    for index, (ball, chance) in enumerate(BALLS.items()):
        item = f"{ball}_grinding_ball"
        write(item_models / f"{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{item}"}})
        png(item_textures / f"{item}.png", (index * .11 + .54) % 1, index + 400)
        lang[f"item.{NS}.{item}"] = f"{ball.replace('_', ' ').title()} Grinding Ball"
        if ball in ball_inputs:
            source = ball_inputs[ball]
            ingredient = {"tag": source[1:]} if source.startswith("#") else {"item": source}
            write(recipes / f"compat/create/grinding_balls/{ball}.json", {"type": "create:compacting", "conditions": [{"type": "forge:mod_loaded", "modid": "create"}], "ingredients": [ingredient] * 4, "results": [{"item": f"{NS}:{item}"}]})
    for ball, proof in (("blood_infused", "bloodmagic:demonslate"), ("fluix", "ae2:fluix_crystal")):
        proof_mod = proof.split(":", 1)[0]
        write(recipes / f"compat/create/grinding_balls/{ball}.json", {"type": "create:compacting", "conditions": [{"type": "forge:mod_loaded", "modid": "create"}, {"type": "forge:mod_loaded", "modid": proof_mod}], "ingredients": [{"item": f"{NS}:steel_grinding_ball"}, {"item": proof}], "results": [{"item": f"{NS}:{ball}_grinding_ball"}]})

    for index, (gem, output) in enumerate((("diamond", "minecraft:diamond"), ("emerald", "minecraft:emerald"), ("amethyst", "minecraft:amethyst_shard"))):
        item = f"{gem}_chip"
        write(item_models / f"{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{item}"}})
        png(item_textures / f"{item}.png", (index * .19 + .48) % 1, index + 500)
        lang[f"item.{NS}.{item}"] = f"{gem.title()} Chip"
        write(recipes / f"crafting/gem_chips/{gem}_assemble.json", {"type": "minecraft:crafting_shapeless", "ingredients": [{"item": f"{NS}:{item}"}] * 9, "result": {"item": output}})

    # First-contact utility: these deposits teach their promise without requiring the
    # complete separation chain. Expert processing still yields substantially more.
    write(item_models / "rock_salt.json", {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": f"{NS}:item/rock_salt_concentrate"},
    })
    lang[f"item.{NS}.rock_salt"] = "Rock Salt"
    write(recipes / "crafting/immediate/evaporite_rock_salt.json", {
        "type": "minecraft:crafting_shapeless",
        "ingredients": [{"item": f"{NS}:ore_chunk_evaporite_beds"}],
        "result": {"item": f"{NS}:rock_salt", "count": 4},
    })
    write(item_models / "rock_salt.json", {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": f"{NS}:item/rock_salt_concentrate"},
    })
    lang[f"item.{NS}.rock_salt"] = "Rock Salt"
    write(recipes / "crafting/immediate/black_shale_soul_sand.json", {
        "type": "minecraft:crafting_shapeless",
        "ingredients": [{"item": f"{NS}:ore_chunk_black_shale"}],
        "result": {"item": "minecraft:soul_sand"},
    })
    write(recipes / "crafting/immediate/gem_pipe_chip.json", {
        "type": "minecraft:crafting_shapeless",
        "ingredients": [{"item": f"{NS}:ore_chunk_gem_pipe"}],
        "result": {"item": f"{NS}:diamond_chip"},
    })
    write(recipes / "crafting/immediate/hotstone_magma.json", {
        "type": "minecraft:crafting_shaped",
        "pattern": ["HH", "HH"],
        "key": {"H": {"item": f"{NS}:ore_chunk_hotstone"}},
        "result": {"item": "minecraft:magma_block"},
    })
    for namespace, path in (("forge", "salt"), ("forge", "salts"), ("c", "salts"), ("c", "foods/salt")):
        write(RES / f"data/{namespace}/tags/items/{path}.json", {
            "replace": False,
            "values": [f"{NS}:rock_salt"],
        })
    write(RES / "data/minecraft/tags/blocks/soul_fire_base_blocks.json", {
        "replace": False,
        "values": [f"{NS}:black_shale", f"{NS}:deepslate_black_shale"],
    })

    for index, material in enumerate(("titanium", "thorium")):
        bucket = f"molten_{material}_bucket"
        write(item_models / f"{bucket}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{bucket}"}})
        png(item_textures / f"{bucket}.png", .55 if material == "titanium" else .20, 600 + index)
        lang[f"item.{NS}.{bucket}"] = f"Molten {material.title()} Bucket"
        lang[f"block.{NS}.molten_{material}"] = f"Molten {material.title()}"
        write(RES / f"data/forge/tags/fluids/molten_{material}.json", {
            "replace": False,
            "values": [f"{NS}:molten_{material}", f"{NS}:flowing_molten_{material}"],
        })

    # Hosted blocks are mining data, never processing-facing ore items.
    forge_item_tags = RES / "data/forge/tags/items"
    if forge_item_tags.exists():
        for path in forge_item_tags.glob("ores*.json"): path.unlink()
        ores_dir = forge_item_tags / "ores"
        if ores_dir.exists(): shutil.rmtree(ores_dir)
    hosted_item_tags = DATA / "tags/items/deposit_ore_blocks"
    if hosted_item_tags.exists(): shutil.rmtree(hosted_item_tags)
    all_hosted_item_tag = DATA / "tags/items/deposit_ore_blocks.json"
    if all_hosted_item_tag.exists(): all_hosted_item_tag.unlink()

    # Cross-mod radiation rules consume these stable categories without knowing block layouts.
    for element, family in (("uranium", "hotstone"), ("thorium", "hotstone")):
        write(DATA / f"tags/items/radioactive_forms/{element}/small_chunks.json", {"replace": False, "values": [f"{NS}:small_ore_chunk_{family}"]})
        write(DATA / f"tags/items/radioactive_forms/{element}/full_chunks.json", {"replace": False, "values": [f"{NS}:ore_chunk_{family}"]})
        write(DATA / f"tags/items/radioactive_forms/{element}/crushed_feed.json", {"replace": False, "values": [f"{NS}:crushed_{family}"]})
        write(DATA / f"tags/items/radioactive_forms/{element}/concentrate.json", {"replace": False, "values": [f"{NS}:{element}_concentrate"]})
        write(DATA / f"tags/items/radioactive_forms/{element}/final_forms.json", {"replace": False, "values": [f"chemlib:{element}_ingot", f"chemlib:{element}_nugget"]})
        blocks = [f"{NS}:{variant['block_id']}" for variant in by_family[family]["variants"]]
        write(DATA / f"tags/blocks/radioactive_forms/{element}/hosted_ore_blocks.json", {"replace": False, "values": blocks})

    write(lang_path, dict(sorted(lang.items())))


if __name__ == "__main__":
    main()
