"""Export only approved city terrain and its generation marker, never player files."""
import argparse,copy,hashlib,json,pathlib,shutil
import nbtlib

ROOT=pathlib.Path(__file__).resolve().parents[1]
TARGET=ROOT/'test-maps/gym-saves-v143/backport-gym-v143-rock-vanilla'

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--source',type=pathlib.Path,required=True);args=parser.parse_args()
    source=args.source.resolve();assert source!=TARGET.resolve()
    marker=nbtlib.load(source/'data/backport_gym_test.dat')
    assert bool(marker['data']['Ready']) and int(marker['data']['Cursor'])==97,'Expected the accepted 97-piece city'
    # Start from the repository's clean world metadata; do not copy runtime
    # accounts, Player, capabilities, FTB teams/quests or external Pokemon data.
    world=copy.deepcopy(nbtlib.load(TARGET/'level.dat'));data=world['Data'];data.pop('Player',None)
    data['LevelName']=nbtlib.String('回迁道馆 1.4.3 岩石城已验收布局 [rock]')
    assert int(data['WorldGenSettings']['seed'])==143143
    assert 'Player' not in data
    world.save(TARGET/'level.dat',gzipped=True)
    files={}
    for file in sorted((source/'region').glob('*.mca')):
        before=file.stat().st_mtime_ns;blob=file.read_bytes();assert before==file.stat().st_mtime_ns,'Source region changed while reading'
        out=TARGET/'region'/file.name;out.parent.mkdir(parents=True,exist_ok=True);out.write_bytes(blob)
        files['region/'+file.name]=hashlib.sha256(blob).hexdigest()
    assert files,'No accepted city terrain found'
    out=TARGET/'data/backport_gym_test.dat';out.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(source/'data/backport_gym_test.dat',out)
    files['data/backport_gym_test.dat']=hashlib.sha256(out.read_bytes()).hexdigest()
    report={'acceptedOn':'2026-10-15','cityPieces':97,'seed':143143,'sourceScreenshot':'a5e2bad4','files':files,
        'included':'Clean repository level.dat, approved region terrain and city-ready marker only',
        'excluded':['Player','playerdata','stats','advancements','pokemon','FTB teams/quests','serverconfig','session.lock','logs']}
    (TARGET/'APPROVED_LAYOUT.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
    print('PASS approved 97-piece rock city exported; clean level metadata, exact terrain hashes, no player directories')

if __name__=='__main__':main()
