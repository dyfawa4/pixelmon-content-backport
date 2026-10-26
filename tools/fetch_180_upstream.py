"""Inspect original 1.21.1 assets first; keep research bytecode out of shipped resources."""
import hashlib,json,zipfile,time
from pathlib import Path
from fetch_tera_visual_upstream import RemoteZip
ROOT=Path(__file__).resolve().parents[1];CACHE=ROOT/'.cache/update-180/upstream';CACHE.mkdir(parents=True,exist_ok=True)
meta=json.loads((ROOT/'.cache/tera-visual-research/pixelmon-941.json').read_text('utf8'))
selected={'ability_shield','utility_umbrella','eject_pack','teal_mask','wellspring_mask','hearthflame_mask','cornerstone_mask',
          'blue_juice','red_juice','pink_juice','purple_juice','yellow_juice','green_juice','colorful_shake','max_mushroom','max_mushroom_block','tera_den_spawner'}
research={'NPCEntityRenderer','NPCEntity','NPCShopkeeper','AbilityShieldTests','AbilityRegistry','PixelmonWrapper','GastroAcid','RolePlay','NeutralizingGas','SkillSwap','MegaStoneItem',
          'WorrySeed','SimpleBeam','Entrainment','WanderingSpirit','Mummy','LingeringAroma','CoreEnforcer','Trace',
          'TeraCrystalDenModelRenderEffect','TeraDenBeamRenderEffect','TeraDustDenParticleRenderEffect','DenRenderer','ItemRegistration'}
class RetryZip(RemoteZip):
    def read(self,n=-1):
        for attempt in range(4):
            try:return super().read(n)
            except OSError:
                if attempt==3:raise
                time.sleep(attempt+1)
remote=RetryZip(next(f for f in meta['files'] if f['primary']))
manifest=[]
with zipfile.ZipFile(remote) as archive:
    names=set(archive.namelist());queue=[]
    for name in sorted(names):
        stem=Path(name).stem
        if name.startswith('assets/pixelmon/models/') and (stem in selected or name.startswith('assets/pixelmon/models/item/') and stem.endswith('_plate') and 'pressure_plate' not in stem):queue.append(name)
        elif name.startswith('assets/pixelmon/blockstates/') and stem in selected:queue.append(name)
        elif name.endswith('.class') and stem in research and ('/api/test/' not in name or stem=='AbilityShieldTests'):queue.append(name)
        elif name in {'assets/pixelmon/textures/block/den/tera_raid_den.bmd','assets/pixelmon/textures/block/den/tera_raid_den.png',
                      'assets/pixelmon/lang/zh_cn.json','assets/pixelmon/lang/en_us.json'}:queue.append(name)
    seen=set()
    while queue:
        name=queue.pop(0)
        if name in seen:continue
        seen.add(name);dest=CACHE/name;dest.parent.mkdir(parents=True,exist_ok=True)
        if not dest.exists():dest.write_bytes(archive.read(name))
        data=dest.read_bytes();manifest.append({'path':name,'sha256':hashlib.sha256(data).hexdigest()})
        print(name,flush=True)
        if name.startswith('assets/') and name.endswith('.json') and '/lang/' not in name:
            tree=json.loads(data)
            def follow(value,key=''):
                if isinstance(value,dict):
                    for k,v in value.items():follow(v,k)
                elif isinstance(value,list):
                    for v in value:follow(v,key)
                elif isinstance(value,str) and value.startswith('pixelmon:'):
                    path=value.split(':',1)[1]
                    for candidate in ['assets/pixelmon/models/'+path+'.json','assets/pixelmon/textures/'+path+'.png',
                                      'assets/pixelmon/textures/'+path]:
                        if candidate in names and candidate not in seen:queue.append(candidate)
            follow(tree)
(CACHE/'manifest.json').write_text(json.dumps({'upstream':'Pixelmon 1.21.1 9.4.1','url':remote.file['url'],'entries':manifest,'network_bytes_this_run':remote.total},ensure_ascii=False,indent=2),'utf8')
print('Fetched/inspected entries:',len(manifest),'network bytes:',remote.total)
