"""Copy reviewed original Pixelmon9.4.2 assets, keeping all registry IDs and gameplay intact."""
from pathlib import Path
from zipfile import ZipFile
import json, hashlib, argparse

ROOT=Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser();p.add_argument('source',type=Path);a=p.parse_args()
archive=ZipFile(a.source)
DEST=ROOT/'resources/assets'
records=[]
def write(relative,data):
    path=DEST/relative;path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(data)
def sprite(name,namespace='pixelmon',target=None):
    model=json.loads(archive.read(f'assets/pixelmon/models/item/{name}.json'))
    original=model['textures']['layer0'].split(':',1)[1]
    data=archive.read(f'assets/pixelmon/textures/{original}.png')
    target=target or name
    texture=f'item/upstream942/{name}'
    write(f'pixelmon_backport/textures/{texture}.png',data)
    # The upstream pixelmon_item parent is exactly item/generated; use the1.16 vanilla parent.
    converted={'parent':'minecraft:item/generated','textures':{'layer0':f'pixelmon_backport:{texture}'}}
    write(f'pixelmon_backport/models/item/upstream942/{name}.json',(json.dumps(converted,ensure_ascii=False,indent=2)+'\n').encode())
    write(f'{namespace}/models/item/{target}.json',json.dumps({'parent':f'pixelmon_backport:item/upstream942/{name}'}).encode())
    records.append({'id':f'{namespace}:{target}','model':f'pixelmon_backport:item/upstream942/{name}','source':f'assets/pixelmon/textures/{original}.png','sha256':hashlib.sha256(data).hexdigest()})

for name in ('blue_juice','red_juice','pink_juice','purple_juice','yellow_juice','green_juice','colorful_shake'):
    sprite(name)
for name in ('teal_mask','wellspring_mask','hearthflame_mask','cornerstone_mask','utility_umbrella','eject_pack','ability_shield'):
    sprite(name,'pixelmon_backport')
plates=('blank','draco','dread','earth','fist','flame','icicle','insect','iron','legend','meadow','mind','pixie','sky','splash','spooky','stone','toxic','zap')
for prefix in plates:
    name=f'{prefix}_plate'
    sprite(name)
    if prefix in ('blank','legend'):
        data=archive.read(f'assets/pixelmon/textures/item/helditems/plates/{prefix}plate.png')
        write(f'pixelmon/textures/items/helditems/{prefix}plate.png',data)
        write(f'pixelmon/textures/item/helditems/plates/{prefix}plate.png',data)
raw=archive.read('assets/pixelmon/models/block/max_mushroom.json');model=json.loads(raw)
# Keep original elements, UVs and GUI/hand transforms. Only relocate texture references.
model.pop('format_version',None)
for key in model['textures']:model['textures'][key]='pixelmon_backport:block/upstream942/max_mushroom'
write('pixelmon_backport/models/block/upstream942/max_mushroom.json',json.dumps(model,indent=2).encode())
tex=archive.read('assets/pixelmon/textures/block/max_mushroom.png')
write('pixelmon_backport/textures/block/upstream942/max_mushroom.png',tex)
item_model=b'{"parent":"pixelmon_backport:block/upstream942/max_mushroom"}\n'
write('pixelmon_backport/models/item/upstream942/max_mushroom.json',item_model)
write('pixelmon/models/block/max_mushroom.json',item_model)
for name in ('max_mushroom','max_mushroom_block'):
    write(f'pixelmon/blockstates/{name}.json',b'{"variants":{"":{"model":"pixelmon_backport:block/upstream942/max_mushroom"}}}\n')
    write(f'pixelmon/models/item/{name}.json',item_model)
records.append({'id':'pixelmon:max_mushroom','model':'pixelmon_backport:item/upstream942/max_mushroom','source':'assets/pixelmon/models/block/max_mushroom.json','sha256':hashlib.sha256(raw).hexdigest(),'texture_sha256':hashlib.sha256(tex).hexdigest(),'elements':len(model['elements'])})
manifest={'source':a.source.name,'items':records,
          'scope':'Only explicitly listed item art and placed Max Mushroom; no warp plates, mechanics, recipes or unrelated blocks.'}
(ROOT/'resources/backport/upstream_art_180.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n','utf8')
print(f'Imported {len(records)} explicit item models/textures and placed mushroom art')
