"""Audit crown resources and every native/backported PQC body without launching Minecraft."""
import argparse
import hashlib
import json
import math
import re
import zipfile
from pathlib import Path
from PIL import Image
from convert_content import Bmd

ROOT = Path(__file__).resolve().parents[1]


def score(name):
    name = re.sub('[^a-z0-9]', '', name.lower())
    if name == 'head': return 100
    if re.fullmatch(r'(bip[0-9]*)?head[0-9]*', name) or name in ('headjnt', 'jnthead'): return 90
    if name.startswith('head') and 'end' not in name and 'top' not in name: return 75
    if 'head' in name and 'end' not in name and 'top' not in name and 'forehead' not in name: return 70
    if name in ('skull', 'face'): return 65
    return 0


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--pixelmon-jar', required=True, type=Path)
    parser.add_argument('--output', type=Path, default=ROOT/'.cache/tera-visual-research/coverage.json')
    args = parser.parse_args()
    manifest = json.loads((ROOT/'resources/backport/tera_visual_sources.json').read_text(encoding='utf-8'))
    assert len(manifest['crowns']) == 19 and len({c['type'] for c in manifest['crowns']}) == 19
    for crown in manifest['crowns']:
        folder = ROOT/'resources/assets/pixelmon_backport/textures/tera/crowns'/crown['type']
        data = (folder/'crown.bmd').read_bytes()
        assert hashlib.sha256(data).hexdigest() == crown['convertedSha256']
        assert hashlib.sha256((folder/'texture.png').read_bytes()).hexdigest() == crown['textureSha256']
        model = Bmd(data)
        assert len(model.faces) == crown['triangles'] and len(model.bones) == 1
        assert crown['bottom'] == min(v[0][2] for f in model.faces for v in f)
        assert all(math.isfinite(c) for face in model.faces for vertex in face for c in vertex[0])
        assert (folder/'model.pqc').read_text().splitlines()[0] == f'$body pixelmon_backport:textures/tera/crowns/{crown["type"]}/crown.bmd'
        with Image.open(folder/'texture.png') as image: image.verify()
    catalog = []
    profiles=json.loads((ROOT/'resources/backport/tera_attachments.json').read_text(encoding='utf-8'))['models']
    for model_ref, profile in profiles.items():
        assert model_ref.startswith('pixelmon_backport:')
        assert profile['bone']=='Head' and len(profile['position'])==3 and all(math.isfinite(v) for v in profile['position']) and profile['scale']>0
        body=ROOT/'resources/assets/pixelmon_backport/textures/pokemon/025_ash_cap'/('shiny.bmd' if 'shiny' in model_ref else 'none.bmd')
        assert any(name==profile['bone'] for _,name in Bmd(body.read_bytes()).bones)
        assert -profile['position'][2]>21.15, 'Custom CapPikachu crown seat must clear the original hat dome'
    with zipfile.ZipFile(args.pixelmon_jar) as native:
        native_names=set(native.namelist())
        def read(path):
            local = ROOT/'resources'/path
            if local.exists(): return local.read_bytes()
            # ResourceLocation normalizes resource paths to lower case.
            return native.read(path if path in native_names else path.lower())
        paths = set(n for n in native.namelist() if '/pokemon/' in n and n.endswith('.pqc'))
        paths.update(p.relative_to(ROOT/'resources').as_posix() for p in (ROOT/'resources').rglob('*.pqc') if '/pokemon/' in p.as_posix())
        for path in sorted(paths):
            bodies = [line.split()[1] for line in read(path).decode().splitlines() if line.startswith('$body ')]
            for body in bodies:
                if ':' in body:
                    namespace, relative = body.split(':', 1)
                    asset = f'assets/{namespace}/'+relative
                else:
                    asset = str(Path(path).parent/body).replace('\\', '/')
                data = read(asset)
                if asset.endswith('.bmd'):
                    model = Bmd(data)
                    assert model.faces, asset
                    heads = [name for parent, name in model.bones if score(name)]
                    catalog.append(dict(pqc=path, body=asset, triangles=len(model.faces),
                        placement='head-bone' if heads else 'mesh-top-fallback', candidateHeads=heads))
                else:
                    catalog.append(dict(pqc=path, body=asset, placement='native-non-bmd-fallback'))
    result = dict(crowns=19, customCapProfiles=len(profiles), pqcCount=len(paths), bodyCount=len(catalog),
        headBone=sum(v['placement']=='head-bone' for v in catalog),
        meshFallback=sum(v['placement']=='mesh-top-fallback' for v in catalog),
        otherFallback=sum(v['placement']=='native-non-bmd-fallback' for v in catalog),
        note='Resource/code coverage only. Crown fit and actual gameplay require manual acceptance; sprite-only graphics settings deliberately keep native sprite rendering.',
        models=catalog)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
    print({k:v for k,v in result.items() if k not in ('models','note')})


if __name__ == '__main__': main()
