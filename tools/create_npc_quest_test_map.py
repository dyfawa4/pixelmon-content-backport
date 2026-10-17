"""Create the independent 1.4.5 NPC quest initial world, without copying live chunks/player data."""
import copy
from pathlib import Path
import nbtlib as n

ROOT = Path(__file__).resolve().parents[1]


def main():
    world = copy.deepcopy(n.load(ROOT / "test-maps/saves/backport-qa-shop/level.dat"))
    world.pop("fml", None)
    world.pop("forge", None)
    data = world["Data"]
    data.pop("Player", None)
    data["LevelName"] = n.String("回迁NPC任务 1.4.5 手动验收")
    data["GameType"] = n.Int(0)
    data["allowCommands"] = n.Byte(1)
    for key, value in [("SpawnX", 0), ("SpawnY", 65), ("SpawnZ", 21)]:
        data[key] = n.Int(value)
    data["Time"] = n.Long(6000)
    data["DayTime"] = n.Long(6000)
    data["raining"] = n.Byte(0)
    data["thundering"] = n.Byte(0)
    data["Difficulty"] = n.Byte(0)
    generation = data["WorldGenSettings"]
    generation["seed"] = n.Long(14520261017)
    generation["generate_features"] = n.Byte(0)
    generation["bonus_chest"] = n.Byte(0)
    generator = generation["dimensions"]["minecraft:overworld"]["generator"]
    generator.clear()
    generator.update({"type": n.String("minecraft:flat"), "settings": n.Compound({
        "biome": n.String("minecraft:plains"),
        "layers": n.List[n.Compound]([n.Compound({"block": n.String("minecraft:" + block), "height": n.Int(height)})
                                    for block, height in [("bedrock", 1), ("stone", 59), ("dirt", 3), ("grass_block", 1)]]),
        "lakes": n.Byte(0), "features": n.Byte(0), "structures": n.Compound({"structures": n.Compound()})})})
    rules = data.setdefault("GameRules", n.Compound())
    for key, value in [("doDaylightCycle", "false"), ("doWeatherCycle", "false"), ("doFireTick", "false"),
                       ("randomTickSpeed", "0"), ("doMobSpawning", "false"), ("keepInventory", "true")]:
        rules[key] = n.String(value)
    target = ROOT / "test-maps/npc-quest-saves-v145/backport-npc-quests-145/level.dat"
    if target.exists() and n.load(target)["Data"]["LevelName"] != data["LevelName"]:
        raise ValueError("Unexpected existing world; refusing to overwrite")
    target.parent.mkdir(parents=True, exist_ok=True)
    world.save(target, gzipped=True)
    print("Prepared independent NPC quest initial world:", target)


if __name__ == "__main__":
    main()
