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
    "evaporite_beds", "hotstone", "black_shale",
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

GRADE = {"major": 1.0, "minor": .5, "trace": .2, "precious": .05}

# One salient technical assay per family. This is the union of the old medium/
# acid variants, keeping the best established grade for each material. Diamond
# is intentionally absent: the Hexerei Tin Quartz route is its deliberate source.
TECHNICAL_ASSAYS = {
    "coal_measures": ("coal", [("iron", "trace")]),
    "ironstone": ("iron", [("nickel", "minor")]),
    "copper_bloom": ("copper", [("sulfur", "major"), ("iron", "minor"), ("gold", "precious")]),
    "tin_quartz": ("tin", [("quartz", "major"), ("aluminum", "minor"), ("amethyst", "major"), ("lapis", "major"), ("emerald", "minor")]),
    "brassroot": ("zinc", [("lead", "minor"), ("cadmium", "trace"), ("silver", "precious")]),
    "evaporite_beds": ("rock_salt", [("sodium_chloride", "major"), ("saltpeter", "minor")]),
    "hotstone": ("uranium", [("thorium", "minor"), ("lead", "minor"), ("titanium", "major"), ("nickel", "minor"), ("cobalt", "trace"), ("iron", "minor"), ("osmium", "minor"), ("sulfur", "major")]),
    "black_shale": ("redstone", [("copper", "major"), ("iron", "minor"), ("soul_sand", "major"), ("sulfur", "minor"), ("gold", "precious")]),
}

EXOTIC_OUTPUTS = {
    "coal_measures": {"blood": "minecraft:blaze_powder", "hexerei": "occultism:otherworld_ashes", "ars": "ars_nouveau:fire_essence"},
    "ironstone": {"blood": "minecraft:clay_ball", "hexerei": "occultism:otherworld_essence", "ars": "ars_nouveau:earth_essence"},
    "copper_bloom": {"blood": "minecraft:phantom_membrane", "hexerei": "occultism:spirit_attuned_gem", "ars": "ars_nouveau:manipulation_essence"},
    "tin_quartz": {"blood": "minecraft:lapis_lazuli", "hexerei": "realistic_ores:diamond_chip", "ars": "ars_nouveau:manipulation_essence"},
    "brassroot": {"blood": "minecraft:phantom_membrane", "hexerei": "occultism:iesnium_dust", "ars": "ars_nouveau:air_essence"},
    "evaporite_beds": {"blood": "minecraft:prismarine_crystals", "hexerei": "hexerei:moon_dust", "ars": "ars_nouveau:water_essence"},
    "hotstone": {"blood": "bloodmagic:destructivecrystal", "hexerei": "occultism:afrit_essence", "ars": "ars_nouveau:fire_essence"},
    "black_shale": {"blood": "bloodmagic:corrupted_dust", "hexerei": "occultism:soul_shard", "ars": "ars_nouveau:abjuration_essence"},
}

