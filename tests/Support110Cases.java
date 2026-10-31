package qa;
import java.nio.file.*;
import java.util.*;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.api.battles.AttackCategory;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import net.minecraft.entity.player.ServerPlayerEntity;
import local.pixelmon.backport.*;
public final class Support110Cases {
 static int checks;
 static void check(boolean v,String why){checks++;if(!v)throw new AssertionError(why);}
 static Pokemon mon(int dex,String... moves){var p=PokemonFactory.create(PixelmonSpecies.fromNationalDex(dex));p.setLevelNum(70);p.initialize();p.getMoveset().clear();for(var m:moves)p.getMoveset().add(new Attack(m));return p;}
 static String names(Pokemon p){return p.getMoveset().stream().map(a->a.getMove().getAttackName()).collect(java.util.stream.Collectors.joining(","));}
 static boolean has(Pokemon p,String move){return p.getMoveset().stream().anyMatch(a->a.getMove().getAttackName().equals(move));}
 public static void run(ServerPlayerEntity player)throws Exception{
  var honch=mon(430,"Sucker Punch","Sky Attack","Haze","Snatch");Files.writeString(Path.of("support110-honch-native.txt"),honch.getForm().getMoves().getAllMoves().toString());var id=honch.getUUID();var original=names(honch);AuthorSupportPlan.configure(List.of(honch));AuthorRoster.train(honch);NpcHeldItems.equip(honch);
  check(AuthorSupportPlan.support(honch)&&honch.getUUID().equals(id)&&names(honch).equals(original),"Approved Honchkrow core retained");
  check(honch.getHeldItem().func_77973_b().getRegistryName().func_110623_a().equals("power_herb"),"Native item scoring chooses Power Herb for Sky Attack/Snatch plan");
  var lugia=mon(249,"Aeroblast","Psychic","Roost","Whirlwind");AuthorSupportPlan.configure(List.of(lugia));AuthorRoster.train(lugia);NpcHeldItems.equip(lugia);
  check(AuthorSupportPlan.support(lugia)&&has(lugia,"Roost")&&has(lugia,"Whirlwind")&&has(lugia,"Toxic"),"Legendary has coherent sustain/control plan");
  check(lugia.getMoveset().stream().filter(a->a.getAttackCategory()!=AttackCategory.STATUS).count()==1,"No forced second attack on dedicated wall");
  for(var p:List.of(honch,lugia)){
   for(var a:p.getMoveset())check(p.getForm().getMoves().getAllMoves().stream().anyMatch(v->v.getAttackName().equals(a.getMove().getAttackName())),"Actual native legal support move "+a);
   check(Arrays.stream(p.getEVs().getArray()).allMatch(v->v==252)&&Arrays.stream(p.getIVs().getArray()).allMatch(v->v==31),"Author numerical rules retained");
  }
  var soleTaunt=mon(430,"Sucker Punch","Sky Attack","Taunt","Roost");String before=soleTaunt.getMoveset().toString();AuthorSupportPlan.configure(List.of(soleTaunt));check(before.equals(soleTaunt.getMoveset().toString()),"Never delete team's sole Taunt to force a profile");
  var mixed=List.of(mon(249,"Aeroblast","Psychic","Roost","Whirlwind"),mon(430,"Sucker Punch","Sky Attack","Haze","Snatch"));AuthorSupportPlan.configure(mixed);check(mixed.stream().filter(AuthorSupportPlan::support).count()==1,"Only one dedicated support role");
  // Existing native roster audit: 224 teams, fixed species, three cores, two strong slots,
  // legal moves/stats and real battle preparation/scouting. Historical migrations are unchanged.
  AuthorRoster190Cases.run(player,false);
  Files.writeString(Path.of("support110-pass.txt"),"PASS targeted="+checks+"; native 224-team/scouting audit passed; unchanged historical migrations not rerun");BossBalance190Probe.log("SUPPORT110_PASS "+checks);
 }
}
