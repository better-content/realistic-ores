#!/usr/bin/env python3
"""Generate the small, auditable Realistic Ores geological worldgen resource set."""

import json
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "src/main/resources/data/realistic_ores"
MANIFEST = ROOT / "tools/geological_worldgen.json"


def write(path: Path, value: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + "\n", encoding="utf-8")


def target(tag: str, state: str) -> dict:
    return {
        "target": {"predicate_type": "minecraft:tag_match", "tag": tag},
        "state": {"Name": f"realistic_ores:{state}"},
    }


def height(kind: str, band: list[int]) -> dict:
    provider = {
        "type": f"minecraft:{kind}",
        "min_inclusive": {"absolute": band[0]},
        "max_inclusive": {"absolute": band[1]},
    }
    if kind == "trapezoid":
        provider["plateau"] = max(0, (band[1] - band[0]) // 3)
    return {"type": "minecraft:height_range", "height": provider}


def main() -> None:
    definitions = json.loads(MANIFEST.read_text(encoding="utf-8"))
    configured = DATA / "worldgen/configured_feature"
    placed = DATA / "worldgen/placed_feature"
    modifiers = DATA / "forge/biome_modifier"
    legacy = DATA / "realistic_ore_generation"
    for directory in (configured, placed):
        shutil.rmtree(directory, ignore_errors=True)
        directory.mkdir(parents=True)
    shutil.rmtree(legacy, ignore_errors=True)
    for path in modifiers.glob("add_*stone.json"):
        path.unlink()
    for path in modifiers.glob("add_*deepslate.json"):
        path.unlink()

    for family, definition in definitions.items():
        write(configured / f"{family}.json", {
            "type": "realistic_ores:geological_deposit",
            "config": {
                "targets": [
                    target("minecraft:stone_ore_replaceables", family),
                    target("minecraft:deepslate_ore_replaceables", f"deepslate_{family}"),
                ],
                "morphology": definition["morphology"],
                "block_budget": definition["home_budget"],
                "budget_spread": definition["home_spread"],
                "deposit_class": "home",
                "discard_chance_on_air_exposure": 0.0,
            },
        })
        for profile in ("home", "echo"):
            feature = f"{family}_{profile}"
            placement = []
            count_key = f"{profile}_count"
            rarity_key = f"{profile}_rarity"
            if count_key in definition:
                placement.append({"type": "minecraft:count", "count": definition[count_key]})
            else:
                placement.append({"type": "minecraft:rarity_filter", "chance": definition[rarity_key]})
            placement.extend([
                {"type": "minecraft:in_square"},
                height("trapezoid" if profile == "home" else "uniform", definition[profile]),
                {"type": "minecraft:biome"},
            ])
            # Echo features carry a larger memorable body without duplicating configuration files.
            configured_ref = f"realistic_ores:{family}"
            if profile == "echo":
                configured_ref = {"type": "realistic_ores:geological_deposit", "config": {
                    "targets": [
                        target("minecraft:stone_ore_replaceables", family),
                        target("minecraft:deepslate_ore_replaceables", f"deepslate_{family}"),
                    ],
                    "morphology": definition["morphology"],
                    "block_budget": definition["echo_budget"],
                    "budget_spread": definition["echo_spread"],
                    "deposit_class": "echo",
                    "discard_chance_on_air_exposure": 0.0,
                }}
            write(placed / f"{feature}.json", {"feature": configured_ref, "placement": placement})
            write(modifiers / f"add_{feature}.json", {
                "type": "forge:add_features",
                "biomes": "#minecraft:is_overworld",
                "features": f"realistic_ores:{feature}",
                "step": "underground_ores",
            })


if __name__ == "__main__":
    main()
