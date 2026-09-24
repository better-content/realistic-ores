"""Unit coverage for the geological resource generator's ownership boundary."""

import importlib.util
import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch


SCRIPT = Path(__file__).with_name("generate_geological_worldgen.py")
SPEC = importlib.util.spec_from_file_location("geological_worldgen_generator", SCRIPT)
GENERATOR = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(GENERATOR)


class GeologicalWorldgenGeneratorTest(unittest.TestCase):
    def test_checked_in_ownership_inventory_matches_manifest(self):
        manifest = json.loads(GENERATOR.MANIFEST.read_text(encoding="utf-8"))
        owned = json.loads(GENERATOR.OWNERSHIP.read_text(encoding="utf-8"))
        self.assertEqual(sorted(manifest), owned)

    def test_regeneration_preserves_unowned_resources(self):
        Path.home().joinpath(".tmp").mkdir(parents=True, exist_ok=True)
        with tempfile.TemporaryDirectory(dir=Path.home() / ".tmp") as temporary:
            root = Path(temporary)
            data = root / "data"
            configured = data / "worldgen/configured_feature"
            placed = data / "worldgen/placed_feature"
            modifiers = data / "forge/biome_modifier"
            for directory in (configured, placed, modifiers):
                directory.mkdir(parents=True)

            (configured / "custom_feature.json").write_text('{"keep":true}', encoding="utf-8")
            (configured / "ironstone.json").write_text('{"old":true}', encoding="utf-8")
            (configured / "retired_family.json").write_text('{"old":true}', encoding="utf-8")
            (placed / "ironstone_home.json").write_text('{"old":true}', encoding="utf-8")
            (placed / "aether_ironstone.json").write_text('{"keep":true}', encoding="utf-8")
            (placed / "retired_family_echo.json").write_text('{"old":true}', encoding="utf-8")
            (modifiers / "add_ironstone_home.json").write_text('{"old":true}', encoding="utf-8")
            (modifiers / "add_aether_ironstone.json").write_text('{"keep":true}', encoding="utf-8")
            (modifiers / "add_retired_family_echo.json").write_text('{"old":true}', encoding="utf-8")

            manifest = root / "geological_worldgen.json"
            manifest.write_text(json.dumps({
                "ironstone": {
                    "morphology": "lenticular_oolitic_bed", "home": [-16, 80],
                    "echo": [112, 288], "home_budget": 34, "home_spread": 10,
                    "echo_budget": 42, "echo_spread": 12, "home_count": 7,
                    "echo_count": 1, "baseline_supply": 280,
                }
            }), encoding="utf-8")
            ownership = root / "geological_worldgen.outputs.json"
            ownership.write_text(json.dumps(["ironstone", "retired_family"]), encoding="utf-8")

            with (patch.object(GENERATOR, "DATA", data),
                  patch.object(GENERATOR, "MANIFEST", manifest),
                  patch.object(GENERATOR, "OWNERSHIP", ownership)):
                GENERATOR.main()

            self.assertEqual('{"keep":true}', (configured / "custom_feature.json").read_text())
            self.assertEqual('{"keep":true}', (placed / "aether_ironstone.json").read_text())
            self.assertEqual('{"keep":true}', (modifiers / "add_aether_ironstone.json").read_text())
            self.assertTrue((configured / "ironstone.json").exists())
            self.assertTrue((placed / "ironstone_home.json").exists())
            self.assertTrue((modifiers / "add_ironstone_home.json").exists())
            self.assertFalse((configured / "retired_family.json").exists())
            self.assertFalse((placed / "retired_family_echo.json").exists())
            self.assertFalse((modifiers / "add_retired_family_echo.json").exists())
            self.assertEqual(["ironstone"], json.loads(ownership.read_text(encoding="utf-8")))


if __name__ == "__main__":
    unittest.main()
