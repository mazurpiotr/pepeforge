#!/usr/bin/env python3
"""Generate Minecraft client translations from PepeForge's item YAML entries."""

from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Any

try:
    import yaml
except ImportError as error:
    raise SystemExit("PyYAML is required: install it with 'python3 -m pip install PyYAML'.") from error


ROOT = Path(__file__).resolve().parents[1]
LANG_DIR = ROOT / "src/main/resources/lang"
OUTPUT_DIR = ROOT / "resourcepack/assets/pepeforge/lang"
ITEM_IDS_SOURCE = ROOT / "src/main/java/pepin/pepeforge/item/ItemIds.java"
ITEM_FACTORY_SOURCE = ROOT / "src/main/java/pepin/pepeforge/item/ItemFactory.java"
CRIMSON_LORE_KEYS = {
    0: "1",
    1: "level",
    2: "2",
    3: "3",
    4: "unlocked",
    6: "rarity",
}
CRIMSON_FEATURE_KEYS = (
    "edge",
    "lifesteal",
    "lifesteal_next",
    "lifesteal_locked",
    "aura",
    "aura_next",
    "aura_locked",
)
CRIMSON_DYNAMIC_TEXT_KEYS = (
    "level_suffix",
    "max_xp",
)
RGB_COLOR = re.compile(r"&#[0-9a-fA-F]{6}")
LEGACY_COLOR = re.compile(r"&([0-9a-fk-orA-FK-OR])")
PLACEHOLDER = re.compile(r"\{([a-zA-Z0-9_]+)}")


def load_language(path: Path) -> dict[str, Any]:
    try:
        data = yaml.safe_load(path.read_text(encoding="utf-8"))
    except (OSError, yaml.YAMLError) as error:
        raise ValueError(f"Could not read {path}: {error}") from error
    if not isinstance(data, dict) or not isinstance(data.get("items"), dict):
        raise ValueError(f"{path} must contain an 'items' mapping")
    return data["items"]


def client_text(value: Any, *, drop_legacy_colors: bool = False) -> str:
    if not isinstance(value, str):
        raise ValueError(f"Expected a string translation, got {value!r}")
    # Existing client strings omit RGB rarity/name colors and represent legacy codes with §.
    value = RGB_COLOR.sub("", value)
    if drop_legacy_colors:
        value = LEGACY_COLOR.sub("", value)
    else:
        value = LEGACY_COLOR.sub(lambda match: "§" + match.group(1).lower(), value)
    value = value.replace("%", "%%")
    return PLACEHOLDER.sub("%s", value)


def add_translation(result: dict[str, str], key: str, value: Any, *, drop_legacy_colors: bool = False) -> None:
    if key in result:
        raise ValueError(f"Duplicate client translation key: {key}")
    result[key] = client_text(value, drop_legacy_colors=drop_legacy_colors)


def generate_language(items: dict[str, Any]) -> dict[str, str]:
    result: dict[str, str] = {}
    for item_id, item in items.items():
        if not isinstance(item_id, str) or not isinstance(item, dict):
            raise ValueError(f"Invalid item entry: {item_id!r}")
        base = f"item.pepeforge.{item_id}"
        add_translation(result, f"{base}.name", item.get("fallback_name"))
        mode_lore = item.get("mode_lore")
        if mode_lore is not None:
            if not isinstance(mode_lore, dict) or set(mode_lore) != {"thrust", "cut"}:
                raise ValueError(f"items.{item_id}.mode_lore must contain exactly thrust and cut")
            for mode in ("thrust", "cut"):
                add_translation(result, f"{base}.mode.{mode}", mode_lore[mode])
        lore = item.get("lore")
        if not isinstance(lore, list):
            raise ValueError(f"items.{item_id}.lore must be a list")

        if item_id == "crimson_sword":
            for index, semantic_key in CRIMSON_LORE_KEYS.items():
                if index >= len(lore):
                    raise ValueError(f"items.crimson_sword.lore is missing index {index}")
                add_translation(result, f"{base}.lore.{semantic_key}", lore[index])
            if len(lore) != 8 or lore[5] != "{features}":
                raise ValueError("items.crimson_sword.lore must keep the {features} entry at index 5")
            features = item.get("features")
            if not isinstance(features, dict):
                raise ValueError("items.crimson_sword.features must be a mapping")
            if set(features) != set(CRIMSON_FEATURE_KEYS):
                raise ValueError("items.crimson_sword.features keys do not match Crimson Sword runtime keys")
            for feature_key in CRIMSON_FEATURE_KEYS:
                add_translation(
                    result,
                    f"{base}.feature.{feature_key}",
                    features[feature_key],
                    drop_legacy_colors=True,
                )
            dynamic_text = item.get("dynamic_text")
            if not isinstance(dynamic_text, dict) or set(dynamic_text) != set(CRIMSON_DYNAMIC_TEXT_KEYS):
                raise ValueError("items.crimson_sword.dynamic_text keys do not match Crimson Sword runtime keys")
            for dynamic_key in CRIMSON_DYNAMIC_TEXT_KEYS:
                add_translation(result, f"{base}.dynamic.{dynamic_key}", dynamic_text[dynamic_key])
        else:
            for index, line in enumerate(lore, start=1):
                add_translation(result, f"{base}.lore.{index}", line)
    return result


