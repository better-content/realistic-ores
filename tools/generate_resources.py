from __future__ import annotations

import json
import shutil
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
RESOURCES = ROOT / "src" / "main" / "resources"
LEGACY_SPEC_PATH = ROOT / "ore_spec.json"
LEGACY_TEXTURES_DIR = ROOT / "textures"

SHORT_ID_MAP = {
    "copper_sulfide_ore": "copper_sulfide",
    "zinc_ore": "zinc",
    "tin_ore": "tin",
    "nickel_sulfide_ore": "nickel_sulfide",
    "titanium_iron_oxide_ore": "titanium_iron_oxide",
    "sulfur_bearing_pyrite_ore": "sulfur_bearing_pyrite",
    "thorium_ore": "thorium",
    "uranium_ore": "uranium",
}

BLOCK_BASE_MAP = {value: key for key, value in SHORT_ID_MAP.items()}

TAG_MAP = {
    "coal_measures": ["minecraft:mineable/pickaxe", "forge:ores", "forge:ores/coal"],
    "ironstone": ["minecraft:mineable/pickaxe", "forge:ores", "forge:ores/iron"],
    "copper_sulfide": ["minecraft:mineable/pickaxe", "forge:ores", "forge:ores/copper"],
    "zinc": ["minecraft:mineable/pickaxe", "forge:ores", "forge:ores/zinc"],
    "tin": ["minecraft:mineable/pickaxe", "forge:ores", "forge:ores/tin"],
    "lead_zinc_vein": ["minecraft:mineable/pickaxe", "forge:ores", "forge:ores/lead", "forge:ores/zinc"],
    "nickel_sulfide": ["minecraft:mineable/pickaxe", "forge:ores", "forge:ores/nickel"],
    "tin_tungsten_greisen": ["minecraft:mineable/pickaxe", "forge:ores", "forge:ores/tin", "forge:ores/tungsten"],
    "titanium_iron_oxide": ["minecraft:mineable/pickaxe", "forge:ores", "forge:ores/titanium", "forge:ores/iron"],
    "bauxite_laterite": ["minecraft:mineable/pickaxe", "forge:ores", "forge:ores/aluminum"],
    "phosphate_rock": ["minecraft:mineable/pickaxe", "forge:ores", "forge:ores/phosphate"],
    "sulfur_bearing_pyrite": ["minecraft:mineable/pickaxe", "forge:ores", "forge:ores/sulfur"],
    "kimberlite_pipe": ["minecraft:mineable/pickaxe", "forge:ores", "forge:ores/diamond"],
    "emerald_schist_beryl_vein": ["minecraft:mineable/pickaxe", "forge:ores", "forge:ores/emerald"],
    "lazurite_vein": ["minecraft:mineable/pickaxe", "forge:ores", "forge:ores/lapis"],
    "cupriferous_redbed_redstone_vein": ["minecraft:mineable/pickaxe", "forge:ores", "forge:ores/redstone", "forge:ores/copper"],
    "quartz_vein": ["minecraft:mineable/pickaxe", "forge:ores", "forge:ores/quartz"],
    "corundum_beryl_gem_vein": ["minecraft:mineable/pickaxe", "forge:ores"],
    "soul_bearing_black_shale_soulstone_vein": ["minecraft:mineable/pickaxe", "forge:ores"],
    "thorium": ["minecraft:mineable/pickaxe", "forge:ores", "forge:ores/thorium"],
    "uranium": ["minecraft:mineable/pickaxe", "forge:ores", "forge:ores/uranium"],
}

VEIN_SIZE_MAP = {
    "coal_measures": 12,
    "ironstone": 10,
    "copper_sulfide": 10,
    "zinc": 8,
    "tin": 8,
    "nickel_sulfide": 8,
    "phosphate_rock": 8,
    "lead_zinc_vein": 7,
    "tin_tungsten_greisen": 7,
    "quartz_vein": 7,
    "bauxite_laterite": 9,
    "sulfur_bearing_pyrite": 9,
    "kimberlite_pipe": 5,
    "corundum_beryl_gem_vein": 5,
    "emerald_schist_beryl_vein": 5,
    "lazurite_vein": 5,
    "cupriferous_redbed_redstone_vein": 6,
    "soul_bearing_black_shale_soulstone_vein": 6,
    "titanium_iron_oxide": 6,
}

