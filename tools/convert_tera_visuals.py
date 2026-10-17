"""Convert official 9.4.1 Tera crowns into legacy static BMD resources."""
import argparse
import hashlib
import json
import math
import struct
from pathlib import Path
from PIL import Image, ImageDraw
from convert_content import Bmd

ROOT = Path(__file__).resolve().parents[1]
TYPES = 'normal fire water grass electric ice fighting poison ground flying psychic bug rock ghost dragon dark steel fairy stellar'.split()


def static_bmd(model):
    # Crowns have no animations. Collapse exporter helper roots into one real
    # rigid root so the legacy multi-root bone traversal cannot drop geometry.
    out = bytearray(struct.pack('>Bhhh', 1, 1, 0, -1))
    out += 'crown\0'.encode('utf-16-be')
    out += struct.pack('>hhh6fh', 1, 1, 0, 0, 0, 0, 0, 0, 0, 1)
    out += 'texture.png\0'.encode('utf-16-be')
    out += struct.pack('>h', len(model.faces))
    for face in model.faces:
        out += b'\0'
        for coordinates, _ in face:
            out += struct.pack('>h8fBhf', 0, *coordinates, 1, 0, 1.0)
    return bytes(out)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--source', type=Path, default=ROOT/'.cache/tera-visual-research/upstream')
    args = parser.parse_args()
    entries = []
    for kind in TYPES:
        folder = args.source/'assets/pixelmon/textures/hat/tera_jewel'/kind
        models = list(folder.glob('*.bmd'))
        if len(models) != 1:
            raise ValueError(f'{kind}: expected one official crown')
        source = models[0].read_bytes()
        model = Bmd(source)
        assert model.faces and len(model.faces) < 32768
        assert all(math.isfinite(v) for face in model.faces for vertex in face for v in vertex[0])
        converted = static_bmd(model)
        check = Bmd(converted)
        assert [v[0] for f in check.faces for v in f] == [v[0] for f in model.faces for v in f]
        dest = ROOT/'resources/assets/pixelmon_backport/textures/tera/crowns'/kind
        dest.mkdir(parents=True, exist_ok=True)
        (dest/'crown.bmd').write_bytes(converted)
        (dest/'model.pqc').write_text(f'$body pixelmon_backport:textures/tera/crowns/{kind}/crown.bmd\n$scale 1\n', encoding='utf-8')
        texture = (folder/'texture.png').read_bytes()
        (dest/'texture.png').write_bytes(texture)
        with Image.open(folder/'texture.png') as image:
            image.verify()
        entries.append(dict(type=kind, source=models[0].relative_to(args.source).as_posix(),
            sourceSha256=hashlib.sha256(source).hexdigest(), textureSha256=hashlib.sha256(texture).hexdigest(),
            convertedSha256=hashlib.sha256(converted).hexdigest(), triangles=len(model.faces),
            bottom=min(vertex[0][2] for face in model.faces for vertex in face)))
    manifest = dict(upstream='Pixelmon 1.21.1 9.4.1', modrinthVersion='tY57GRtu',
        sourceUrl='https://cdn.modrinth.com/data/59ZceYlU/versions/tY57GRtu/Pixelmon-1.21.1-9.4.1-universal.jar',
        conversion='Original vertex positions, normals, UVs and textures retained; static bone weights collapsed to one root.', crowns=entries)
    out = ROOT/'resources/backport/tera_visual_sources.json'
    out.write_text(json.dumps(manifest, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
    # Original procedural facet mask, not a modification of an upstream image.
    image = Image.new('RGBA', (128,128), (255,255,255,55))
    draw = ImageDraw.Draw(image)
    for y in range(0,128,16):
        for x in range(0,128,16):
            shade = 110 + (x*13+y*7)%120
            draw.polygon([(x,y),(x+16,y),(x,y+16)], fill=(shade,shade,255,95))
            draw.polygon([(x+16,y),(x+16,y+16),(x,y+16)], fill=(255,255,255,38))
            draw.line([(x,y+16),(x+16,y)], fill=(255,255,255,160), width=1)
    image.save(ROOT/'resources/assets/pixelmon_backport/textures/tera/facets.png')
    print('Converted all 19 official crowns; preserved all mesh coordinates and texture bytes')


if __name__ == '__main__':
    main()
