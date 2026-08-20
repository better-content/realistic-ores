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
    "coal_measures", "ironstone", "copper_sulfide", "tin", "zinc", "lead_zinc_vein",
    "quartz_vein", "bauxite_laterite", "nickel_sulfide", "osmiridium_lava_sulfide",
    "tin_tungsten_greisen", "titanium_iron_oxide", "kimberlite_pipe",
    "emerald_schist_beryl", "amethyst_beryl_pegmatite", "uranium_ore", "thorium_ore",
    "cupriferous_redbed_redstone_vein", "lazurite_vein", "phosphate_rock",
    "soul_bearing_black_shale_soulstone_vein", "sulfur_bearing_pyrite_ore",
    "gold_quartz_vein",
]

MATERIALS = {
    "coal": ("bulk", "minecraft:coal", None, None),
    "carbon": ("bulk", "chemlib:carbon", None, None),
    "iron": ("metal", "minecraft:iron_ingot", "minecraft:iron_nugget", "forge:molten_iron"),
    "nickel": ("metal", "chemlib:nickel_ingot", "chemlib:nickel_nugget", "forge:molten_nickel"),
    "chromium": ("metal", "chemlib:chromium_ingot", "chemlib:chromium_nugget", "forge:molten_chromium"),
    "copper": ("metal", "minecraft:copper_ingot", "tconstruct:copper_nugget", "forge:molten_copper"),
    "sulfur": ("bulk", "chemlib:sulfur", None, None),
    "gold": ("metal", "minecraft:gold_ingot", "minecraft:gold_nugget", "forge:molten_gold"),
    "tin": ("metal", "chemlib:tin_ingot", "chemlib:tin_nugget", "forge:molten_tin"),
    "quartz": ("bulk", "minecraft:quartz", None, "tconstruct:molten_quartz"),
    "tungsten": ("metal", "chemlib:tungsten_ingot", "chemlib:tungsten_nugget", "forge:molten_tungsten"),
    "zinc": ("metal", "chemlib:zinc_ingot", "chemlib:zinc_nugget", "forge:molten_zinc"),
    "lead": ("metal", "chemlib:lead_ingot", "chemlib:lead_nugget", "forge:molten_lead"),
    "cadmium": ("metal", "chemlib:cadmium_ingot", "chemlib:cadmium_nugget", "forge:molten_cadmium"),
    "silver": ("metal", "chemlib:silver_ingot", "chemlib:silver_nugget", "forge:molten_silver"),
    "silicon": ("bulk", "chemlib:silicon", None, None),
    "aluminum": ("metal", "chemlib:aluminum_ingot", "chemlib:aluminum_nugget", "forge:molten_aluminum"),
    "titanium": ("metal", "chemlib:titanium_ingot", "chemlib:titanium_nugget", "forge:molten_titanium"),
    "gallium": ("metal", "chemlib:gallium_ingot", "chemlib:gallium_nugget", None),
    "cobalt": ("metal", "chemlib:cobalt_ingot", "chemlib:cobalt_nugget", "forge:molten_cobalt"),
    "platinum": ("metal", "chemlib:platinum_ingot", "chemlib:platinum_nugget", "forge:molten_platinum"),
    "osmium": ("metal", "chemlib:osmium_ingot", "chemlib:osmium_nugget", "forge:molten_osmium"),
    "iridium": ("metal", "chemlib:iridium_ingot", "chemlib:iridium_nugget", "forge:molten_iridium"),
    "tantalum": ("metal", "chemlib:tantalum_ingot", "chemlib:tantalum_nugget", "forge:molten_tantalum"),
    "magnesium": ("metal", "chemlib:magnesium_ingot", "chemlib:magnesium_nugget", "forge:molten_magnesium"),
    "diamond": ("gem", "minecraft:diamond", "realistic_ores:diamond_chip", "tconstruct:molten_diamond"),
    "emerald": ("gem", "minecraft:emerald", "realistic_ores:emerald_chip", "tconstruct:molten_emerald"),
    "beryl": ("bulk", "chemlib:beryl", None, None),
    "beryllium": ("metal", "chemlib:beryllium_ingot", "chemlib:beryllium_nugget", "forge:molten_beryllium"),
    "amethyst": ("gem", "minecraft:amethyst_shard", "realistic_ores:amethyst_chip", "tconstruct:molten_amethyst"),
    "uranium": ("metal", "chemlib:uranium_ingot", "chemlib:uranium_nugget", "forge:molten_uranium"),
    "thorium": ("metal", "chemlib:thorium_ingot", "chemlib:thorium_nugget", "forge:molten_thorium"),
    "calcium": ("metal", "chemlib:calcium_ingot", "chemlib:calcium_nugget", None),
    "redstone": ("bulk", "minecraft:redstone", None, None),
    "lapis": ("bulk", "minecraft:lapis_lazuli", None, None),
    "sodium": ("metal", "chemlib:sodium_ingot", "chemlib:sodium_nugget", None),
    "phosphate": ("bulk", "chemlib:phosphate", None, None),
    "soul_sand": ("bulk", "minecraft:soul_sand", None, None),
}

