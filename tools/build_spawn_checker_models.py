"""Native cuboid handheld detectors; a separate generated sprite is used in GUI only."""
import json
from copy import deepcopy
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MODELS = ROOT / "resources/assets/pixelmon_backport/models/item"


def write(name, value):
    (MODELS / (name + ".json")).write_text(
        json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )


def cuboid(name, low, high, texture, front=None):
    faces = {side: {"uv": [0, 0, 16, 16], "texture": "#" + texture}
             for side in ("north", "south", "east", "west", "up", "down")}
    if front:
        faces["north"] = {"uv": front, "texture": "#screen"}
    return {"name": name, "from": low, "to": high, "faces": faces}


FACE_AXES = {
    "north": (2, -1, 0, True, 1, True),
    "south": (2, 1, 0, False, 1, True),
    "west": (0, -1, 2, False, 1, True),
    "east": (0, 1, 2, True, 1, True),
    "down": (1, -1, 0, False, 2, True),
    "up": (1, 1, 0, False, 2, False),
}


def exterior_elements(parts):
    """Keep only the solid union's exterior, assigning coincident faces once.

    Cuboids may intersect structurally, but emitting all six uncut faces causes
    depth fighting at the casing/rim/socket seams. Split the face rectangles at
    component bounds, discard hidden cells, and merge the remaining cells. The
    original surface positions and UV mapping are preserved; no epsilon offset
    is used to conceal duplicate surfaces. Later components own material seams.
    """
    output = []
    for index, part in enumerate(parts):
        low, high = part["from"], part["to"]
        for side, (axis, sign, ua, flip_u, va, flip_v) in FACE_AXES.items():
            if side not in part["faces"]:
                continue
            plane = high[axis] if sign > 0 else low[axis]
            relevant = [(j, other) for j, other in enumerate(parts) if j != index
                        and other["from"][axis] <= plane <= other["to"][axis]]
            us, vs = {low[ua], high[ua]}, {low[va], high[va]}
            for _, other in relevant:
                for edge in (other["from"][ua], other["to"][ua]):
                    if low[ua] < edge < high[ua]: us.add(edge)
                for edge in (other["from"][va], other["to"][va]):
                    if low[va] < edge < high[va]: vs.add(edge)
            us, vs = sorted(us), sorted(vs)
            visible = set()
            for x in range(len(us) - 1):
                for y in range(len(vs) - 1):
                    u, v = (us[x] + us[x + 1]) / 2, (vs[y] + vs[y + 1]) / 2
                    hidden = False
                    for j, other in relevant:
                        a, b = other["from"], other["to"]
                        if not (a[ua] < u < b[ua] and a[va] < v < b[va]): continue
                        outside_solid = a[axis] <= plane < b[axis] if sign > 0 else a[axis] < plane <= b[axis]
                        same_surface = b[axis] == plane if sign > 0 else a[axis] == plane
                        if outside_solid or same_surface and j > index:
                            hidden = True
                            break
                    if not hidden: visible.add((x, y))
            while visible:
                x, y = min(visible, key=lambda cell: (cell[1], cell[0]))
                end_x = x + 1
                while (end_x, y) in visible: end_x += 1
                end_y = y + 1
                while all((i, end_y) in visible for i in range(x, end_x)): end_y += 1
                for i in range(x, end_x):
                    for j in range(y, end_y): visible.remove((i, j))
                start, end = list(low), list(high)
                start[ua], end[ua] = us[x], us[end_x]
                start[va], end[va] = vs[y], vs[end_y]
                face = deepcopy(part["faces"][side])
                old_uv = face["uv"]
                fractions = []
                for coordinate, flipped in ((ua, flip_u), (va, flip_v)):
                    a = (start[coordinate] - low[coordinate]) / (high[coordinate] - low[coordinate])
                    b = (end[coordinate] - low[coordinate]) / (high[coordinate] - low[coordinate])
                    fractions.append((1 - b, 1 - a) if flipped else (a, b))
                face["uv"] = [round(old_uv[0] + fractions[0][0] * (old_uv[2] - old_uv[0]), 6),
                              round(old_uv[1] + fractions[1][0] * (old_uv[3] - old_uv[1]), 6),
                              round(old_uv[0] + fractions[0][1] * (old_uv[2] - old_uv[0]), 6),
                              round(old_uv[1] + fractions[1][1] * (old_uv[3] - old_uv[1]), 6)]
                output.append({"name": part["name"] + "/" + side + "/" + str(len(output)),
                               "from": start, "to": end, "faces": {side: face}})
    return output


