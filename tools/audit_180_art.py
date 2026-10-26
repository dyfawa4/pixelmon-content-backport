"""Verify shipped reviewed art against the provided source, independent of screenshots or file counts."""
from pathlib import Path
from zipfile import ZipFile
import argparse,json,hashlib
ROOT=Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser();p.add_argument('source',type=Path);a=p.parse_args()
manifest=json.loads((ROOT/'resources/backport/upstream_art_180.json').read_text('utf8'))
with ZipFile(a.source) as source:
    assert len(manifest['items'])==34
    for row in manifest['items']:
        raw=source.read(row['source']);assert hashlib.sha256(raw).hexdigest()==row['sha256'],row['id']
        name=row['id'].split(':')[1]
        if name=='max_mushroom':
            upstream=json.loads(raw);upstream.pop('format_version',None)
            for key in upstream['textures']:upstream['textures'][key]='pixelmon_backport:block/upstream942/max_mushroom'
            actual=json.loads((ROOT/'resources/assets/pixelmon_backport/models/block/upstream942/max_mushroom.json').read_text('utf8'))
            assert actual==upstream and len(actual['elements'])==4
            assert (ROOT/'resources/assets/pixelmon_backport/textures/block/upstream942/max_mushroom.png').read_bytes()==source.read('assets/pixelmon/textures/block/max_mushroom.png')
        else:
            assert (ROOT/f'resources/assets/pixelmon_backport/textures/item/upstream942/{name}.png').read_bytes()==raw,row['id']
        model=ROOT/('resources/assets/'+row['model'].replace(':','/models/')+'.json');assert model.is_file(),str(model)
    mesh=json.loads((ROOT/'resources/backport/tera_den_mesh.json').read_text('utf8'))
    assert mesh['source_sha256']==hashlib.sha256(source.read('assets/pixelmon/textures/block/den/tera_raid_den.bmd')).hexdigest()
    assert len(mesh['faces'])==379 and set(mesh['materials'])=={'gem','cave'}
    assert (ROOT/'resources/assets/pixelmon_backport/textures/tera/den/tera_raid_den.png').read_bytes()==source.read('assets/pixelmon/textures/block/den/tera_raid_den.png')
print('ART942_AUDIT_PASS: 7 drinks, 19 plates, 4 masks, umbrella, eject pack, shield, 4-element mushroom, 379-triangle den; original PNG bytes and geometry preserved')