# (medium, acid or None, [(coproduct, grade)])
ROUTES = {
    "coal_measures": ("coal", [("andesite", None, [("carbon", "major")]), ("blood_infused", "hydrochloric", [("carbon", "major"), ("iron", "trace")])]),
    "ironstone": ("iron", [("iron", None, [("nickel", "minor")]), ("steel", "hydrochloric", [("chromium", "trace")]), ("nickel", "nitric", [("nickel", "minor"), ("chromium", "trace")])]),
    "copper_sulfide": ("copper", [("brass", None, [("sulfur", "major"), ("iron", "minor")]), ("steel", "sulfuric", [("sulfur", "major"), ("iron", "minor")]), ("nickel", "mixed", [("sulfur", "major"), ("iron", "minor"), ("gold", "precious")])]),
    "tin": ("tin", [("brass", None, [("quartz", "major")]), ("steel", "hydrochloric", [("quartz", "major"), ("tungsten", "trace")]), ("titanium", "nitric", [("tungsten", "trace")])]),
    "zinc": ("zinc", [("brass", None, [("lead", "minor")]), ("steel", "sulfuric", [("lead", "minor"), ("cadmium", "trace")]), ("nickel", "nitric", [("cadmium", "trace")])]),
    "lead_zinc_vein": ("lead", [("brass", None, [("zinc", "major")]), ("steel", "sulfuric", [("zinc", "major")]), ("nickel", "nitric", [("zinc", "major"), ("silver", "precious")])]),
    "quartz_vein": ("quartz", [("andesite", None, [("silicon", "major")]), ("brass", "hydrochloric", [("silicon", "major"), ("copper", "trace")]), ("nickel", "mixed", [("silicon", "major"), ("copper", "trace"), ("gold", "precious")])]),
    "bauxite_laterite": ("aluminum", [("steel", "sulfuric", [("nickel", "minor")]), ("titanium", "hydrochloric", [("titanium", "minor"), ("gallium", "trace")])]),
    "nickel_sulfide": ("nickel", [("iron", None, [("sulfur", "major"), ("iron", "minor")]), ("nickel", "sulfuric", [("cobalt", "trace")]), ("titanium", "mixed", [("cobalt", "trace"), ("platinum", "precious")])]),
    "osmiridium_lava_sulfide": ("osmium", [("nickel", None, [("sulfur", "major")]), ("titanium", "nitric", [("iridium", "minor")]), ("titanium", "mixed", [("iridium", "minor"), ("platinum", "precious")])]),
    "tin_tungsten_greisen": ("tungsten", [("brass", None, [("tin", "major"), ("quartz", "major")]), ("steel", "hydrochloric", [("tin", "major"), ("quartz", "major")]), ("titanium", "nitric", [("tin", "major"), ("tantalum", "trace")])]),
    "titanium_iron_oxide": ("titanium", [("iron", None, [("iron", "major")]), ("steel", "hydrochloric", [("iron", "major"), ("chromium", "trace")]), ("titanium", "sulfuric", [("iron", "major"), ("chromium", "trace")])]),
    "kimberlite_pipe": ("diamond", [("steel", None, [("carbon", "major"), ("magnesium", "minor")]), ("blood_infused", "hydrochloric", [("carbon", "major")]), ("fluix", "hydrochloric", [("magnesium", "minor")])]),
    "emerald_schist_beryl": ("emerald", [("steel", None, [("beryl", "major")]), ("titanium", "hydrochloric", [("beryllium", "minor"), ("silicon", "trace")]), ("fluix", "hydrochloric", [("aluminum", "minor"), ("silicon", "trace")])]),
    "amethyst_beryl_pegmatite": ("amethyst", [("steel", None, [("aluminum", "major"), ("quartz", "minor")]), ("titanium", "hydrochloric", [("beryllium", "minor"), ("quartz", "minor")]), ("fluix", "hydrochloric", [("aluminum", "major"), ("beryllium", "minor")])]),
    "uranium_ore": ("uranium", [("titanium", "sulfuric", [("lead", "minor"), ("calcium", "minor"), ("thorium", "trace")]), ("nickel", "nitric", [("lead", "minor"), ("thorium", "trace")])]),
    "thorium_ore": ("thorium", [("titanium", "sulfuric", [("lead", "minor"), ("uranium", "trace")]), ("nickel", "nitric", [("lead", "minor"), ("uranium", "trace")])]),
    "cupriferous_redbed_redstone_vein": ("redstone", [("brass", None, [("copper", "major"), ("iron", "minor")]), ("steel", "hydrochloric", [("copper", "major"), ("iron", "minor")]), ("nickel", "mixed", [("copper", "major"), ("gold", "precious")])]),
    "lazurite_vein": ("lapis", [("andesite", None, [("sodium", "minor")]), ("steel", "hydrochloric", [("aluminum", "minor"), ("silicon", "trace")]), ("fluix", "hydrochloric", [("sodium", "minor"), ("aluminum", "minor"), ("silicon", "trace")])]),
    "phosphate_rock": ("phosphate", [("iron", None, [("calcium", "major")]), ("steel", "sulfuric", [("calcium", "major")])]),
    "soul_bearing_black_shale_soulstone_vein": ("soul_sand", [("blood_infused", None, [("carbon", "major")]), ("blood_infused", "hydrochloric", [("carbon", "major"), ("sulfur", "minor")]), ("fluix", "hydrochloric", [("redstone", "trace")])]),
    "sulfur_bearing_pyrite_ore": ("sulfur", [("iron", None, [("iron", "major")]), ("brass", "sulfuric", [("iron", "major"), ("copper", "minor")]), ("nickel", "mixed", [("copper", "minor"), ("gold", "precious")])]),
    "gold_quartz_vein": ("gold", [("nickel", "mixed", [("quartz", "major"), ("copper", "trace")]), ("titanium", "mixed", [("quartz", "major"), ("silver", "minor"), ("copper", "trace")])]),
}