HEAT = {"copper": 500, "tin": 700, "zinc": 700}
CONCENTRATE_MATERIALS = tuple(material for material in MATERIALS if material != "diamond")


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

    # Fluid rendering is provided by IClientFluidTypeExtensions, but Minecraft
    # still resolves a blockstate and particle model for each liquid level.
    for material in ("titanium", "thorium"):
        block = f"molten_{material}"
        write(ASSETS / f"blockstates/{block}.json", {
            "variants": {"": {"model": f"{NS}:block/{block}"}},
        })
        write(ASSETS / f"models/block/{block}.json", {
            "textures": {"particle": "minecraft:block/water_still"},
        })
    for key in list(lang):
        if any(token in key for token in ("ore_chunk_", "small_ore_chunk_", "crushed_", "rinsed_", "_concentrate", "_grinding_ball", "_chip")):
            del lang[key]
        elif key.startswith(f"block.{NS}.") and not any(
                preserved in key for preserved in ("oil_seep", "molten_titanium", "molten_thorium")):
            del lang[key]

    item_models = ASSETS / "models/item"
    item_textures = ASSETS / "textures/item"
    for pattern in ("ore_chunk_*.json", "crushed_*.json", "rinsed_*.json", "small_ore_chunk_*.json", "*_concentrate.json", "*_grinding_ball.json", "*_chip.json"):
        for path in item_models.glob(pattern): path.unlink()
    # Curated chunk, small-chunk, crushed-feed, and concentrate sprites are reduced
    # from committed high-resolution masters and must never be replaced here.
    for pattern in ("*_grinding_ball.png", "diamond_concentrate.png", "*_chip.png"):
        for path in item_textures.glob(pattern): path.unlink()

    recipes = DATA / "recipes"
    for directory in (
            recipes / "crafting/small_chunks", recipes / "crafting/ore_reassembly",
            recipes / "crafting/immediate", recipes / "compat/create/crushing",
            recipes / "compat/create/milling/ore_chunks", recipes / "compat/create/separation",
            recipes / "compat/create/grinding_balls", recipes / "compat/create/rinsing",
            recipes / "compat/pneumaticcraft/separation", recipes / "compat/bloodmagic/separation",
            recipes / "compat/hexerei/separation", recipes / "compat/ars_nouveau/separation",
            recipes / "thermal/furnace", recipes / "thermal/blasting",
            recipes / "compat/tconstruct/melting", recipes / "compat/tconstruct/foundry",
            recipes / "crafting/gem_chips"):
        reset(directory)
    processing_dir = DATA / "processing_definitions"
    reset(processing_dir)
    reset(DATA / "tags/blocks/deposit_ore_blocks")
    reset(DATA / "tags/items/crushed_feeds")
    reset(DATA / "tags/items/rinsed_feeds")
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

    all_chunks, all_small, all_crushed, all_rinsed = [], [], [], []
    for index, family in enumerate(FAMILIES):
        definition = by_family[family]
        chunk, small = f"ore_chunk_{family}", f"small_ore_chunk_{family}"
        crushed, rinsed = f"crushed_{family}", f"rinsed_{family}"
        all_chunks.append(f"{NS}:{chunk}"); all_small.append(f"{NS}:{small}")
        all_crushed.append(f"{NS}:{crushed}"); all_rinsed.append(f"{NS}:{rinsed}")
        for item, suffix, label in (
                (chunk, "chunk", "Ore Chunk"),
                (crushed, "crushed", "Crushed Feed"),
                (rinsed, "rinsed", "Rinsed Feed")):
            texture = crushed if suffix == "rinsed" else item
            write(item_models / f"{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{texture}"}})
            if suffix != "rinsed":
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

        primary, coproducts = TECHNICAL_ASSAYS[family]
        exotic = EXOTIC_OUTPUTS[family]
        processing = {
            "schema": "bc.realistic_ores.processing.v2",
            "family": family,
            "primary": primary,
            "stages": {"chunk": chunk, "crushed": crushed, "rinsed": rinsed},
            "routes": {
                "tech": {
                    "input_count": 4,
                    "pressure": 2.0,
                    "primary_count": 4,
                    "coproducts": [
                        {"material": material, "grade": grade, "nominal_chance": GRADE[grade]}
                        for material, grade in coproducts
                    ],
                },
                "blood": {"input_count": 4, "output": exotic["blood"]},
                "hexerei": {"input_count": 4, "output": exotic["hexerei"]},
                "ars": {"input_count": 1, "output": exotic["ars"], "chance": .25},
            },
        }
        if family == "tin_quartz":
            processing["routes"]["ars"]["bonus"] = {
                "item": "ars_nouveau:source_gem", "chance": .0625,
            }
        write(processing_dir / f"{family}.json", processing)

        write(recipes / f"compat/create/rinsing/{family}.json", {
            "type": "create:filling",
            "conditions": [{"type": "forge:mod_loaded", "modid": "create"}],
            "ingredients": [
                {"item": f"{NS}:{crushed}"},
                {"fluid": "minecraft:water", "amount": 250},
            ],
            "results": [{"item": f"{NS}:{rinsed}"}],
        })

        tech_results = [{"item": f"{NS}:{primary}_concentrate", "count": 4}]
        tech_results += [{"item": f"{NS}:{material}_concentrate"} for material, _ in coproducts]
        write(recipes / f"compat/pneumaticcraft/separation/{family}.json", {
            "type": "pneumaticcraft:pressure_chamber",
            "conditions": [{"type": "forge:mod_loaded", "modid": "pneumaticcraft"}],
            "inputs": [{
                "type": "pneumaticcraft:stacked_item",
                "item": f"{NS}:{rinsed}",
                "count": 4,
            }],
            "pressure": 2.0,
            "results": tech_results,
        })

        blood_tier = 3 if family in ("hotstone", "black_shale") else 2
        write(recipes / f"compat/bloodmagic/separation/{family}.json", {
            "type": "bloodmagic:alchemytable",
            "conditions": [{"type": "forge:mod_loaded", "modid": "bloodmagic"}],
            "input": [{"item": f"{NS}:{crushed}"}] * 4,
            "output": {"item": exotic["blood"]},
            "syphon": 5000 if blood_tier == 3 else 2000,
            "ticks": 400 if blood_tier == 3 else 200,
            "upgradeLevel": blood_tier,
        })

        font_proofs = [
            {"item": "aether:ambrosium_shard"},
            {"item": "minecraft:blaze_powder"},
            {"item": "the_bumblezone:honey_crystal_shards"},
            {"item": "rats:gem_of_ratlantis"},
        ]
        hot_family = family in ("hotstone", "black_shale")
        font_fluid = "minecraft:lava" if hot_family else "minecraft:water"
        write(recipes / f"compat/hexerei/separation/{family}.json", {
            "type": "hexerei:mixingcauldron",
            "conditions": [{"type": "forge:mod_loaded", "modid": "hexerei"}],
            "liquid": {"fluid": font_fluid},
            "ingredients": [{"item": f"{NS}:{crushed}"}] * 4 + font_proofs,
            "output": {"item": exotic["hexerei"]},
            "liquidOutput": {"fluid": font_fluid},
            "fluidLevelsConsumed": 333 if hot_family else 250,
            "heatRequirement": "heated",
        })

        ars_outputs = [{
            "chance": .25, "count": 1, "item": exotic["ars"], "maxRange": 1,
        }]
        if family == "tin_quartz":
            ars_outputs.append({
                "chance": .0625, "count": 1,
                "item": "ars_nouveau:source_gem", "maxRange": 1,
            })
        write(recipes / f"compat/ars_nouveau/separation/{family}.json", {
            "type": "ars_nouveau:crush",
            "conditions": [{"type": "forge:mod_loaded", "modid": "ars_nouveau"}],
            "input": {"item": f"{NS}:{crushed}"},
            "output": ars_outputs,
            "skip_block_place": False,
        })

        kind, output, fraction, fluid = MATERIALS[primary]
        if kind in ("gem", "bulk"):
            cooked = fraction if fraction is not None else output
            for stage, item, count in (("chunk", chunk, 2), ("crushed", crushed, 1)):
                write(recipes / f"thermal/furnace/{family}_{stage}.json", {"type": "minecraft:smelting", "ingredient": {"item": f"{NS}:{item}"}, "result": {"item": cooked, "count": count}, "experience": .1, "cookingtime": 240})
                write(recipes / f"thermal/blasting/{family}_{stage}.json", {"type": "minecraft:blasting", "ingredient": {"item": f"{NS}:{item}"}, "result": {"item": cooked, "count": count}, "experience": .1, "cookingtime": 120})
        if kind == "metal" and fluid:
            fluid_result = {"tag": fluid, "amount": 20} if fluid.startswith("forge:") else {"fluid": fluid, "amount": 20}
            foundry_result = {**fluid_result, "amount": 30}
            crushed_result = {**fluid_result, "amount": 10}
            crushed_foundry_result = {**fluid_result, "amount": 15}
            rinsed_result = {**fluid_result, "amount": 15}
            rinsed_foundry_result = {**fluid_result, "amount": 25}
            temperature = HEAT.get(primary, 950)
            condition = [{"type": "forge:mod_loaded", "modid": "tconstruct"}]
            write(recipes / f"compat/tconstruct/melting/{family}_chunk.json", {"type": "tconstruct:melting", "conditions": condition, "ingredient": {"item": f"{NS}:{chunk}"}, "result": fluid_result, "temperature": temperature, "time": 120})
            write(recipes / f"compat/tconstruct/melting/{family}_crushed.json", {"type": "tconstruct:melting", "conditions": condition, "ingredient": {"item": f"{NS}:{crushed}"}, "result": crushed_result, "temperature": temperature, "time": 120})
            write(recipes / f"compat/tconstruct/melting/{family}_rinsed.json", {"type": "tconstruct:melting", "conditions": condition, "ingredient": {"item": f"{NS}:{rinsed}"}, "result": rinsed_result, "temperature": temperature, "time": 120})
            chunk_foundry = {"type": "tconstruct:ore_melting", "conditions": condition, "ingredient": {"item": f"{NS}:{chunk}"}, "result": foundry_result, "rate": "metal", "temperature": temperature, "time": 120}
            crushed_foundry = {"type": "tconstruct:ore_melting", "conditions": condition, "ingredient": {"item": f"{NS}:{crushed}"}, "result": crushed_foundry_result, "rate": "metal", "temperature": temperature, "time": 120}
            rinsed_foundry = {"type": "tconstruct:ore_melting", "conditions": condition, "ingredient": {"item": f"{NS}:{rinsed}"}, "result": rinsed_foundry_result, "rate": "metal", "temperature": temperature, "time": 120}
            write(recipes / f"compat/tconstruct/foundry/{family}_chunk.json", chunk_foundry)
            write(recipes / f"compat/tconstruct/foundry/{family}_crushed.json", crushed_foundry)
            write(recipes / f"compat/tconstruct/foundry/{family}_rinsed.json", rinsed_foundry)

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
    write(DATA / "tags/items/rinsed_feeds.json", {"replace": False, "values": all_rinsed})
    reset(DATA / "tags/items/deposit_chunks")
    for family in FAMILIES:
        write(DATA / f"tags/items/deposit_chunks/{family}.json", {"replace": False, "values": [f"{NS}:ore_chunk_{family}"]})
        write(DATA / f"tags/items/crushed_feeds/{family}.json", {"replace": False, "values": [f"{NS}:crushed_{family}"]})
        write(DATA / f"tags/items/rinsed_feeds/{family}.json", {"replace": False, "values": [f"{NS}:rinsed_{family}"]})
    write(DATA / "tags/blocks/deposit_ore_blocks.json", {"replace": False, "values": [f"#{NS}:deposit_ore_blocks/{family}" for family in FAMILIES]})

    for index, material in enumerate(CONCENTRATE_MATERIALS):
        item = f"{material}_concentrate"
        write(item_models / f"{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{item}"}})
        require_curated_item_texture(item_textures, item)
        lang[f"item.{NS}.{item}"] = f"{material.replace('_', ' ').title()} Concentrate"
        kind, output, fraction, fluid = MATERIALS[material]
        if kind in ("gem", "bulk"):
            cooked = fraction if fraction is not None else output
            write(recipes / f"thermal/furnace/concentrate_{material}.json", {"type": "minecraft:smelting", "ingredient": {"item": f"{NS}:{item}"}, "result": {"item": cooked, "count": 2}, "experience": .2, "cookingtime": 240})
            write(recipes / f"thermal/blasting/concentrate_{material}.json", {"type": "minecraft:blasting", "ingredient": {"item": f"{NS}:{item}"}, "result": {"item": cooked, "count": 2}, "experience": .2, "cookingtime": 120})
        if kind == "metal" and fluid:
            result = ({"tag": fluid, "amount": 30} if fluid.startswith("forge:") else {"fluid": fluid, "amount": 30})
            melting_result = ({"tag": fluid, "amount": 20} if fluid.startswith("forge:") else {"fluid": fluid, "amount": 20})
            temperature = HEAT.get(material, 950)
            write(recipes / f"compat/tconstruct/melting/concentrate_{material}.json", {"type": "tconstruct:melting", "conditions": [{"type": "forge:mod_loaded", "modid": "tconstruct"}], "ingredient": {"item": f"{NS}:{item}"}, "result": melting_result, "temperature": temperature, "time": 120})
            write(recipes / f"compat/tconstruct/foundry/concentrate_{material}.json", {"type": "tconstruct:ore_melting", "conditions": [{"type": "forge:mod_loaded", "modid": "tconstruct"}], "ingredient": {"item": f"{NS}:{item}"}, "result": result, "rate": "metal", "temperature": temperature, "time": 120})

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
        write(DATA / f"tags/items/radioactive_forms/{element}/rinsed_feed.json", {"replace": False, "values": [f"{NS}:rinsed_{family}"]})
        write(DATA / f"tags/items/radioactive_forms/{element}/concentrate.json", {"replace": False, "values": [f"{NS}:{element}_concentrate"]})
        write(DATA / f"tags/items/radioactive_forms/{element}/final_forms.json", {"replace": False, "values": [f"chemlib:{element}_ingot", f"chemlib:{element}_nugget"]})
        blocks = [f"{NS}:{variant['block_id']}" for variant in by_family[family]["variants"]]
        write(DATA / f"tags/blocks/radioactive_forms/{element}/hosted_ore_blocks.json", {"replace": False, "values": blocks})

    write(lang_path, dict(sorted(lang.items())))


if __name__ == "__main__":
    main()
