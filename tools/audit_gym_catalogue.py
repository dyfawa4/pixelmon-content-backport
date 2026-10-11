"""Read-only catalogue of upstream gyms, badge rewards and separate team bases."""
import argparse, collections, gzip, hashlib, io, json, pathlib, zipfile
import nbtlib

def audit(path):
    with zipfile.ZipFile(path) as archive:
        names=archive.namelist(); groups=collections.defaultdict(lambda:dict(structures=0,leaders=[],badges=set(),missingRewards=[]))
        for name in names:
            if not name.startswith('data/pixelmon/structures/gyms/') or not name.endswith('.nbt'):continue
            theme=name.split('/')[4];groups[theme]['structures']+=1
            if '_leader_' not in name:continue
            root=nbtlib.File.parse(io.BytesIO(gzip.decompress(archive.read(name))))
            for entity in root.get('entities',[]):
                npc=entity['nbt'];badgeIds=[str(item.get('id','')) for item in npc.get('WinningsTag',{}).values() if int(item.get('Count',0))>0]
                groups[theme]['leaders'].append(dict(template=name.rsplit('/',1)[1],name=str(npc.get('Name','')),trainer=str(npc.get('TrainerIndex','')),greeting=str(npc.get('Greeting','')),firstDex=int(npc.get('pokeStore',{}).get('party0',{}).get('ndex',0)),badges=badgeIds))
                groups[theme]['badges'].update(badgeIds)
                if not badgeIds:groups[theme]['missingRewards'].append(name.rsplit('/',1)[1])
        for value in groups.values():value['badges']=sorted(value['badges'])
        structureClass=archive.read('com/pixelmonmod/pixelmon/world/structure/PixelmonStructureFeatures.class')
        biomeClass=archive.read('com/pixelmonmod/pixelmon/world/generation/AddToBiomes.class')
        configured=[theme for theme in groups if (theme+'_gym').encode() in structureClass and (theme.upper()+'_GYM').encode() in biomeClass]
        bases=[name for name in names if name.startswith('data/pixelmon/structures/teams/') and name.endswith('.nbt')]
        return dict(file=path.name,sha256=hashlib.sha256(path.read_bytes()).hexdigest(),gymTypes=dict(sorted(groups.items())),registeredBiomeGymTypes=sorted(configured),separateTeamStructures=bases)

def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--pixelmon',type=pathlib.Path,required=True);parser.add_argument('--za',type=pathlib.Path);parser.add_argument('--output',type=pathlib.Path,required=True);args=parser.parse_args()
    sources=[audit(args.pixelmon)]
    if args.za:sources.append(audit(args.za))
    args.output.parent.mkdir(parents=True,exist_ok=True);args.output.write_text(json.dumps(sources,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    for source in sources:print(source['file'], 'gym types=',','.join(source['registeredBiomeGymTypes']), 'team pieces=',len(source['separateTeamStructures']))
if __name__=='__main__':main()