BALLS = {"andesite": .80, "iron": .84, "brass": .87, "steel": .91, "nickel": .93, "titanium": .95, "blood_infused": .97, "fluix": .98}
GRADE = {"major": 1.0, "minor": .5, "trace": .2, "precious": .05}
ACIDS = {"sulfuric": "chemlib:sulfuric_acid_fluid", "hydrochloric": "chemlib:hydrochloric_acid_fluid", "nitric": "chemlib:nitric_acid_fluid"}


def write(path: Path, value: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + "\n", encoding="utf-8")


def reset(path: Path) -> None:
    if path.exists():
        shutil.rmtree(path)
    path.mkdir(parents=True)


def png(path: Path, hue: float, shape: int) -> None:
    rgb = tuple(round(channel * 255) for channel in colorsys.hsv_to_rgb(hue, .58, .9))
    rows = []
    for y in range(16):
        row = bytearray()
        for x in range(16):
            visible = 2 <= x <= 13 and 3 <= y <= 12 and ((x * 7 + y * 11 + shape) % 5 != 0)
            shade = .58 + ((x + y + shape) % 4) * .12
            row.extend((*[min(255, round(c * shade)) for c in rgb], 255 if visible else 0))
        rows.append(b"\0" + bytes(row))
    raw = b"".join(rows)
    def chunk(kind: bytes, payload: bytes) -> bytes:
        return struct.pack(">I", len(payload)) + kind + payload + struct.pack(">I", zlib.crc32(kind + payload))
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0)) + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))


