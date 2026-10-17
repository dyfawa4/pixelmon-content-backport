"""Read-only audit of the supplied Pixelmon quests and effective English/Chinese language keys."""
import argparse
import collections
import hashlib
import json
from pathlib import Path
import re
import zipfile

ROOT = Path(__file__).resolve().parents[1]


def audit(jar_path):
    with zipfile.ZipFile(jar_path) as jar:
        quests = {n: json.loads(jar.read(n)) for n in sorted(jar.namelist())
                  if n.startswith("data/pixelmon/quests/") and n.endswith(".json")}
        languages = {lang: json.loads(jar.read(f"assets/pixelmon/lang/{lang}.json")) for lang in ("en_us", "zh_cn")}
        native_missing = sorted(set(languages["en_us"]) - set(languages["zh_cn"]))
    for lang, effective in languages.items():
        for namespace in ("pixelmon", "pixelmon_backport"):
            path = ROOT / "resources/assets" / namespace / "lang" / (lang + ".json")
            if path.exists():
                effective.update(json.loads(path.read_text(encoding="utf-8-sig")))
    en, zh = languages["en_us"], languages["zh_cn"]
    related = {k: v for k, v in en.items() if k.startswith(("quest.", "quests.", "gui.quest", "npc.", "pixelmon_backport.npc_quest."))}
    missing = sorted(set(en) - set(zh))
    format_errors = []
    for key, value in related.items():
        if key not in zh:
            continue
        # Numbered parameters may move in Chinese, but their identities/types must remain.
        spec = r"%(?:\d+\$)?[sd]"
        if sorted(re.findall(spec, value)) != sorted(re.findall(spec, zh[key])) or value.count("#") != zh[key].count("#"):
            format_errors.append(key)
    unresolved, graph_errors, rows = [], [], []
    objective_types = collections.Counter()
    for filename, quest in quests.items():
        prefix = "quest." + Path(filename).stem + "."
        stages = quest["stages"]
        ids = {s["stage"] for s in stages}
        if len(ids) != len(stages):
            graph_errors.append([filename, "duplicate stage"])
        types = collections.Counter()
        for stage in stages:
            if stage["nextStage"] >= 0 and stage["nextStage"] not in ids:
                graph_errors.append([filename, stage["stage"], stage["nextStage"]])
            for raw in stage.get("conditions", []) + stage.get("objectives", []):
                args = raw.split(" ")
                types[args[0]] += 1
                if args[0] == "DIALOGUE":
                    for name in args[2:]:
                        if name == "-":
                            continue
                        key = prefix + name.lower()
                        if key not in zh and name not in quest.get("strings", {}):
                            unresolved.append([filename, stage["stage"], key])
            for raw in stage.get("actions", []):
                args = raw.split(" ")
                if len(args) == 3 and args[1] == "SET_STAGE" and int(args[2]) not in ids:
                    graph_errors.append([filename, "action", raw])
        objective_types.update(types)
        rows.append({"id": filename, "stages": len(stages), "objective_types": dict(sorted(types.items())),
                     "retained": True, "reward_and_stage_definition_changed": False,
                     "native_definition_sha256": hashlib.sha256(json.dumps(quest, sort_keys=True, ensure_ascii=False).encode()).hexdigest()})
    english_same = {k: v for k, v in en.items() if k in zh and zh[k] == v and re.search(r"[A-Za-z]{4,}", v)
                    and not re.search(r"[\u3400-\u9fff]", v)}
    report = {"version": "1.4.5", "pixelmon_jar": Path(jar_path).name,
              "quest_count": len(rows), "stage_count": sum(row["stages"] for row in rows),
              "objective_types": dict(sorted(objective_types.items())), "quests": rows,
              "graph_errors": graph_errors, "unresolved_dialogue_keys": unresolved,
              "localization": {"native_missing_zh_keys_before_addon": native_missing,
                               "effective_english_key_count": len(en), "effective_missing_zh_keys": missing,
                               "quest_npc_key_count": len(related),
                               "quest_npc_missing_zh_keys": sorted(set(related) - set(zh)),
                               "quest_npc_format_errors": format_errors,
                               "identical_english_values": english_same},
              "scope": "Resource/key and graph audit; does not claim every live NPC/world/dialogue has been manually played."}
    if missing or unresolved or graph_errors or format_errors:
        raise ValueError(json.dumps({k: report[k] for k in ("graph_errors", "unresolved_dialogue_keys", "localization")}, ensure_ascii=False))
    return report


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--pixelmon-jar", required=True)
    parser.add_argument("--output", type=Path, default=ROOT / "docs/NPC_QUEST_AUDIT_1.4.5.json")
    args = parser.parse_args()
    result = audit(args.pixelmon_jar)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"PASS {result['quest_count']} native quests / {result['stage_count']} stages; "
          f"{result['localization']['quest_npc_key_count']} quest/NPC keys; no missing Chinese keys, bad placeholders or unresolved dialogues.")


if __name__ == "__main__":
    main()