COUNT_MAP = {
    "coal_measures": 18,
    "ironstone": 14,
    "copper_sulfide": 12,
    "zinc": 10,
    "tin": 10,
    "lead_zinc_vein": 8,
    "nickel_sulfide": 8,
    "phosphate_rock": 8,
    "sulfur_bearing_pyrite": 8,
    "tin_tungsten_greisen": 6,
    "bauxite_laterite": 6,
    "quartz_vein": 6,
    "kimberlite_pipe": 4,
    "corundum_beryl_gem_vein": 4,
    "emerald_schist_beryl_vein": 4,
    "lazurite_vein": 4,
    "cupriferous_redbed_redstone_vein": 5,
    "soul_bearing_black_shale_soulstone_vein": 5,
    "titanium_iron_oxide": 5,
}

UNIFORM_DISTRIBUTION = {
    "coal_measures",
    "ironstone",
    "copper_sulfide",
    "zinc",
    "tin",
    "bauxite_laterite",
    "phosphate_rock",
    "sulfur_bearing_pyrite",
}

DISABLED_VANILLA_FEATURES = [
    "minecraft:ore_coal_upper",
    "minecraft:ore_coal_lower",
    "minecraft:ore_iron_upper",
    "minecraft:ore_iron_middle",
    "minecraft:ore_iron_small",
    "minecraft:ore_copper_large",
    "minecraft:ore_copper",
    "minecraft:ore_gold",
    "minecraft:ore_gold_lower",
    "minecraft:ore_redstone",
    "minecraft:ore_redstone_lower",
    "minecraft:ore_diamond",
    "minecraft:ore_diamond_large",
    "minecraft:ore_diamond_buried",
    "minecraft:ore_lapis",
    "minecraft:ore_lapis_buried",
    "minecraft:ore_emerald",
]


def short_id(source_id: str) -> str:
    return SHORT_ID_MAP.get(source_id, source_id)


def block_base(ore_id: str) -> str:
    return BLOCK_BASE_MAP.get(ore_id, ore_id)


def title_case(name: str) -> str:
    return " ".join(part.capitalize() for part in name.split("_"))


def display_name_for_block(base: str) -> str:
    words = title_case(base)
    if words.endswith(" Ore"):
        return f"{words} Deposit"
    return f"{words} Deposit"


def distribution(ore_id: str) -> str:
    return "uniform" if ore_id in UNIFORM_DISTRIBUTION else "triangle"