def ensure_gold_definition() -> None:
    target = ORE_DIR / "gold_quartz_vein.json"
    if not target.exists():
        quartz = json.loads((ORE_DIR / "quartz_vein.json").read_text())
        text = json.dumps(quartz).replace("quartz_vein", "gold_quartz_vein")
        gold = json.loads(text)
        gold["id"] = "gold_quartz_vein"
        gold["display_name"] = "Gold-Quartz Vein Deposit"
        write(target, gold)
    for directory in (ASSETS / "textures/block", ASSETS / "models/block"):
        for source in directory.glob("*quartz_vein*"):
            if "gold_quartz" in source.name:
                continue
            shutil.copy2(source, directory / source.name.replace("quartz_vein", "gold_quartz_vein"))
    # Models copied above still reference quartz textures.
    for model in (ASSETS / "models/block").glob("*gold_quartz_vein*.json"):
        model.write_text(model.read_text().replace("quartz_vein", "gold_quartz_vein"))
    for host in ("gold_quartz_vein", "deepslate_gold_quartz_vein"):
        source = ASSETS / "blockstates" / host.replace("gold_", "")
        if source.with_suffix(".json").exists():
            payload = source.with_suffix(".json").read_text().replace("quartz_vein", "gold_quartz_vein")
            (ASSETS / "blockstates" / f"{host}.json").write_text(payload)
        source_item = ASSETS / "models/item" / f"{host.replace('gold_', '')}.json"
        if source_item.exists():
            (ASSETS / "models/item" / f"{host}.json").write_text(source_item.read_text().replace("quartz_vein", "gold_quartz_vein"))
    for host in ("stone", "deepslate"):
        source_name = f"quartz_vein_{host}.json"
        target_name = f"gold_quartz_vein_{host}.json"
        generation_source = DATA / "realistic_ore_generation" / source_name
        if not generation_source.exists():
            continue
        generation = json.loads(generation_source.read_text())
        generation["ore_id"] = "gold_quartz_vein"
        generation["vein_size"] = 8
        write(DATA / "realistic_ore_generation" / target_name, generation)
        configured_source = DATA / "worldgen/configured_feature" / source_name
        configured = json.loads(configured_source.read_text().replace("quartz_vein", "gold_quartz_vein"))
        configured["config"]["size"] = 8
        write(DATA / "worldgen/configured_feature" / target_name, configured)
        placed_source = DATA / "worldgen/placed_feature" / source_name
        placed = json.loads(placed_source.read_text().replace("quartz_vein", "gold_quartz_vein"))
        placed["placement"].insert(1, {"type": "minecraft:rarity_filter", "chance": 4})
        write(DATA / "worldgen/placed_feature" / target_name, placed)
        biome_source = DATA / "forge/biome_modifier" / f"add_{source_name}"
        if biome_source.exists():
            (DATA / "forge/biome_modifier" / f"add_{target_name}").write_text(
                biome_source.read_text().replace("quartz_vein", "gold_quartz_vein"))