def build(legendary):
    name = "legendary_spawn_checker" if legendary else "normal_spawn_checker"
    resource = "pixelmon_backport:item/" + name
    write(name, {
        "loader": "forge:separate-perspective",
        "ambientocclusion": False,
        "textures": {"particle": resource},
        "base": {"parent": resource + "_handheld"},
        "perspectives": {"gui": {"parent": resource + "_icon"}},
    })
    write(name + "_icon", {
        "parent": "minecraft:item/generated", "gui_light": "front",
        "textures": {"layer0": resource},
    })
    # The screen samples only the generated radar display. Other components are
    # real volumes with native game materials, including the rear and sides.
    uv = [5.5, 5.25, 11.5, 11.75] if legendary else [3.75, 5.5, 10.75, 12]
    elements = [
        cuboid("backplate", [4, 2, 6], [12, 12, 8.5], "body"),
        cuboid("rounded_side_body", [3, 3, 6], [13, 11, 8.5], "body"),
        cuboid("upper_rim", [4, 11, 5.5], [12, 12, 6.5], "rim"),
        cuboid("lower_rim", [4, 3, 5.5], [12, 4, 6.5], "rim"),
        cuboid("left_rim", [3.5, 4, 5.5], [4.5, 11, 6.5], "rim"),
        cuboid("right_rim", [11.5, 4, 5.5], [12.5, 11, 6.5], "rim"),
        cuboid("screen_recess", [4.5, 4, 5.6], [11.5, 11, 6], "dark"),
        cuboid("radar_glass", [4.75, 4.25, 5.45], [11.25, 10.75, 5.7], "dark", uv),
        cuboid("bottom_grip", [5, .75, 6], [11, 3, 8.75], "dark"),
        cuboid("grip_inlay", [6, 1.25, 5.75], [10, 2.25, 6.25], "accent"),
        cuboid("antenna_socket", [10.5, 11.5, 6], [12.5, 13, 8.5], "dark"),
        cuboid("antenna", [11, 12.5, 6.5], [12, 15, 7.75], "rim"),
        cuboid("antenna_tip", [10.75, 14.5, 6.25], [12.25, 15.75, 8], "accent"),
        cuboid("side_dial", [12.75, 6.5, 6.25], [14, 8.5, 8.25], "rim"),
        cuboid("dial_cap", [13.75, 6.9, 6.6], [14.25, 8.1, 7.9], "accent"),
        cuboid("left_button", [2.5, 5.5, 6.5], [3.5, 7.5, 8], "accent"),
        cuboid("rear_panel", [5, 4, 8.5], [11, 10, 8.75], "dark"),
        cuboid("rear_clasp", [7, 3.5, 8.75], [9, 5, 9], "rim"),
    ]
    write(name + "_handheld", {
        "ambientocclusion": False,
        "textures": {
            "particle": resource, "screen": resource,
            "body": "minecraft:block/purple_concrete" if legendary else "minecraft:block/gray_concrete",
            "rim": "minecraft:block/gold_block" if legendary else "minecraft:block/iron_block",
            "dark": "minecraft:block/black_concrete",
            "accent": "minecraft:block/yellow_concrete" if legendary else "minecraft:block/emerald_block",
        },
        "elements": exterior_elements(elements),
        "display": {
            "firstperson_righthand": {"rotation": [8, 170, -10], "translation": [1, 1.5, 1], "scale": [.75, .75, .75]},
            "firstperson_lefthand": {"rotation": [8, 190, 10], "translation": [1, 1.5, 1], "scale": [.75, .75, .75]},
            "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 1.5, 1], "scale": [.65, .65, .65]},
            "thirdperson_lefthand": {"rotation": [0, 0, 0], "translation": [0, 1.5, 1], "scale": [.65, .65, .65]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [.4, .4, .4]},
            "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [.8, .8, .8]},
        },
    })


if __name__ == "__main__":
    for legendary in (False, True):
        build(legendary)
    print("Built two 18-component handheld detectors with only exposed, non-overlapping surfaces.")
