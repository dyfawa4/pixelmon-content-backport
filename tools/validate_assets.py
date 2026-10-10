"""Strict structural checks of generated legacy models and their animations."""
import io,json,math,pathlib,struct,shlex
from convert_content import Bmd
root=pathlib.Path(__file__).resolve().parents[1]
assets=root/'resources/assets/pixelmon_backport/textures'

def skeleton(path,animation=False):
    if path.suffix=='.smd':
        lines=path.read_text().splitlines();start=lines.index('nodes')+1;end=lines.index('end',start)
        bones=[]
        for line in lines[start:end]:
            values=shlex.split(line);idx=int(values[0]);parent=int(values[2]);assert idx==len(bones)
            bones.append((parent,values[1]));assert -1<=parent<idx
        return bones
    fp=io.BytesIO(path.read_bytes())
    def read(fmt):
        s=struct.Struct('>'+fmt);return s.unpack(fp.read(s.size))
    assert read('B')[0]==1
    bones=[]
    for i in range(read('h')[0]):
        idx,parent=read('hh');assert idx==i and -1<=parent<i
        name=[]
        while (c:=read('H')[0]):name.append(chr(c))
        bones.append((parent,''.join(name)))
    frames=read('h')[0];assert frames>0
    for frame in range(frames):
        seen=set()
        for _ in range(read('h')[0]):
            idx=read('h')[0];assert 0<=idx<len(bones) and idx not in seen;seen.add(idx)
            assert all(math.isfinite(v) for v in read('6f'))
        if frame==0:assert len(seen)==len(bones),'Incomplete initial skeleton'
    if animation:
        # Upstream exports append an empty material/triangle section to some
        # animation files. Accept only a fully parsed BMD tail, never garbage.
        tail=fp.read()
        if tail:Bmd(path.read_bytes())
    else:
        model=Bmd(path.read_bytes())
        for face in model.faces:
            for coord,weights in face:
                assert all(math.isfinite(v) for v in coord)
                assert all(0<=bone<len(bones) and math.isfinite(weight) and 0<=weight<=1.001 for bone,weight in weights)
                assert abs(sum(w for _,w in weights)-1)<.002,'Invalid skin weights: '+str(path)+' '+str(weights)
                assert all(bones[b][1]!='blender_implicit' for b,w in weights if w>0),'Native loader rejects dummy-bone weights: '+str(path)
    return bones

count=animations=0
def resource_file(value):
    assert ':' in value,'Legacy relative PQC paths use the wrong namespace'
    ns,path=value.split(':',1)
    assert '..' not in path and path.startswith('textures/'),value
    return root/'resources/assets'/ns/path
for pqc in sorted(assets.rglob('*.pqc')):
    body=None;anims=[]
    for line in pqc.read_text().splitlines():
        words=line.split()
        if words[0]=='$body':body=resource_file(words[1])
        elif words[0]=='$anim':anims.append(resource_file(words[2]))
    assert body and body.is_file(),str(pqc)
    bones=skeleton(body)
    for animation in anims:
        assert animation.is_file(),str(animation)
        other=skeleton(animation,True)
        assert bones==other,f'Animation skeleton mismatch: {pqc} / {animation}'
        animations+=1
    count+=1
for path in assets.rglob('*.matrices'):
    raw=path.read_bytes();version,frames,bones=struct.unpack('>iii',raw[:12])
    assert version==1 and frames>0 and bones>0 and len(raw)==12+frames*bones*64
    assert all(math.isfinite(v) for v in struct.unpack('>'+str(frames*bones*16)+'f',raw[12:]))
print(f'PASS asset structure: {count} descriptors, {animations} compatible animations; matrices finite')