def main() -> None:
    ensure_gold_definition()
    definitions = [json.loads(path.read_text()) for path in sorted(ORE_DIR.glob("*.json"))]
    by_family = {definition["id"]: definition for definition in definitions}
    if set(by_family) != set(FAMILIES):
        raise RuntimeError(f"family mismatch: missing={set(FAMILIES)-set(by_family)}, extra={set(by_family)-set(FAMILIES)}")

    lang_path = ASSETS / "lang/en_us.json"
    lang = json.loads(lang_path.read_text())
    for key in list(lang):
        if any(token in key for token in ("ore_chunk_", "small_ore_chunk_", "crushed_", "_concentrate", "_grinding_ball", "_chip")):
            del lang[key]

    item_models = ASSETS / "models/item"
    item_textures = ASSETS / "textures/item"
    for pattern in ("ore_chunk_*.json", "crushed_*.json", "small_ore_chunk_*.json", "*_concentrate.json", "*_grinding_ball.json", "*_chip.json"):
        for path in item_models.glob(pattern): path.unlink()
    for pattern in ("ore_chunk_*.png", "crushed_*.png", "small_ore_chunk_*.png", "*_concentrate.png", "*_grinding_ball.png", "*_chip.png"):
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
            png(item_textures / f"{item}.png", (index * .071) % 1, index + (0 if suffix == "chunk" else 91))
            lang[f"item.{NS}.{item}"] = f"{definition['display_name'].removesuffix(' Deposit')} {label}"
        sample = f"surface_sample_{family}"
        write(item_models / f"{small}.json", {"parent": f"{NS}:block/{sample}_2"})
        lang[f"item.{NS}.{small}"] = f"Small {definition['display_name'].removesuffix(' Deposit')} Chunk"
        write(recipes / f"crafting/small_chunks/{family}.json", {"type": "minecraft:crafting_shapeless", "ingredients": [{"item": f"{NS}:{small}"}] * 9, "result": {"item": f"{NS}:{chunk}"}})
        write(recipes / f"compat/create/crushing/ore_chunks/{family}.json", {"type": "create:crushing", "conditions": [{"type": "forge:mod_loaded", "modid": "create"}], "ingredients": [{"item": f"{NS}:{chunk}"}], "processingTime": 400, "results": [{"item": f"{NS}:{crushed}"}] + [{"item": f"{NS}:{crushed}", "chance": .3}] * 3})
        write(recipes / f"compat/create/milling/ore_chunks/{family}.json", {"type": "create:milling", "conditions": [{"type": "forge:mod_loaded", "modid": "create"}], "ingredients": [{"item": f"{NS}:{chunk}"}], "processingTime": 400, "results": [{"item": f"{NS}:{crushed}"}, {"item": f"{NS}:{crushed}", "chance": .1}]})
        hosted = []
        for variant in definition["variants"]:
            block, host = variant["block_id"], variant["copy_properties_from"]
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
        if family != "bauxite_laterite":
            if kind in ("metal", "gem", "bulk"):
                counts = (("chunk", chunk, 4), ("crushed", crushed, 9)) if kind != "bulk" else (("chunk", chunk, 2), ("crushed", crushed, 4))
                cooked = fraction if fraction is not None else output
                for stage, item, count in counts:
                    write(recipes / f"thermal/furnace/{family}_{stage}.json", {"type": "minecraft:smelting", "ingredient": {"item": f"{NS}:{item}"}, "result": {"item": cooked, "count": count}, "experience": .1, "cookingtime": 240})
                    write(recipes / f"thermal/blasting/{family}_{stage}.json", {"type": "minecraft:blasting", "ingredient": {"item": f"{NS}:{item}"}, "result": {"item": cooked, "count": count}, "experience": .1, "cookingtime": 120})
            if fluid:
                fluid_result = {"tag": fluid, "amount": 90} if fluid.startswith("forge:") else {"fluid": fluid, "amount": 90}
                foundry_result = {**fluid_result, "amount": 180}
                crushed_result = {**fluid_result, "amount": 120}
                crushed_foundry_result = {**fluid_result, "amount": 150}
                grade_amounts = {"major": 45, "minor": 20, "trace": 10, "precious": 5}
                byproducts = []
                seen_byproducts = set()
                for _, _, coproducts in routes:
                    for coproduct, grade in coproducts:
                        coproduct_fluid = MATERIALS[coproduct][3]
                        if coproduct_fluid is None or coproduct in seen_byproducts: continue
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
                    crushed_foundry["byproducts"] = [{**result, "amount": round(result["amount"] * 1.5)} for result in byproducts]
                write(recipes / f"compat/tconstruct/foundry/{family}_chunk.json", chunk_foundry)
                write(recipes / f"compat/tconstruct/foundry/{family}_crushed.json", crushed_foundry)

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
        png(item_textures / f"{item}.png", (index * .043 + .17) % 1, index + 200)
        lang[f"item.{NS}.{item}"] = f"{material.replace('_', ' ').title()} Concentrate"
        kind, output, fraction, fluid = MATERIALS[material]
        if kind in ("metal", "gem", "bulk"):
            cooked = fraction if fraction is not None else output
            write(recipes / f"thermal/furnace/concentrate_{material}.json", {"type": "minecraft:smelting", "ingredient": {"item": f"{NS}:{item}"}, "result": {"item": cooked, "count": 12}, "experience": .2, "cookingtime": 240})
            write(recipes / f"thermal/blasting/concentrate_{material}.json", {"type": "minecraft:blasting", "ingredient": {"item": f"{NS}:{item}"}, "result": {"item": cooked, "count": 12}, "experience": .2, "cookingtime": 120})
        if fluid:
            result = ({"tag": fluid, "amount": 180} if fluid.startswith("forge:") else {"fluid": fluid, "amount": 180})
            melting_result = ({"tag": fluid, "amount": 135} if fluid.startswith("forge:") else {"fluid": fluid, "amount": 135})
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
    for element, family in (("uranium", "uranium_ore"), ("thorium", "thorium_ore")):
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
