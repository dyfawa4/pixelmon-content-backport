"""Read-only Minecraft 1.16 overworld den census from saved region snapshots.

Does not load/generate chunks or modify a world. Live unsaved entities are absent;
native Max encounter data is not inferred from missing persisted raid fields.
"""
import argparse,collections,io,json,pathlib,struct,zlib
import nbtlib

def audit(world):
    rows=[];errors=[];chunks=0;unstable=[]
    # Deliberately exclude DIM-1, DIM1 and mod dimensions: this is overworld only.
    for file in sorted((world/'region').glob('*.mca')):
        stamp=file.stat().st_mtime_ns;raw=file.read_bytes()
        if stamp!=file.stat().st_mtime_ns:
            unstable.append(file.name);continue
        if len(raw)<8192:continue
        for index in range(1024):
            offset=int.from_bytes(raw[index*4:index*4+3],'big')*4096
            if not offset:continue
            try:
                size=struct.unpack_from('>I',raw,offset)[0];kind=raw[offset+4]
                payload=raw[offset+5:offset+4+size]
                if kind==1:payload=zlib.decompress(payload,31)
                elif kind==2:payload=zlib.decompress(payload)
                elif kind!=3:raise ValueError(f'Unsupported compression: {kind}')
                root=nbtlib.File.parse(io.BytesIO(payload));level=root.get('Level',root);chunks+=1
                for entity in level.get('Entities',[]):
                    name=str(entity.get('id',''))
                    if name not in ('pixelmon:den','pixelmon_backport:tera_den'):continue
                    tag=entity.get('ForgeData',{})
                    rows.append({'id':name,'pos':list(map(float,entity['Pos'])),'region':file.name,
                        'decoration':bool(entity.get('Decoration',False)),
                        'kind':int(tag.get('BackportDenKind',-1)),
                        'teraRaidSaved':'BackportTeraRaid' in entity,
                        'stateVersion':int(entity.get('BackportTeraStateVersion',0))})
            except Exception as ex:errors.append({'region':file.name,'slot':index,'error':str(ex)})
    physical=[row for row in rows if not row['decoration']]
    counts=dict(collections.Counter(row['id'] for row in physical))
    return {'world':str(world),'dimension':'minecraft:overworld','savedChunks':chunks,
        'scope':'Saved entities only; small/local samples do not establish global generation odds.',
        'physicalNonDecorationCounts':counts,'total':len(physical),'dens':rows,
        'unstableRegionsSkipped':unstable,'errors':errors}

def main():
    parser=argparse.ArgumentParser();parser.add_argument('world',type=pathlib.Path);parser.add_argument('--output',type=pathlib.Path,required=True)
    args=parser.parse_args();report=audit(args.world)
    args.output.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
    print(json.dumps({k:v for k,v in report.items() if k!='dens'},ensure_ascii=False,indent=2))

if __name__=='__main__':main()
