package local.pixelmon.backport;
import java.util.*;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.pokemon.species.Stats;
import com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
/** One cohesive defensive role per author team, without sacrificing form items or team coverage. */
public final class AuthorSupportPlan {
 public static final String TAG="BackportAuthorSupport110";
 private static final Map<Integer,List<String>> PLANS=Map.ofEntries(
  Map.entry(430,List.of("Sucker Punch","Sky Attack","Haze","Snatch")),
  Map.entry(249,List.of("Aeroblast","Roost","Whirlwind","Toxic")),
  Map.entry(250,List.of("Sacred Fire","Brave Bird","Recover","Whirlwind")),
  Map.entry(487,List.of("Hex","Dragon Tail","Will-O-Wisp","Rest")),
  Map.entry(717,List.of("Oblivion Wing","Sucker Punch","Snatch","Taunt")),
  Map.entry(748,List.of("Scald","Recover","Haze","Toxic")),
  Map.entry(630,List.of("Foul Play","Roost","Taunt","Defog")),
  Map.entry(823,List.of("Body Press","U-turn","Roost","Defog")),
  Map.entry(36,List.of("Moonblast","Soft-Boiled","Encore","Thunder Wave")));
 private static boolean has(Pokemon p,String move){return p.getMoveset().stream().anyMatch(a->a.getMove().getAttackName().equals(move));}
 private static boolean legal(Stats form,List<String> moves){return moves.stream().allMatch(n->form.getMoves().getAllMoves().stream().anyMatch(a->a.getAttackName().equals(n)));}
 public static double rank(int dex,Stats form){
  var plan=PLANS.get(dex);if(plan==null||!legal(form,plan))return 0;
  var s=form.getBattleStats();return s.getStat(BattleStatsType.HP)+s.getStat(BattleStatsType.DEFENSE)+s.getStat(BattleStatsType.SPECIAL_DEFENSE)+(plan.contains("Roost")||plan.contains("Recover")?60:0)+(plan.contains("Snatch")&&plan.contains("Haze")?400:0);
 }
 public static boolean support(Pokemon p){return p.getPersistentData().func_74767_n(TAG);}
 private static boolean fits(List<Pokemon> team,Pokemon p,List<String> plan){
  // Do not take away the team's only answer when assigning a new role.
  for(String move:List.of("Haze","Snatch","Defog","Rapid Spin","Court Change","Taunt","Encore"))
   if(has(p,move)&&!plan.contains(move)&&team.stream().noneMatch(q->q!=p&&has(q,move)))return false;
  return true;
 }
 private static boolean coherent(Pokemon p){
  if(p.getMoveset().size()!=4||!legal(p.getForm(),p.getMoveset().stream().map(a->a.getMove().getAttackName()).collect(java.util.stream.Collectors.toList())))return false;
  boolean damage=p.getMoveset().stream().anyMatch(a->a.getAttackCategory()!=com.pixelmonmod.pixelmon.api.battles.AttackCategory.STATUS&&a.getMove().getBasePower()>0);
  long controls=List.of("Haze","Snatch","Whirlwind","Roar","Taunt","Encore","Toxic","Will-O-Wisp","Defog","Thunder Wave").stream().filter(n->has(p,n)).count();
  boolean sustain=p.getMoveset().stream().anyMatch(NpcMoves::recovery)||has(p,"Rest")||has(p,"Oblivion Wing");
  return damage&&controls>0&&(sustain||controls>=2&&has(p,"Sucker Punch"));
 }
 public static void configure(List<Pokemon> team){
  Pokemon chosen=null;double score=0;
  // Scouting may add the only Taunt/Defog to an already assigned wall. Keep that
  // coherent adaptation instead of removing the role or deleting the counter.
  for(var p:team)if(support(p)&&PLANS.containsKey(p.getSpecies().getDex())&&coherent(p)&&!fits(team,p,PLANS.get(p.getSpecies().getDex())))return;
  for(var p:team){var plan=PLANS.get(p.getSpecies().getDex());if(plan==null||!legal(p.getForm(),plan)||!fits(team,p,plan))continue;
   double value=rank(p.getSpecies().getDex(),p.getForm());if(value>score){chosen=p;score=value;}}
  for(var p:team)p.getPersistentData().func_74757_a(TAG,p==chosen);
  if(chosen==null)return;final Pokemon selected=chosen;var plan=PLANS.get(chosen.getSpecies().getDex());
  if(chosen.getMoveset().size()==4&&plan.stream().allMatch(n->has(selected,n)))return;
  var replacement=new ArrayList<Attack>();for(String move:plan)replacement.add(new Attack(selected.getForm().getMoves().getAllMoves().stream().filter(a->a.getAttackName().equals(move)).findFirst().orElseThrow()));
  chosen.getMoveset().clear();chosen.getMoveset().addAll(replacement);
 }
}
