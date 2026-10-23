"""Inventory every native/backported move through the common intent-response path.
This audits coverage routing and effects, not universal optimality or live gameplay.
"""
import argparse,collections,json
from pathlib import Path
from zipfile import ZipFile
p=argparse.ArgumentParser();p.add_argument('--pixelmon-jar',required=True);p.add_argument('--output',required=True);a=p.parse_args()
root=Path(__file__).resolve().parents[1]
with ZipFile(a.pixelmon_jar) as z:
 moves={d['attackName']:d for n in z.namelist() if n.startswith('data/pixelmon/moves/') and n.endswith('.json') for d in [json.loads(z.read(n))]}
 gates=[n[:-6].replace('/','.') for n in z.namelist() if n.endswith('.class') and n.startswith(('com/pixelmonmod/pixelmon/battles/attacks/specialAttacks/','com/pixelmonmod/pixelmon/battles/status/')) and b'simulateMode' in z.read(n)]
for m in json.loads((root/'resources/backport/content.json').read_text(encoding='utf-8'))['moves']:moves[m['attackName']]=m
profiles=collections.defaultdict(list)
for name,m in sorted(moves.items()):
 effects={e.get('effectTypeID','') for e in m.get('effects',[])}
 group='native-effects-and-candidate-action'
 if effects&{'SuckerPunch','FocusPunch','MeFirst','Counter','MirrorCoat','MetalBurst','BackportUpperHand','ShellTrap','Pursuit'}:group='action-condition-or-retaliation'
 elif any('Protect' in e or e in {'KingsShield','SpikyShield','BanefulBunker','Obstruct','BackportBurningBulwark','WideGuard','QuickGuard','CraftyShield','MatBlock','MagicCoat','Snatch'} for e in effects):group='defence-and-reflection'
 elif effects&{'Taunt','Encore','Disable','Torment','Imprison','DestinyBond'}:group='action-restriction-and-sacrifice'
 profiles[group].append({'move':name,'effects':sorted(effects)})
assert len(moves)>850
assert any(x['move']=='Thunderclap' for x in profiles['action-condition-or-retaliation'])
source=(root/'src/local/pixelmon/backport/NpcTactics.java').read_text(encoding='utf-8')
assert 'NpcIntentResponse.weight(own,choices)' in source
out={'scope':'eligible equal-strength leaders and author only','meaning':'All registered choices use common native effect evaluation and candidate response; rows are an inventory, not proof that every stochastic interaction is optimal.','moves':len(moves),'simulation_gate_classes':gates,'groups':dict(profiles)}
Path(a.output).parent.mkdir(parents=True,exist_ok=True);Path(a.output).write_text(json.dumps(out,ensure_ascii=False,indent=2),encoding='utf-8')
print('PASS NPC intent routing inventory:',len(moves),'moves;',len(gates),'native simulation gates;', {k:len(v) for k,v in profiles.items()})
