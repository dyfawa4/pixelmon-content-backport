"""Offline matchup configuration audit; never simulates or declares a battle win."""
import argparse,json,pathlib,zipfile,collections
from extend_gym_cities import ROOT,RES,read_nbt,NATIVE_BADGES,THEMES

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--pixelmon',required=True);args=parser.parse_args()
    chart=json.loads((ROOT/'docs/GYM_TYPE_CHART_1.3.0.json').read_text(encoding='utf-8'))
    cities=json.loads((RES/'backport/gym_cities.json').read_text(encoding='utf-8'))
    with zipfile.ZipFile(args.pixelmon) as native:
        species={};moves={}
        for name in native.namelist():
            if name.startswith('data/pixelmon/species/') and name.endswith('.json'):
                value=json.loads(native.read(name));species[int(value['dex'])]=value
            elif name.startswith('data/pixelmon/moves/') and name.endswith('.json'):
                value=json.loads(native.read(name));moves[value['attackName']]=value
    def form(dex,name=''):
        data=species[dex];return next((f for f in data['forms'] if f['name']==name),data['forms'][0])
    def effective(attack,types):
        result=1
        for defense in types:result*=chart[attack][defense.lower()]
        return result
    def coverage(names,target):
        return sorted(name for name in names if name in moves and moves[name]['attackCategory']!='STATUS'
            and (moves[name]['basePower']>0 or name in {'Low Kick','Grass Knot','Heavy Slam','Heat Crash','Electro Ball','Gyro Ball','Stored Power','Power Trip','Return','Frustration'})
            and chart[moves[name]['attackType'].lower()][target]>1)
    report={'scope':'Native type chart plus serialized rosters/learnable pools; no AI decision or gameplay win simulation',
            'limits':['Learnable pool coverage is not a claim that all four equipped moves cover every counter.',
                      'Abilities, speed, actual damage, weather, multi-type teams and battle outcomes require manual acceptance.',
                      'Native advanced switching and legal defensive Tera remain part of the runtime strategy.'], 'themes':{}}
    for theme in [*NATIVE_BADGES,*THEMES]:
        weaknesses=[attack for attack in chart if chart[attack][theme]>1]
        defenses={attack:[t for t in chart if chart[attack][t]<=.5] for attack in weaknesses}
        assert all(defenses.values()),theme
        row={'counter_attack_types':weaknesses,'legal_defensive_tera_types':defenses,'scenarios':[]}
        if theme in THEMES:
            pool=cities['newThemes'][theme]['roster'];row['mode']='runtime species pool, not fixed four-move sets';row['species_pool']=pool
            for counter in weaknesses:
                resistant=[];armed={}
                for dex in pool:
                    f=form(dex);types=f.get('types',species[dex]['forms'][0]['types'])
                    if effective(counter,types)<=1:resistant.append(dex)
                    learn=set()
                    for field,values in f['moves'].items():
                        if field=='levelUpMoves':learn.update(name for level in values for name in level['attacks'])
                        elif isinstance(values,list):learn.update(v for v in values if isinstance(v,str))
                    found=coverage(learn,counter)
                    if found:armed[dex]=found[:8]
                row['scenarios'].append({'counter':counter,'nonweak_species':resistant,'legal_countermove_candidates':armed,
                    'note':'Actual four slots use NpcMoves.configure; native ace/utility plans are preserved.'})
        else:
            teams=[]
            for path in (RES/f'data/pixelmon/structures/gyms/{theme}').glob('*.nbt'):
                for entity in read_nbt(path.read_bytes()).get('entities',[]):
                    npc=entity['nbt']
                    if str(npc.get('ForgeData',{}).get('BackportGymRole',''))!='leader':continue
                    party=[]
                    for p in npc.get('pokeStore',{}).values():
                        if not hasattr(p,'keys') or 'ndex' not in p:continue
                        dex=int(p['ndex']);f=form(dex,str(p.get('Variant','')))
                        party.append({'dex':dex,'types':f.get('types',species[dex]['forms'][0]['types']),
                            'ability':str(p.get('Ability','')),'held_item':str(p.get('HeldItemStack',{}).get('id','')),
                            'moves':[str(m['MoveID']) for m in p.get('Moveset',[])]})
                    if party:teams.append({'template':path.name,'party':party})
            assert teams,(theme,'no serialized leader teams');row['mode']='serialized native leader baseline before incremental runtime coverage';row['teams']=teams
            for counter in weaknesses:
                scenarios=[]
                for team in teams:
                    resistant=[p['dex'] for p in team['party'] if effective(counter,p['types'])<=1]
                    armed={p['dex']:coverage(p['moves'],counter) for p in team['party'] if coverage(p['moves'],counter)}
                    immunity={'water':{'WaterAbsorb','StormDrain','DrySkin','DesolateLand'},'fire':{'FlashFire','WellBakedBody','PrimordialSea'},
                        'electric':{'VoltAbsorb','LightningRod','MotorDrive'},'grass':{'SapSipper'},'ground':{'Levitate','EarthEater'}}.get(counter,set())
                    support=[]
                    for p in team['party']:
                        if p['ability'] in immunity:support.append({'dex':p['dex'],'kind':'conditional ability immunity','ability':p['ability']})
                        if counter=='water' and p['ability']=='Drought':support.append({'dex':p['dex'],'kind':'sun halves Water damage while weather is active'})
                        if counter=='water' and p['dex']==383 and p['held_item']=='pixelmon:red_orb':support.append({'dex':383,'kind':'native Primal Reversion and Desolate Land while active'})
                    scenarios.append({'template':team['template'],'nonweak_members':resistant,'equipped_countermoves':armed,
                        'conditional_weather_or_immunity_support':support,'baseline_gap':not resistant and not armed,
                        'unexplained_configuration_gap':not resistant and not armed and not support})
                row['scenarios'].append({'counter':counter,'templates':scenarios})
        report['themes'][theme]=row
    target=ROOT/'docs/GYM_TYPE_RISK_REVIEW_1.3.0.json';target.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    gaps=[(theme,s['counter'],t['template']) for theme,r in report['themes'].items() for s in r['scenarios']
          for t in s.get('templates',[]) if t['baseline_gap']]
    empty=[(theme,s['counter']) for theme,r in report['themes'].items() for s in r['scenarios']
           if 'legal_countermove_candidates' in s and not s['legal_countermove_candidates']]
    print('REVIEW 18 gyms:',sum(len(r['scenarios']) for r in report['themes'].values()),'counter-type scenarios;',
          len(gaps),'native baseline gaps;',len(empty),'new pool gaps; all have legal defensive Tera options.')
    for gap in gaps:print('BASELINE GAP',*gap)
    for gap in empty:print('POOL GAP',*gap)
    unexplained=[(theme,s['counter'],t['template']) for theme,r in report['themes'].items() for s in r['scenarios']
        for t in s.get('templates',[]) if t['unexplained_configuration_gap']]
    print('Unexplained native configuration gaps after including variable-power attacks and conditional weather/ability support:',len(unexplained))
    print('This is configuration evidence, not a simulated win-rate or a promise of difficulty.')
if __name__=='__main__':main()
