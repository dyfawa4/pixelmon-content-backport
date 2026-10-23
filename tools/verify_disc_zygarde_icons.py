"""Targeted resource checks for the reported disc and three Zygarde toast icons."""
import io,json,sys,zipfile
from pathlib import Path
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'resources'
with zipfile.ZipFile(sys.argv[1]) as native:
    species=json.loads(native.read('data/pixelmon/species/718_zygarde.json'))
    forms={f['name'] for f in species['forms']}
    patch=next(p['species'] for p in json.loads((RES/'backport/content.json').read_text('utf8'))['patches'] if p['species'].get('dex')==718)
    forms.update(f['name'] for f in patch['forms'])
    for slug,form in [('zygarde_power_construct','fifty_percent'),('zygarde_complete','complete'),('zygarde_mega','mega')]:
        icon=json.loads((RES/f'data/pixelmon_backport/advancements/legends/{slug}.json').read_text('utf8'))['display']['icon']
        assert icon['item']=='pixelmon:pixelmon_sprite'
        assert f'form:"{form}"' in icon['nbt'] and form in forms
        if form=='mega':
            palette=next(f for f in patch['forms'] if f['name']==form)['genderProperties'][0]['palettes'][0]
            namespace,path=palette['sprite'].split(':')
            texture=RES/f'assets/{namespace}/textures/{path}'
            assert texture.is_file(),texture
            image=Image.open(texture)
            # Imported reference portraits need explicit atlas registration; direct UI loads do not.
            handler=(ROOT/'src/local/pixelmon/backport/client/ReferenceSpriteAtlas.java').read_text()
            assert 'textures/reference/pokemon' in handler and 'event.addSprite' in handler
            assert 'Dist.CLIENT' in handler and 'AtlasTexture.field_110575_b' in handler
        else:
            image=Image.open(io.BytesIO(native.read(f'assets/pixelmon/textures/pokemon/718_zygarde/all/{form}/none/sprite.png')))
        assert image.convert('RGBA').getchannel('A').getbbox()
        print(slug,form,image.size,'PASS')
    disc=Image.open(RES/'assets/pixelmon_backport/textures/item/wild_music_disc_two.png').convert('RGBA')
    reference=Image.open(io.BytesIO(native.read('assets/pixelmon/textures/items/discs/wild_disc.png'))).convert('RGBA')
    assert disc.size==reference.size==(16,16)
    assert disc.getchannel('A').tobytes()==reference.getchannel('A').tobytes(),'Native silhouette and slot occupancy'
    assert disc.tobytes()!=reference.tobytes(),'Distinct second disc'
    model=json.loads((RES/'assets/pixelmon_backport/models/item/wild_music_disc_two.json').read_text())
    assert model['parent']=='pixelmon:item/pixelmon_item'
    print('DISC_PASS native resolution, exact native alpha silhouette, distinct art, native model parent')
print('TARGETED_RESOURCE_PASS; no game launch or broad regression')
