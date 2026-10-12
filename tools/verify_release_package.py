"""Check review artifacts, direct saves layout, Java version and source coverage."""
import argparse, hashlib, io, json, pathlib, struct, zipfile

ROOT=pathlib.Path(__file__).resolve().parents[1]
DIST=ROOT/'dist'
parser=argparse.ArgumentParser();parser.add_argument('--version',default='1.3.0');VERSION=parser.parse_args().version

def jar(data, helper=False):
    with zipfile.ZipFile(io.BytesIO(data)) as archive:
        assert archive.testzip() is None
        assert json.loads(archive.read('pack.mcmeta'))['pack']['pack_format']==6
        assert f'version="{VERSION}"' in archive.read('META-INF/mods.toml').decode().replace(' ','')
        classes=[name for name in archive.namelist() if name.endswith('.class')]
        assert classes
        for name in classes:
            assert struct.unpack('>H',archive.read(name)[6:8])[0]==55, name
        if helper:
            assert 'MapSupport.class' in classes and 'FeatureTestMaps.class' in classes and 'FeatureStations.class' in classes
            assert not any('Verification' in name or name=='LiveVerification.class' for name in classes)
        return len(classes)

def main():
    production=DIST/f'pixelmon-content-backport-1.16.5-{VERSION}.jar'
    classes=jar(production.read_bytes())
    with zipfile.ZipFile(production) as archive:
        nodes=[name for name in archive.namelist() if name.startswith('data/pixelmon_backport/advancements/legends/') and name.endswith('.json')]
        assert [name for name in nodes if json.loads(archive.read(name))['display']['frame']=='challenge']==['data/pixelmon_backport/advancements/legends/capture_493.json']
        if VERSION=='1.3.0':assert all(json.loads(archive.read(name))['display']['hidden'] for name in nodes)
    for suffix,count in [('test-maps',9),('gym-test-maps',19)]:
        with zipfile.ZipFile(DIST/f'Pixelmon-Backport-{suffix}-{VERSION}.zip') as archive:
            assert archive.testzip() is None
            names=archive.namelist()
            worlds=[name for name in names if name.startswith('saves/') and name.endswith('/level.dat')]
            assert len(worlds)==count and all(len(name.split('/'))==3 for name in worlds), worlds
            assert not any(name.endswith('.zip') or '/playerdata/' in name for name in names)
            mods=[name for name in names if name.startswith('mods/') and name.endswith('.jar')]
            assert len(mods)==1
            jar(archive.read(mods[0]),helper=True)
    with zipfile.ZipFile(DIST/f'Pixelmon-Backport-{VERSION}.zip') as archive:
        assert archive.testzip() is None
        assert archive.read('mods/'+production.name)==production.read_bytes()
        assert f'docs/VALIDATION_{VERSION}.md' in archive.namelist()
    with zipfile.ZipFile(DIST/f'Pixelmon-Backport-source-{VERSION}.zip') as archive:
        assert archive.testzip() is None
        names=set(archive.namelist())
        assert not any(name.startswith(('.git/','.cache/','dist/')) or name.endswith(('.jar','.class','.log')) for name in names)
        for folder in ['src','resources','tests','tools','test-maps']:
            for path in (ROOT/folder).rglob('*'):
                if path.is_file() and not any(part=='__pycache__' for part in path.parts) and path.suffix not in ['.pyc','.class','.jar','.log']:
                    assert path.relative_to(ROOT).as_posix() in names,path
        expected=sum(1 for path in (ROOT/'test-maps').rglob('level.dat'))
        assert sum(name.endswith('/level.dat') for name in names)==expected
    for line in (DIST/f'SHA256SUMS-{VERSION}.txt').read_text().splitlines():
        digest,name=line.split('  ',1)
        assert hashlib.sha256((DIST/name).read_bytes()).hexdigest()==digest,name
    print(f'PASS review artifacts: Java 11 classes={classes}; direct saves=9+19; complete source; metadata/CRC/SHA256; no internal automatic QA in map helper.')

if __name__=='__main__':main()
