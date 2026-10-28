package qa;
import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import local.pixelmon.backport.*;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.api.registries.PixelmonItems;
import com.pixelmonmod.pixelmon.battles.controller.*;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
public final class LightBall190Cases {
 static int assertions,mismatches;
 static void check(boolean yes,String why){assertions++;if(!yes)throw new AssertionError(why);}
 static int[] stats(PixelmonWrapper p){var s=p.getBattleStats();return new int[]{s.attackStat,s.defenseStat,s.specialAttackStat,s.specialDefenseStat,s.speedStat};}
 public static void run(ServerPlayerEntity player)throws Exception{
  boolean baseline=Files.exists(Path.of("lightball-baseline"));var rows=new JsonArray();var party=StorageProxy.getParty(player);int[] dex={25,CapSpecies.DEX,26,172,132};for(int i=0;i<6;i++)party.set(i,i<dex.length?BossBalance190Probe.mon(dex[i],100,"Tackle"):null);
  var npc=new com.pixelmonmod.pixelmon.entities.npcs.NPCTrainer(player.field_70170_p);npc.getPersistentData().func_74757_a("BackportNpcDisable",true);npc.getPokemonStorage().set(0,BossBalance190Probe.mon(143,100,"Tackle"));var own=new PlayerParticipant(player,party.get(0));var foe=new TrainerParticipant(npc,1);var b=new BattleController(new BattleParticipant[]{own},new BattleParticipant[]{foe},new com.pixelmonmod.pixelmon.battles.api.rules.BattleRules());b.sendMessages=false;var init=b.getClass().getDeclaredMethod("initBattle");init.setAccessible(true);init.invoke(b);
  try{for(var p:own.allPokemon){if(p==null)continue;p.pokemon.setHeldItem(ItemStack.field_190927_a);b.modifyStats(p);int[] empty=stats(p);p.pokemon.setHeldItem(new ItemStack(PixelmonItems.light_ball));b.modifyStats(p);int[] equipped=stats(p);boolean affected=p.getSpecies().getDex()==25||p.getSpecies().getDex()==CapSpecies.DEX;boolean okay=true;
    for(int i=0;i<5;i++)okay&=equipped[i]==empty[i]*((i==0||i==2)&&affected?2:1);
    if(!okay){mismatches++;if(!baseline)throw new AssertionError("Real native stats "+p.getSpecies().getDex()+Arrays.toString(empty)+" -> "+Arrays.toString(equipped));}
    var row=new JsonObject();row.addProperty("dex",p.getSpecies().getDex());row.add("unheld",new Gson().toJsonTree(empty));row.add("lightBall",new Gson().toJsonTree(equipped));row.addProperty("expectedDouble",affected);rows.add(row);
    p.setTempAbility(new com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.Klutz());check(p.getUsableHeldItem()!=PixelmonItems.light_ball,"Native Klutz item suppression");b.modifyStats(p);check(Arrays.equals(stats(p),empty),"Klutz prevents all LightBall boosts");p.tempAbility=null;
    p.pokemon.setHeldItem(ItemStack.field_190927_a);b.modifyStats(p);check(Arrays.equals(stats(p),empty),"Removing item restores native values");
  }}finally{b.battleEnded=true;npc.func_70106_y();}
  String tooltip=((com.pixelmonmod.pixelmon.items.PixelmonItem)PixelmonItems.light_ball).getTooltipText();if(!baseline)check(tooltip.contains("该道具同样可以在戴帽子的皮卡丘身上生效")&&tooltip.indexOf("该道具同样")==tooltip.lastIndexOf("该道具同样")&&tooltip.indexOf('\n')>0,"Appended exact Chinese description once; original retained");
  var result=new JsonObject();result.addProperty("pass",true);result.addProperty("assertions",assertions);result.addProperty("mismatches",mismatches);result.addProperty("baseline",baseline);result.addProperty("tooltip",tooltip);result.add("nativeStats",rows);Files.writeString(Path.of("lightball190-results.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result));BossBalance190Probe.log("LIGHTBALL190_PASS "+result);
 }
}
