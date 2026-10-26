package qa;
import java.util.*;
import java.nio.file.*;
import local.pixelmon.backport.*;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.pokemon.stats.*;
import com.pixelmonmod.pixelmon.battles.controller.*;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import net.minecraft.entity.player.ServerPlayerEntity;
public final class Update180R12StatusCases {
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 static Class<?> curse(){try{return Class.forName("local.pixelmon.backport.Calamity");}catch(Exception absent){try{return Class.forName("local.pixelmon.backport.Calamity");}catch(Exception e){throw new RuntimeException(e);}}}
 static void record(List<Object> rows,String key,PixelmonWrapper p){var s=p.getBattleStats();rows.add(List.of(key,p.getHealth(),p.getMaxHealth(),s.attackStat,s.defenseStat,s.specialAttackStat,s.specialDefenseStat,s.speedStat,Erosion.temporaryRemaining(p),LightningOath.remaining(p),LightningOath.multiplier(0),LightningOath.multiplier(2)));}
 public static void run(ServerPlayerEntity player)throws Exception{
  var party=StorageProxy.getParty(player);Pokemon[] saved=party.getAll().clone();List<Object> rows=new ArrayList<>();String tag=(String)curse().getField("TAG").get(null);var stats=List.of(BattleStatsType.HP,BattleStatsType.ATTACK,BattleStatsType.DEFENSE,BattleStatsType.SPECIAL_ATTACK,BattleStatsType.SPECIAL_DEFENSE,BattleStatsType.SPEED);
  try{for(int dex:new int[]{1,292,383,384,716,888,898,1024})for(int mask=0;mask<8;mask++)for(int health:new int[]{1,37,100}){
   var mon=Update180Cases.mon(dex,"Recover","Tackle");if(dex==383)mon.setForm("primal");if(dex==888)mon.setForm("crowned");if(dex==898)mon.setForm("shadowrider");for(var stat:stats){mon.getIVs().setStat(stat,31);mon.getEVs().setStat(stat,252);}mon.setMintNature(dex%2==0?Nature.TIMID:Nature.ADAMANT);mon.heal();for(int i=0;i<6;i++)party.set(i,i==0?mon:null);
   var npc=new com.pixelmonmod.pixelmon.entities.npcs.NPCTrainer(player.field_70170_p);npc.getPokemonStorage().set(0,Update180Cases.mon(143,"Tackle"));var human=new PlayerParticipant(player,mon);var foe=new TrainerParticipant(npc,1);var b=new BattleController(new BattleParticipant[]{human},new BattleParticipant[]{foe},new com.pixelmonmod.pixelmon.battles.api.rules.BattleRules());b.sendMessages=false;var init=BattleController.class.getDeclaredMethod("initBattle");init.setAccessible(true);init.invoke(b);var p=human.allPokemon[0];var enemy=foe.allPokemon[0];String key=dex+"/"+mask+"/"+health;
   try{p.setHealth(Math.max(1,p.getMaxHealth()*health/100));record(rows,key+" initial",p);
    if((mask&1)!=0){mon.getPersistentData().func_74768_a(tag,20);curse().getMethod("enter",PixelmonWrapper.class).invoke(null,p);}if((mask&2)!=0)Erosion.apply(p);b.modifyStats(p);record(rows,key+" effects",p);
    var copy=NpcSimulationCopy.of(p);record(rows,key+" copy",copy);
    b.battleTurn=2;if((mask&4)!=0)LightningOath.apply(enemy,p);p.onBattlefield=false;b.battleTurn=5;Erosion.expire(b);LightningOath.expire(b);b.modifyStats(p);record(rows,key+" bench5",p);
    p.setHealth(0);b.battleTurn=7;Erosion.expire(b);LightningOath.expire(b);record(rows,key+" faint7",p);check(p.isFainted(),"No effect expiry resurrection");
    Erosion.end(b);try{Class.forName("local.pixelmon.backport.CalamityBattle").getMethod("end",BattleController.class).invoke(null,b);}catch(ClassNotFoundException old){}record(rows,key+" end",p);
   }finally{Erosion.end(b);try{Class.forName("local.pixelmon.backport.CalamityBattle").getMethod("end",BattleController.class).invoke(null,b);}catch(ClassNotFoundException old){}LightningOath.end(b);b.battleEnded=true;npc.func_70106_y();}
  }}finally{for(int i=0;i<6;i++)party.set(i,saved[i]);}
  var gson=new com.google.gson.GsonBuilder().setPrettyPrinting().create();String result=gson.toJson(rows);Path baseline=Path.of("status-baseline-r11.json");if(Files.exists(Path.of("status-compare"))){var old=new com.google.gson.JsonParser().parse(Files.readString(baseline));var now=new com.google.gson.JsonParser().parse(result);check(old.equals(now),"Native status differential differs; inspect status-current.json");System.out.println("R12_STATUS_DIFFERENTIAL_PASS "+rows.size()+" native snapshots,8 species/forms x8 combinations x3 HP fractions; HP/max,5 combat stats, clocks, simulation copy, bench expiry, faint/end unchanged");}else{Files.writeString(baseline,result);System.out.println("R12_STATUS_BASELINE_PASS "+rows.size()+" native r11 snapshots captured");}Files.writeString(Path.of("status-current.json"),result);
 }
 public static void client(int age){if(age>35)net.minecraft.client.Minecraft.func_71410_x().func_71400_g();}
}

