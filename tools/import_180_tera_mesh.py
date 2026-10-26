"""Flatten reviewed upstream static crystal geometry into the1.16 renderer's vertex layout."""
from pathlib import Path
import io,struct,json,hashlib,sys,argparse,zipfile
sys.path.insert(0,str(Path(__file__).resolve().parent))
from convert_content import legacy_bmd
ROOT=Path(__file__).resolve().parents[1];folder=ROOT/'.cache/update-180/upstream/assets/pixelmon/textures/block/den'
p=argparse.ArgumentParser();p.add_argument('--source',type=Path);a=p.parse_args()
archive=zipfile.ZipFile(a.source) if a.source else None
raw=archive.read('assets/pixelmon/textures/block/den/tera_raid_den.bmd') if archive else (folder/'tera_raid_den.bmd').read_bytes();fp=io.BytesIO(legacy_bmd(raw))
def read(fmt):
    s=struct.Struct('>'+fmt);return s.unpack(fp.read(s.size))
def string():
    chars=[]
    while (c:=read('H')[0]):chars.append(chr(c))
    return ''.join(chars)
assert read('B')[0]==1
for _ in range(read('h')[0]):read('hh');string()
for _ in range(read('h')[0]):
    for _ in range(read('h')[0]):read('h6f')
materials=[string() for _ in range(read('h')[0])];faces=[]
for _ in range(read('h')[0]):
    material=read('B')[0];vertices=[]
    for _ in range(3):
        rigid=read('h')[0];x,y,z,nx,ny,nz,u,v=read('8f')
        for _ in range(read('B')[0]):read('hf')
        vertices.append([round(n,6) for n in (x/16,z/16,-y/16,nx,nz,-ny,u,1-v)])
    faces.append([material,vertices])
assert not fp.read()
data={'source':(a.source.name if a.source else 'Pixelmon1.21.1 9.4.1')+' tera_raid_den.bmd','source_sha256':hashlib.sha256(raw).hexdigest(),
      'conversion':'Static original positions/normals/UVs; Z-up to Y-up, sixteen model units per block. No redrawn geometry.',
      'materials':materials,'faces':faces}
path=ROOT/'resources/backport/tera_den_mesh.json';path.write_text(json.dumps(data,separators=(',',':'))+'\n','utf8')
dest=ROOT/'resources/assets/pixelmon_backport/textures/tera/den';dest.mkdir(parents=True,exist_ok=True)
(dest/'tera_raid_den.png').write_bytes(archive.read('assets/pixelmon/textures/block/den/tera_raid_den.png') if archive else (folder/'tera_raid_den.png').read_bytes())
assert set(materials)=={'gem','cave'} and len(faces)==379
print(f'Original Tera den mesh imported: {len(faces)} triangles, {materials}')
