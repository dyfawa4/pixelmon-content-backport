"""Inspect reference dimensions and native-loader constraints, without mutation."""
import argparse, json, pathlib, zipfile
from convert_content import Bmd, asset

root=pathlib.Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--reference',required=True,type=pathlib.Path)
p.add_argument('--old',required=True,type=pathlib.Path)
a=p.parse_args()
reference=a.reference
z=zipfile.ZipFile(reference)
data=json.loads((root/'resources/backport/content.json').read_text(encoding='utf-8'))
for patch in data['patches']:
    species=patch['species']
    if species['dex'] not in (1017,1024):continue
    for form in species['forms']:
        print(species['dex'],form['name'],{k:form[k] for k in ('height','width','length','weight','dimensions') if k in form})
        for g in form.get('genderProperties',[]):
            for palette in g.get('palettes',[]):
                if palette['name']!='none':continue
                for locator in palette['modelLocator']['pqc']:
                    path=root/'resources'/asset(locator)
                    lines=path.read_text().splitlines()
                    print(' PQC',lines)
                    ref=next(l.split()[1] for l in lines if l.startswith('$body'))
                    ns,part=ref.split(':',1)
                    body=root/'resources/assets'/ns/part
                    if body.suffix!='.bmd':continue
                    m=Bmd(body.read_bytes())
                    print(' bones',m.bones[:4], 'root pose',m.frames[0].get(0))
                    print(' XYZ bounds',[(min(v[0][a] for f in m.faces for v in f),max(v[0][a] for f in m.faces for v in f)) for a in range(3)])
                    print(' implicit linked',sum(b==i for f in m.faces for v in f for b,w in v[1] for i,(_,name) in enumerate(m.bones) if name=='blender_implicit'))
old=zipfile.ZipFile(a.old)
print(' Renderer classes',[n for n in old.namelist() if ('RenderPixelmon' in n or 'PixelmonModelHolder' in n) and n.endswith('.class')])