def write_json(path: Path, payload: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")


def reset_dir(path: Path) -> None:
    if path.exists():
        shutil.rmtree(path)
    path.mkdir(parents=True, exist_ok=True)


def append_tag_value(tag_files: dict[Path, set[str]], path: Path, value: str) -> None:
    tag_files.setdefault(path, set()).add(value)


def main() -> None:
    if not LEGACY_SPEC_PATH.exists() or not LEGACY_TEXTURES_DIR.exists():
        missing = []
        if not LEGACY_SPEC_PATH.exists():
            missing.append(str(LEGACY_SPEC_PATH.relative_to(ROOT)))
        if not LEGACY_TEXTURES_DIR.exists():
            missing.append(str(LEGACY_TEXTURES_DIR.relative_to(ROOT)))
        raise SystemExit(
            "Legacy generator inputs are missing: "
            + ", ".join(missing)
            + ". This repository now keeps generated resources under src/main/resources."
        )

    spec = json.loads(LEGACY_SPEC_PATH.read_text(encoding="utf-8"))

    ore_dir = RESOURCES / "data" / "realisticores" / "realistic_ores"
    worldgen_dir = RESOURCES / "data" / "realisticores" / "realistic_ore_generation"
    disabled_dir = RESOURCES / "data" / "realisticores" / "disabled_placed_features"
    configured_feature_dir = RESOURCES / "data" / "realisticores" / "worldgen" / "configured_feature"
    placed_feature_dir = RESOURCES / "data" / "realisticores" / "worldgen" / "placed_feature"
    biome_modifier_dir = RESOURCES / "data" / "realisticores" / "forge" / "biome_modifier"
    texture_dir = RESOURCES / "assets" / "realisticores" / "textures" / "block"
    blockstate_dir = RESOURCES / "assets" / "realisticores" / "blockstates"
    block_model_dir = RESOURCES / "assets" / "realisticores" / "models" / "block"
    item_model_dir = RESOURCES / "assets" / "realisticores" / "models" / "item"
    loot_dir = RESOURCES / "data" / "realisticores" / "loot_tables" / "blocks"

    for directory in [
        ore_dir,
        worldgen_dir,
        disabled_dir,
        configured_feature_dir,
        placed_feature_dir,
        biome_modifier_dir,
        texture_dir,
        blockstate_dir,
        block_model_dir,
        item_model_dir,
        loot_dir,
    ]:
        reset_dir(directory)

    tag_files: dict[Path, set[str]] = {}
    lang: dict[str, str] = {}

    for ore in spec["ores"]:
        source_id = ore["ore_id"]
        ore_id = short_id(source_id)
        base = block_base(ore_id)
        display_name = display_name_for_block(base)
        stone_block = base
        deepslate_block = f"deepslate_{base}"
        tags = TAG_MAP.get(ore_id, ["minecraft:mineable/pickaxe"])

        write_json(
            ore_dir / f"{ore_id}.json",
            {
                "id": ore_id,
                "display_name": display_name,
                "variants": [
                    {
                        "host": "stone",
                        "block_id": stone_block,
                        "texture_mode": "cube_all",
                        "textures": {"all": f"realisticores:block/{stone_block}"},
                        "copy_properties_from": "minecraft:stone",
                    },
                    {
                        "host": "deepslate",
                        "block_id": deepslate_block,
                        "texture_mode": "cube_column_like",
                        "textures": {
                            "side": f"realisticores:block/{deepslate_block}_side",
                            "top": f"realisticores:block/{deepslate_block}_top",
                            "bottom": f"realisticores:block/{deepslate_block}_bottom",
                        },
                        "copy_properties_from": "minecraft:deepslate",
                    },
                ],
                "tags": tags,
            },
        )

        shutil.copyfile(ROOT / "textures" / "stone" / f"{source_id}_stone.png", texture_dir / f"{stone_block}.png")
        shutil.copyfile(ROOT / "textures" / "deepslate" / f"{source_id}_deepslate_side.png", texture_dir / f"{deepslate_block}_side.png")
        shutil.copyfile(ROOT / "textures" / "deepslate" / f"{source_id}_deepslate_top.png", texture_dir / f"{deepslate_block}_top.png")
        shutil.copyfile(ROOT / "textures" / "deepslate" / f"{source_id}_deepslate_bottom.png", texture_dir / f"{deepslate_block}_bottom.png")

        write_json(blockstate_dir / f"{stone_block}.json", {"variants": {"": {"model": f"realisticores:block/{stone_block}"}}})
        write_json(block_model_dir / f"{stone_block}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"realisticores:block/{stone_block}"}})
        write_json(item_model_dir / f"{stone_block}.json", {"parent": f"realisticores:block/{stone_block}"})

        write_json(blockstate_dir / f"{deepslate_block}.json", {"variants": {"": {"model": f"realisticores:block/{deepslate_block}"}}})
        write_json(
            block_model_dir / f"{deepslate_block}.json",
            {
                "parent": "minecraft:block/cube_bottom_top",
                "textures": {
                    "side": f"realisticores:block/{deepslate_block}_side",
                    "top": f"realisticores:block/{deepslate_block}_top",
                    "bottom": f"realisticores:block/{deepslate_block}_bottom",
                },
            },
        )
        write_json(item_model_dir / f"{deepslate_block}.json", {"parent": f"realisticores:block/{deepslate_block}"})

        loot_template = lambda name: {
            "type": "minecraft:block",
            "pools": [
                {
                    "rolls": 1.0,
                    "entries": [{"type": "minecraft:item", "name": f"realisticores:{name}"}],
                    "conditions": [{"condition": "minecraft:survives_explosion"}],
                }
            ],
        }
        write_json(loot_dir / f"{stone_block}.json", loot_template(stone_block))
        write_json(loot_dir / f"{deepslate_block}.json", loot_template(deepslate_block))

        lang[f"block.realisticores.{stone_block}"] = display_name
        lang[f"block.realisticores.{deepslate_block}"] = f"Deepslate {display_name}"

        for block_id in [stone_block, deepslate_block]:
            full_id = f"realisticores:{block_id}"
            append_tag_value(tag_files, RESOURCES / "data" / "minecraft" / "tags" / "blocks" / "mineable" / "pickaxe.json", full_id)

            for tag in tags:
                namespace, tag_path = tag.split(":", 1)
                append_tag_value(tag_files, RESOURCES / "data" / namespace / "tags" / "blocks" / f"{tag_path}.json", full_id)
                if not (namespace == "minecraft" and tag_path.startswith("mineable/")):
                    append_tag_value(tag_files, RESOURCES / "data" / namespace / "tags" / "items" / f"{tag_path}.json", full_id)

        y_bands = ore.get("y_bands", {})
        for variant, target_tag in [("stone", "minecraft:stone_ore_replaceables"), ("deepslate", "minecraft:deepslate_ore_replaceables")]:
            band = y_bands.get(variant)
            if not band:
                continue

            feature_id = f"{ore_id}_{variant}"

            worldgen_definition = {
                "ore_id": ore_id,
                "variant": variant,
                "target_tag": target_tag,
                "vein_size": VEIN_SIZE_MAP.get(ore_id, 8),
                "count_per_chunk": COUNT_MAP.get(ore_id, 8),
                "distribution": distribution(ore_id),
                "min_y": band["min_y"],
                "max_y": band["max_y"],
                "biome_filter": "#minecraft:is_overworld",
                "generation_step": "underground_ores",
                "enabled": True,
            }
            write_json(worldgen_dir / f"{feature_id}.json", worldgen_definition)

            write_json(
                configured_feature_dir / f"{feature_id}.json",
                {
                    "type": "minecraft:ore",
                    "config": {
                        "size": worldgen_definition["vein_size"],
                        "discard_chance_on_air_exposure": 0.0,
                        "targets": [
                            {
                                "target": {
                                    "predicate_type": "minecraft:tag_match",
                                    "tag": target_tag,
                                },
                                "state": {
                                    "Name": f"realisticores:{stone_block if variant == 'stone' else deepslate_block}"
                                },
                            }
                        ],
                    },
                },
            )

            height_placement = {
                "type": "minecraft:height_range",
                "height": {
                    "type": f"minecraft:{'uniform' if worldgen_definition['distribution'] == 'uniform' else 'trapezoid'}",
                    "min_inclusive": {"absolute": worldgen_definition["min_y"]},
                    "max_inclusive": {"absolute": worldgen_definition["max_y"]},
                },
            }
            write_json(
                placed_feature_dir / f"{feature_id}.json",
                {
                    "feature": f"realisticores:{feature_id}",
                    "placement": [
                        {"type": "minecraft:count", "count": worldgen_definition["count_per_chunk"]},
                        {"type": "minecraft:in_square"},
                        height_placement,
                        {"type": "minecraft:biome"},
                    ],
                },
            )

            write_json(
                biome_modifier_dir / f"add_{feature_id}.json",
                {
                    "type": "forge:add_features",
                    "biomes": worldgen_definition["biome_filter"],
                    "features": f"realisticores:{feature_id}",
                    "step": worldgen_definition["generation_step"],
                },
            )

    write_json(disabled_dir / "vanilla.json", {"features": DISABLED_VANILLA_FEATURES})
    write_json(RESOURCES / "assets" / "realisticores" / "lang" / "en_us.json", lang)
    write_json(
        biome_modifier_dir / "remove_disabled_vanilla_ores.json",
        {
            "type": "forge:remove_features",
            "biomes": "#minecraft:is_overworld",
            "features": DISABLED_VANILLA_FEATURES,
            "steps": ["underground_ores"],
        },
    )

    for path, values in tag_files.items():
        write_json(path, {"replace": False, "values": sorted(values)})


if __name__ == "__main__":
    main()
