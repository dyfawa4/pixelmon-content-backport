"""Import existing Z-A box icons; extract one native 32x32 frame without redrawing.

Source artwork and attribution remain separate from this project's code license.
"""
import argparse, hashlib, json, pathlib, shutil
from PIL import Image

ROOT=pathlib.Path(__file__).resolve().parents[1]
COMMIT='d430fb553df98ccde5f9f90ece78e36b07d8083c'
REPO='https://github.com/PokemonWorkshop/GameDataPacks'

def run(source_root):
    targets=[]
    for path in sorted((ROOT/'resources/assets/pixelmon_backport/textures/reference/pokemon').rglob('sprite.png')):
        if not any(p.startswith('mega') for p in path.parts):continue
        # Existing native-style artwork is retained verbatim.
        with Image.open(path) as im:
            if im.size!=(64,64):continue
        species=path.relative_to(ROOT/'resources/assets/pixelmon_backport/textures/reference/pokemon').parts[0]
        dex=int(species.split('_',1)[0]);palette=path.parent.name
        assert palette in ('none','shiny'),path
        filename=f"{dex:04d}{'s' if palette=='shiny' else ''}_30.png"
        source_path=f'Gen 9/Legends Z-A/graphics/pokedex/pokeicon/{filename}'
        source=source_root/source_path
        data=json.loads((source_root/f"Gen 9/Legends Z-A/Data/Studio/pokemon/{species.split('_',1)[1]}.json").read_text('utf8'))
        assert data['id']==dex and any(f['form']==30 for f in data['forms']),f'Mega form mapping absent: {species}'
        with Image.open(source) as im:
            assert im.size==(64,32),f'Unexpected source frame layout: {source}'
            frame=im.convert('RGBA').crop((0,0,32,32))
            assert frame.getchannel('A').getbbox(),f'Empty source icon: {source}'
        backup=ROOT/'.cache/mega-avatar-backup'/path.relative_to(ROOT/'resources')
        backup.parent.mkdir(parents=True,exist_ok=True)
        if not backup.exists():shutil.copy2(path,backup)
        frame.save(path)
        targets.append({'dex':dex,'species':species,'form':'mega','palette':palette,
            'resource':str(path.relative_to(ROOT/'resources')).replace('\\','/'),
            'source_path':source_path,'source_sha256':hashlib.sha256(source.read_bytes()).hexdigest(),
            'resource_sha256':hashlib.sha256(path.read_bytes()).hexdigest(),
            'operation':'first 32x32 frame, exact RGBA pixels; no resizing, recoloring or AI generation'})
    manifest={'repository':REPO,'commit':COMMIT,'license':'Pokemon Studio Public License (Attribution, NonCommercial, ShareAlike)',
        'license_url':'https://github.com/PokemonWorkshop/PokemonStudio/blob/develop/LICENSE.md',
        'credits':'Z-A Icon Sprites: Batin, ezerart, Luis779. Compilation/alignment: Aelysya. Pokemon characters belong to Nintendo / Game Freak / The Pokemon Company.',
        'targets':targets}
    assert len(targets)==46,f'Expected 46 model portraits, found {len(targets)}; restore backups before reimporting'
    (ROOT/'resources/backport/mega_avatar_sources.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
    print('Imported 46 existing Mega normal/shiny icons as exact 32x32 static frames; no generation or color conversion.')

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--source-root',type=pathlib.Path,required=True)
    run(parser.parse_args().source_root)
