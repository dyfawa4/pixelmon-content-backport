"""Batch-audit native/backported body selection without launching individual Pokemon."""
import argparse,json,math,re,zipfile
from pathlib import Path
from convert_content import Bmd
ROOT=Path(__file__).resolve().parents[1]
EXCLUDE=re.compile(r'wing|arm|leg|foot|hand|tail|neck|head|face|mouth|jaw|eye|ear|horn|hair|flower|leaf|fin|skin|mesh|fire|flame|mane|effect',re.I)
def score(name):
    n=name.lower()
    if EXCLUDE.search(n):return 0
    return 100 if 'shell' in n or 'carapace' in n else 95 if 'torso' in n else 90 if any(k in n for k in ('body','belly','abdomen')) else 80 if 'spine' in n or 'chest' in n else 75 if any(k in n for k in ('waist','pelvis','hip')) else 5 if 'root' in n or 'origin' in n else 0
def select(model):
    vertices={};weights={i:set() for i in range(len(model.bones))};children={i:[] for i in weights}
    for i,(parent,name) in enumerate(model.bones):
        if parent>=0:children[parent].append(i)
    for f in model.faces:
        for coords,influences in f:
            p=tuple(coords[:3]);vertices[p]=True
            for bone,w in influences:
                if w>=.2:weights[bone].add(p)
    def collect(i):
        result=set(weights[i])
        for c in children[i]:
            if not EXCLUDE.search(model.bones[c][1]) and (score(model.bones[c][1])>=5 or score(model.bones[i][1])<5):result.update(collect(c))
        return result
    candidates=[]
    for i,(_,name) in enumerate(model.bones):
        if not EXCLUDE.search(name):
            points=collect(i)
            if len(points)>=max(24,len(vertices)//50):candidates.append((score(name),len(points),name,points))
    anatomical=[row for row in candidates if row[0]>=75]
    if anatomical:return 'torso:'+','.join(row[2] for row in anatomical),set.union(*(row[3] for row in anatomical))
    if not candidates:return 'authored-xz-mesh-fallback',set(vertices)
    winner=max(candidates,key=lambda row:row[:2]);return winner[2],winner[3]
def main():
    parser=argparse.ArgumentParser();parser.add_argument('--pixelmon-jar',type=Path,required=True);parser.add_argument('--output',type=Path,default=ROOT/'docs/RIDING_MODEL_AUDIT_1.5.0.json');args=parser.parse_args()
    rows=[];seen=set();rotation_checks=0;old_box_drifts=0
    with zipfile.ZipFile(args.pixelmon_jar) as archive:
        names=set(archive.namelist());pqc={n for n in names if '/pokemon/' in n and n.endswith('.pqc')};pqc.update(p.relative_to(ROOT/'resources').as_posix() for p in (ROOT/'resources/assets').rglob('*.pqc') if '/pokemon/' in p.as_posix())
        def read(path):
            local=ROOT/'resources'/path
            return local.read_bytes() if local.is_file() else archive.read(path if path in names else path.lower())
        for path in sorted(pqc):
            for line in read(path).decode().splitlines():
                if not line.startswith('$body '):continue
                ref=line.split()[1];asset='assets/'+ref.replace(':','/',1) if ':' in ref else (Path(path).parent/ref).as_posix()
                if asset in seen:continue
                seen.add(asset)
                if not asset.endswith('.bmd'):rows.append(dict(body=asset,selection='non-bmd-native-fallback'));continue
                model=Bmd(read(asset));chosen,points=select(model);assert points,asset
                bounds=[(min(p[i] for p in points),max(p[i] for p in points)) for i in range(3)];assert all(math.isfinite(x) for pair in bounds for x in pair),asset
                # Regression: a world-axis bounding-box midpoint of an asymmetric
                # torso moves as it turns. Undo heading before finding the seat.
                center=((bounds[0][0]+bounds[0][1])/2,(bounds[1][0]+bounds[1][1])/2);drift=False
                for degrees in (30,90,140,275):
                    angle=math.radians(degrees);c,s=math.cos(angle),math.sin(angle)
                    turned=[(p[0]*c-p[1]*s,p[0]*s+p[1]*c) for p in points]
                    restored=[(p[0]*c+p[1]*s,-p[0]*s+p[1]*c) for p in turned]
                    recovered=tuple((min(p[i] for p in restored)+max(p[i] for p in restored))/2 for i in (0,1))
                    assert math.dist(center,recovered)<1e-5*max(1,abs(center[0]),abs(center[1])),asset
                    world_box=tuple((min(p[i] for p in turned)+max(p[i] for p in turned))/2 for i in (0,1));expected=(center[0]*c-center[1]*s,center[0]*s+center[1]*c)
                    drift=drift or math.dist(world_box,expected)>.01;rotation_checks+=1
                old_box_drifts+=int(drift)
                rows.append(dict(body=asset,selection=chosen,vertices=len(points),bounds=bounds))
    output=dict(bodyCount=len(rows),anatomical=sum(r['selection'].startswith('torso:') for r in rows),fallback=sum(r['selection'].endswith('fallback') for r in rows),rotationChecks=rotation_checks,oldWorldBoxDriftModels=old_box_drifts,frame='heading-local with animated torso and head landmarks; shoulder bias 0.18',note='Bind-pose resource coverage only. Runtime uses animated torso surfaces, original horizontal riding offsets as a fallback, and camera clearance. Meshes are never cut. No added per-species offsets.',models=rows)
    args.output.write_text(json.dumps(output,ensure_ascii=False,indent=2)+'\n',encoding='utf-8');print({k:v for k,v in output.items() if k not in ('models','note')})
    lapras=next(r for r in rows if '131_lapras/all/base/none/lapras.bmd' in r['body']);assert lapras['selection']=='torso:Waist',lapras
if __name__=='__main__':main()