def placeholder_names(value: str) -> list[str]:
    return PLACEHOLDER.findall(value)


def canonical_item_ids() -> set[str]:
    ids_source = ITEM_IDS_SOURCE.read_text(encoding="utf-8")
    constants = dict(re.findall(r'public static final String (\w+) = "([^"]+)";', ids_source))
    factory_source = ITEM_FACTORY_SOURCE.read_text(encoding="utf-8")
    match = re.search(r"CANONICAL_ITEM_IDS\s*=\s*List\.of\((.*?)\);", factory_source, re.DOTALL)
    if match is None:
        raise ValueError(f"Could not find ItemFactory.CANONICAL_ITEM_IDS in {ITEM_FACTORY_SOURCE}")
    constant_names = re.findall(r"ItemIds\.(\w+)", match.group(1))
    if not constant_names or any(name not in constants for name in constant_names):
        raise ValueError("ItemFactory.CANONICAL_ITEM_IDS contains an unknown or missing ItemIds constant")
    return {constants[name] for name in constant_names}


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="fail if generated JSON files are stale")
    args = parser.parse_args()

    language_files = sorted(LANG_DIR.glob("*.yml"))
    if not language_files:
        raise ValueError(f"No locale YAML files found in {LANG_DIR}")
    if not any(path.stem == "en_us" for path in language_files):
        raise ValueError(f"Required locale file {LANG_DIR / 'en_us.yml'} is missing")

    generated: dict[str, dict[str, str]] = {}
    canonical_ids = canonical_item_ids()
    for path in language_files:
        locale = path.stem
        items = load_language(path)
        item_ids = set(items)
        if item_ids != canonical_ids:
            raise ValueError(
                f"{locale} item list mismatch; missing={sorted(canonical_ids - item_ids)}, "
                f"unknown={sorted(item_ids - canonical_ids)}"
            )
        generated[locale] = generate_language(items)

    reference_locale = "en_us"
    reference_keys = set(generated[reference_locale])
    for locale, translations in generated.items():
        keys = set(translations)
        if keys != reference_keys:
            missing = sorted(reference_keys - keys)
            extra = sorted(keys - reference_keys)
            raise ValueError(f"{locale} translation key mismatch; missing={missing}, extra={extra}")

    # Check each generated entry's argument count and order against the YAML source values.
    source_values: dict[str, dict[str, str]] = {}
    for locale, path in ((path.stem, path) for path in language_files):
        source_values[locale] = {}
        for item_id, item in load_language(path).items():
            base = f"item.pepeforge.{item_id}"
            source_values[locale][f"{base}.name"] = item["fallback_name"]
            for mode, line in item.get("mode_lore", {}).items():
                source_values[locale][f"{base}.mode.{mode}"] = line
            if item_id == "crimson_sword":
                for index, semantic_key in CRIMSON_LORE_KEYS.items():
                    source_values[locale][f"{base}.lore.{semantic_key}"] = item["lore"][index]
                for feature_key in CRIMSON_FEATURE_KEYS:
                    source_values[locale][f"{base}.feature.{feature_key}"] = item["features"][feature_key]
                for dynamic_key in CRIMSON_DYNAMIC_TEXT_KEYS:
                    source_values[locale][f"{base}.dynamic.{dynamic_key}"] = item["dynamic_text"][dynamic_key]
            else:
                for index, line in enumerate(item["lore"], start=1):
                    source_values[locale][f"{base}.lore.{index}"] = line

    for key in sorted(reference_keys):
        reference = placeholder_names(source_values[reference_locale][key])
        for locale in generated:
            actual = placeholder_names(source_values[locale][key])
            if actual != reference:
                raise ValueError(f"Placeholder mismatch for {key} in {locale}: expected {reference}, got {actual}")

    if not args.check:
        OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    stale: list[str] = []
    for locale, translations in generated.items():
        path = OUTPUT_DIR / f"{locale}.json"
        content = json.dumps(translations, ensure_ascii=False, indent=2) + "\n"
        if args.check:
            if not path.exists() or path.read_text(encoding="utf-8") != content:
                stale.append(str(path.relative_to(ROOT)))
        else:
            path.write_text(content, encoding="utf-8")
            print(f"Generated {path.relative_to(ROOT)} ({len(translations)} keys)")

    if stale:
        raise ValueError("Generated client translations are stale: " + ", ".join(stale))
    if args.check:
        print(f"Client translations are current for {len(generated)} locales ({len(reference_keys)} keys each)")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (OSError, ValueError) as error:
        print(f"Client translation generation failed: {error}", file=sys.stderr)
        raise SystemExit(1) from error
