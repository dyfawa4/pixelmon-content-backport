"""Native 32px party icons with a pixel cap; never touches the approved 3D model.

The user explicitly chose editing the native sprite with code. Keep native
normal/shiny body colours, silhouette, transparency and logical pixel size.
"""
import argparse
import io
import json
from pathlib import Path
from zipfile import ZipFile
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
CAP = (
    "  ######    ",
    " #RRWWRR#   ",
    "#RRWWGWRR#  ",
    "#RRWWWWRRR# ",
    "#RRRRRRRRRR#",
    " ########## ",
)
COLOURS = {"#": (64, 62, 59, 255), "R": (196, 34, 43, 255),
           "W": (248, 247, 233, 255), "G": (23, 124, 72, 255)}


def build(jar):
    output = ROOT / 'resources/assets/pixelmon_backport/textures/pokemon/025_ash_cap'
    output.mkdir(parents=True, exist_ok=True)
    with ZipFile(jar) as z:
        species = json.loads(z.read('data/pixelmon/species/025_pikachu.json'))
        palettes = species['forms'][0]['genderProperties'][0]['palettes']
        for name in ('none', 'shiny'):
            palette = next(p for p in palettes if p['name'] == name)
            namespace, path = palette['sprite'].split(':', 1)
            image = Image.open(io.BytesIO(z.read(f'assets/{namespace}/textures/{path}'))).convert('RGBA')
            if image.size != (32, 32):
                raise ValueError('Unexpected native sprite dimensions; review before editing')
            for y, row in enumerate(CAP, 13):
                for x, pixel in enumerate(row, 7):
                    if pixel in COLOURS:
                        image.putpixel((x, y), COLOURS[pixel])
            image.save(output / f'{name}_sprite.png')
            print(f'{name}: native 32x32 sprite + cap, alpha bounds {image.getbbox()}')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--pixelmon', required=True)
    build(parser.parse_args().pixelmon)
