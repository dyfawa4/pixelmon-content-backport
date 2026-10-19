"""Check review artifacts, direct saves layout, Java version and source coverage."""
import argparse, hashlib, io, json, pathlib, struct, zipfile

ROOT=pathlib.Path(__file__).resolve().parents[1]
DIST=ROOT/'dist'
parser=argparse.ArgumentParser();parser.add_argument('--version',default='1.3.0');parser.add_argument('--review-only',action='store_true');ARGS=parser.parse_args();VERSION=ARGS.version

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
        expected=['data/pixelmon_backport/advancements/legends/capture_493.json']
        if VERSION in {'1.4.0', '1.4.1', '1.4.2', '1.4.3', '1.4.4', '1.5.0', '1.6.0', '1.6.1'}:expected.append('data/pixelmon_backport/advancements/legends/capture_1026.json')
        if VERSION == '1.6.1':expected.append('data/pixelmon_backport/advancements/legends/palette_249_drowned.json')
        assert sorted(name for name in nodes if json.loads(archive.read(name))['display']['frame']=='challenge')==sorted(expected)
        if VERSION=='1.3.0':assert all(json.loads(archive.read(name))['display']['hidden'] for name in nodes)
    packages=[('test-maps',9),('gym-test-maps',21 if VERSION in {'1.4.3', '1.4.4', '1.5.0'} else 20 if VERSION in {'1.4.0', '1.4.1', '1.4.2', '1.4.3', '1.4.4', '1.5.0'} else 19)]
    if VERSION in {'1.4.0', '1.4.1', '1.4.2', '1.4.3', '1.4.4', '1.5.0'}:packages.append(('cap-test-map',1))
    for suffix,count in packages:
        with zipfile.ZipFile(DIST/f'Pixelmon-Backport-{suffix}-{VERSION}.zip') as archive:
            assert archive.testzip() is None
            names=archive.namelist()
            worlds=[name for name in names if name.startswith('saves/') and name.endswith('/level.dat')]
            assert len(worlds)==count and all(len(name.split('/'))==3 for name in worlds), worlds
            assert not any(name.endswith('.zip') or '/playerdata/' in name for name in names)
            if VERSION in {'1.4.3', '1.4.4', '1.5.0'} and suffix=='gym-test-maps':
                prefix='saves/backport-gym-v143-rock-vanilla/'
                accepted=json.loads(archive.read(prefix+'APPROVED_LAYOUT.json'))
                assert accepted['cityPieces']==97 and accepted['seed']==143143
                for relative,digest in accepted['files'].items():
                    assert hashlib.sha256(archive.read(prefix+relative)).hexdigest()==digest,relative
                for name in names:
                    if not name.startswith(prefix):continue
                    relative=name[len(prefix):]
                    if not relative or relative.endswith('/'):continue
                    assert relative in {'level.dat','APPROVED_LAYOUT.json',*accepted['files']},name
            mods=[name for name in names if name.startswith('mods/') and name.endswith('.jar')]
            if VERSION in {'1.4.0', '1.4.1', '1.4.2', '1.4.3', '1.4.4', '1.5.0'}:
                assert len(mods)==2
                assert archive.read('mods/'+production.name)==production.read_bytes(),(suffix,'stale production jar')
                helper=next(name for name in mods if name!='mods/'+production.name);jar(archive.read(helper),helper=True)
            else:
                assert len(mods)==1;jar(archive.read(mods[0]),helper=True)
    if ARGS.review_only:
        print(f'PASS unpublished review artifacts: Java 11 classes={classes}; direct saves={packages}; metadata/CRC and exact current addon in every test package; no playerdata/automatic QA.')
        return
    with zipfile.ZipFile(DIST/f'Pixelmon-Backport-{VERSION}.zip') as archive:
        assert archive.testzip() is None
        assert archive.read('mods/'+production.name)==production.read_bytes()
        assert f'docs/VALIDATION_{VERSION}.md' in archive.namelist()
        if VERSION in {'1.4.0', '1.4.1', '1.4.2', '1.4.3', '1.4.4', '1.5.0'}:
            assert archive.read('HANDBOOK.md')==(ROOT/'HANDBOOK.md').read_bytes()
            assert archive.read(f'docs/CAP_PIKACHU_{VERSION}.md')==(ROOT/f'docs/CAP_PIKACHU_{VERSION}.md').read_bytes()
            assert (DIST/f'Pixelmon-Backport-Cap-Pikachu-{VERSION}.md').read_bytes()==(ROOT/f'docs/CAP_PIKACHU_{VERSION}.md').read_bytes()
    if VERSION in {'1.4.1', '1.4.2', '1.4.3', '1.4.4', '1.5.0'}:
        assert (DIST/f'Pixelmon-Backport-Greninja-{VERSION}.md').read_bytes()==(ROOT/f'docs/GRENINJA_{VERSION}.md').read_bytes()
    with zipfile.ZipFile(DIST/f'Pixelmon-Backport-source-{VERSION}.zip') as archive:
        assert archive.testzip() is None
        names=set(archive.namelist())
        assert not any(name.startswith(('.git/','.cache/','dist/')) or name.endswith(('.jar','.class','.log')) for name in names)
        for folder in ['src','resources','tests','tools','test-maps']:
            for path in (ROOT/folder).rglob('*'):
                if path.is_file() and not any(part=='__pycache__' for part in path.parts) and path.suffix not in ['.pyc','.class','.jar','.log']:
                    assert path.relative_to(ROOT).as_posix() in names,path
        if VERSION=='1.4.4':
            launcher=archive.read('tools/清理旧缓存.cmd')
            assert launcher.isascii() and launcher.count(b'\n')==launcher.count(b'\r\n'), 'CMD archive must preserve CRLF'
            preview=archive.read('tools/cleanup-local.ps1')
            assert preview.startswith(b'\xef\xbb\xbf') and b'Remove-Item' not in preview, 'Cleanup must remain preview-only'
            for name in ['docs/TERA_RAID_CAPTURE_1.4.4.md','docs/LOCAL_CLEANUP_SAFETY_2026-10-16.md','tests/RaidCaptureParityVerification.java']:
                assert archive.read(name)==(ROOT/name).read_bytes(), name
        expected=sum(1 for path in (ROOT/'test-maps').rglob('level.dat'))
        assert sum(name.endswith('/level.dat') for name in names)==expected
    for line in (DIST/f'SHA256SUMS-{VERSION}.txt').read_text().splitlines():
        digest,name=line.split('  ',1)
        assert hashlib.sha256((DIST/name).read_bytes()).hexdigest()==digest,name
    print(f'PASS release artifacts: Java 11 classes={classes}; direct saves={packages}; complete source; metadata/CRC/SHA256; no internal automatic QA in map helper.')

if __name__=='__main__':main()
