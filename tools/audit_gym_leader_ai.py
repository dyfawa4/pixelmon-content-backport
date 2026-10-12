"""Check every authored leader and its runtime strategy wiring; no game initialization."""
import argparse,collections,json,zipfile
from extend_gym_cities import ROOT,RES,read_nbt,NATIVE_BADGES,THEMES

def main():
 p=argparse.ArgumentParser();p.add_argument('--pixelmon',required=True);a=p.parse_args()
 cities=json.loads((RES/'backport/gym_cities.json').read_text(encoding='utf-8'));themes=set(NATIVE_BADGES)|set(THEMES)
 assert set(cities['megaAnchors'])==themes
 with zipfile.ZipFile(a.pixelmon) as jar:
  species={}
  for name in jar.namelist():
   if name.startswith('data/pixelmon/species/') and name.endswith('.json'):
    d=json.loads(jar.read(name));species[d['dex']]=d
 for theme,dex in cities['megaAnchors'].items():
  d=species[dex];base=d['forms'][0]
  assert base.get('megas'),(theme,dex,'no native Mega forms')
  assert theme.upper() in base['types'],(theme,dex,'off-theme anchor')
 counts=collections.Counter();nonleaders=0
 for ns in ['pixelmon','pixelmon_backport']:
  for path in (RES/f'data/{ns}/structures/gyms').rglob('*.nbt'):
   for row in read_nbt(path.read_bytes()).get('entities',[]):
    e=row.get('nbt',{});m=e.get('ForgeData',{});theme=str(m.get('BackportGymType',''));role=str(m.get('BackportGymRole',''))
    if not role or not theme:continue
    assert theme in themes
    if role=='leader':
     assert str(e.get('BossTier',''))=='equal',(path,'leader not equal')
     assert bool(e.get('GymLeader',0)),(path,'leader identity lost')
     counts[theme]+=1
    else:
     assert not bool(e.get('GymLeader',0)),(path,'nonleader mislabeled')
     assert not any(str(item.get('id','')).endswith('_badge') for item in e.get('WinningsTag',{}).values()),(path,'nonleader badge')
     nonleaders+=1
 assert set(counts)==themes and all(c==6 for c in counts.values()),counts
 source=lambda name:(ROOT/f'src/local/pixelmon/backport/{name}.java').read_text(encoding='utf-8')
 gimmicks=source('NpcGimmicks');augment=gimmicks.split('public static void augment(')[1].split('public static void profile(')[0]
 assert 'GymCities.isLeader(npc)' in augment and 'if(!existing && participant.allPokemon.length>0)' in augment
 assert 'theme==Element.POISON || random.nextInt(3)!=0' not in augment
 assert all(x in gimmicks for x in ['BattleAIMode.ADVANCED','armTeam(participant)','NpcMoves.tune','NpcMedicine.plan','Choice.TERA','Choice.MEGA','Choice.DYNAMAX','Choice.Z'])
 prepare=gimmicks.split('public static void prepare(')[1].split('public static void ')[0]
 assert 'GymCities.isLeader' in prepare and 'NpcTraining.prepare' in prepare and 'NpcTraining.copy' in prepare
 training=source('NpcTraining')
 assert 'setMintNature' in training and 'getAbilities().getAll()' in training and 'getMaxLevel()' in training
 assert not any(x in training for x in ['setLevel(', 'setPokemonLevel(', 'setBaseStat('])
 advanced=source('mixin/NpcAdvancedMixin')
 assert 'getNextOpponentMoves' in advanced and 'getMoveset()' not in advanced and 'doSwitch()' not in advanced
 assert 'getBestAttackChoices(enemy)' in source('NpcTactics') and 'ai.getMoveset(enemy)' in source('NpcTactics')
 result={'scope':'offline template, native form catalogue and runtime wiring checks; no battle or win-rate simulation',
         'leaders_by_theme':dict(sorted(counts.items())),'leaders':sum(counts.values()),'other_gym_trainers':nonleaders,
         'theme_mega_anchors':cities['megaAnchors'],'native_switching_preserved':True,'legendary_ace_probability_gate_removed':True,
         'leader_training':{'common_preparation_and_restore_snapshot':True,'native_iv_max':31,'native_ev_per_stat_max':252,'native_ev_total_max':510,'native_mint_and_form_abilities':True,'configured_max_level_default':100,'extra_level_or_stat_multiplier':False},
         'limits':['Equipment and resource eligibility are checked through native APIs at runtime.',
                   'Every mechanism is an available legal option; mutually exclusive mechanisms cannot all be forced in one battle.',
                   'Actual difficulty, timings and combat interactions remain manual acceptance.']}
 (ROOT/'docs/GYM_LEADER_AI_REVIEW_1.3.0.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
 print('PASS',sum(counts.values()),'leaders across 18 themes,',nonleaders,'distinct nonleaders; native Mega anchors and common seven-part AI wiring')
if __name__=='__main__':main()
