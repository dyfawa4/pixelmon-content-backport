"""Exclude exact TCG shop template entries while retaining every other entry and weight."""
from pathlib import Path
from zipfile import ZipFile
import json
ROOT=Path(__file__).resolve().parents[1];resources=ROOT/'resources'
sources={}
with ZipFile(ROOT/'.cache/map-search-full-1547/mods/Pixelmon-legacy.jar') as archive:
    for name in archive.namelist():
        if name.endswith('.json') and '/worldgen/template_pool/' in name and b'tcg_shop' in archive.read(name):sources[name]=json.loads(archive.read(name))
for path in resources.glob('data/**/worldgen/template_pool/**/*.json'):
    name=path.relative_to(resources).as_posix()
    tree=json.loads(path.read_text('utf8'))
    if 'tcg_shop' in str(tree) or name in sources:sources[name]=tree
removed={}
for name,tree in sources.items():
    before=tree.get('elements',[])
    selected=[entry for entry in before if '_tcg_shop' in entry.get('element',{}).get('location','')]
    if not selected:continue
    tree['elements']=[entry for entry in before if entry not in selected]
    assert tree['elements'] and all(entry in before for entry in tree['elements'])
    path=resources/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(tree,ensure_ascii=False,indent=2)+'\n','utf8')
    removed[name]=[entry['element']['location'] for entry in selected]
(ROOT/'.cache/update-180/tcg-pool-removal.json').write_text(json.dumps(removed,ensure_ascii=False,indent=2)+'\n','utf8')
print(f'Removed {sum(map(len,removed.values()))} exact TCG shop entries from {len(removed)} pools; other entries/weights unchanged')
