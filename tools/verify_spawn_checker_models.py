"""Reject positive-area coplanar overlaps and buried faces in the detector mesh."""
import json
from itertools import combinations
from pathlib import Path
from build_spawn_checker_models import FACE_AXES, exterior_elements

ROOT = Path(__file__).resolve().parents[1]


def faces(elements):
    for part in elements:
        lo, hi = part["from"], part["to"]
        for side in part["faces"]:
            axis, sign, u, _, v, _ = FACE_AXES[side]
            yield (part["name"], axis, hi[axis] if sign > 0 else lo[axis],
                   lo[u], hi[u], lo[v], hi[v], side)


def overlaps(elements):
    hits = []
    for a, b in combinations(faces(elements), 2):
        if a[1] != b[1] or abs(a[2] - b[2]) > 1e-7: continue
        width = min(a[4], b[4]) - max(a[3], b[3])
        height = min(a[6], b[6]) - max(a[5], b[5])
        if width > 1e-7 and height > 1e-7:
            hits.append((a[0], b[0], width * height))
    return hits


def surface_area(elements):
    return sum((face[4] - face[3]) * (face[6] - face[5]) for face in faces(elements))


def run():
    original = json.loads((ROOT / "tests/resources/DetectorZFightOriginal.json").read_text())["elements"]
    assert overlaps(original), "Original casing/metal-rim flicker was not reproduced"
    repaired = exterior_elements(original)
    assert not overlaps(repaired), "Original same-plane defect survives the surface cleanup"
    # Analytic union of overlapping 2x2x2 cubes is a 3x2x2 solid: area 32.
    template = {"name": "a", "from": [0, 0, 0], "to": [2, 2, 2],
                "faces": {side: {"texture": "#body", "uv": [0, 0, 16, 16]} for side in FACE_AXES}}
    second = {**template, "name": "b", "from": [1, 0, 0], "to": [3, 2, 2]}
    joined = exterior_elements([template, second])
    assert not overlaps(joined) and abs(surface_area(joined) - 32) < 1e-7
    identical = exterior_elements([template, {**template, "name": "replacement"}])
    assert abs(surface_area(identical) - 24) < 1e-7
    assert all(part["name"].startswith("replacement/") for part in identical)
    for name in ("normal_spawn_checker", "legendary_spawn_checker"):
        model = json.loads((ROOT / f"resources/assets/pixelmon_backport/models/item/{name}_handheld.json").read_text())
        parts = model["elements"]
        collisions = overlaps(parts)
        assert not collisions, (name, collisions[:5])
        assert len({part["name"].split("/")[0] for part in parts}) == 18
        for part in parts:
            assert all(a < b for a, b in zip(part["from"], part["to"]))
            assert len(part["faces"]) == 1
            uv = next(iter(part["faces"].values()))["uv"]
            assert 0 <= uv[0] < uv[2] <= 16 and 0 <= uv[1] < uv[3] <= 16
        print(f"PASS {name}: 18 components, {len(parts)} exposed quads, zero overlapping coplanar faces")
    print("PASS original body/rim flicker reproduced; exterior union, duplicate ownership, UV and analytic closed-surface area verified")


if __name__ == "__main__":
    run()
