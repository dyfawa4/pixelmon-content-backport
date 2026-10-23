package qa;
import java.util.*;
import java.awt.Color;
import java.lang.reflect.*;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.client.ClientProxy;
import com.pixelmonmod.pixelmon.client.gui.battles.*;
import com.pixelmonmod.pixelmon.client.gui.battles.battleScreens.ChooseAttack;
import local.pixelmon.backport.*;
/** Small native transformed-button check for the two reported screenshots. */
public final class OathMoveColorCases {
 static PixelmonClientData mon(int dex){var p=PokemonFactory.create(PixelmonSpecies.fromNationalDex(dex));p.setLevelNum(100);p.initialize();return new PixelmonClientData(p);}
 static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
 public static void run()throws Exception{
  var previous=ClientProxy.battleManager;var snapshot=TeraClient.snapshot;var reply=TeraClient.battle;
  var oathField=LightningOathClient.class.getDeclaredField("state");oathField.setAccessible(true);var oldOath=oathField.get(null);
  try{
   var bm=new ClientBattleManager();ClientProxy.battleManager=bm;bm.battleControllerIndex=791;bm.battleEnded=false;bm.currentPokemon=0;TeraClient.snapshot=null;TeraClient.battle=null;
   var user=mon(CapSpecies.DEX);var target=mon(51);bm.fullOurPokemon=List.of(user);bm.displayedOurPokemon=new PixelmonClientData[]{user};bm.displayedEnemyPokemon=new PixelmonClientData[]{target};
   var button=new ChooseAttack.MoveButton(null,0);var attack=button.getClass().getDeclaredField("attack");attack.setAccessible(true);
   var color=Arrays.stream(button.getClass().getDeclaredMethods()).filter(m->m.getName().contains("pmbattleinfo$getEffectivenessColor")).findFirst().orElseThrow();color.setAccessible(true);
   var electric=new Attack("Thunderbolt");attack.set(button,electric);check(color.invoke(button).equals(new Color(85,85,85)),"Ground native immunity gray");
   attack.set(button,new Attack("Ten Million Volts"));check(color.invoke(button).equals(new Color(85,255,85)),"Signature first-hit 1.5 must be GREEN");
   var state=new LightningOathNetwork.State();state.battle=791;state.turns.put(target.pokemonUUID,5);LightningOathClient.accept(state);
   for(String move:List.of("Ten Million Volts","Thunderbolt","Discharge")){attack.set(button,new Attack(move));check(color.invoke(button).equals(new Color(85,255,85)),"Oath Ground GREEN "+move);}
   check(BattleInfoCompat.multiplier(electric,user,target,false)==1.5,"Damage preview retains actual 1.5");
   TeraClient.snapshot=new TeraNetwork.Snapshot(791);TeraClient.snapshot.burst.put(user.pokemonUUID,17);attack.set(button,electric);
   check(color.invoke(button).equals(new Color(85,255,85)),"Burst 1.875 GREEN");check(BattleInfoCompat.multiplier(electric,user,target,false)==1.875,"Burst true value unchanged");
   for(int dex:new int[]{181,130}){target=mon(dex);bm.displayedEnemyPokemon[0]=target;state.turns.put(target.pokemonUUID,5);LightningOathClient.accept(state);attack.set(button,electric);check(color.invoke(button).equals(dex==130?new Color(0,170,0):new Color(85,255,85)),"Resist/4x+Oath+Burst GREEN "+dex);}
   TeraClient.snapshot=null;target=mon(181);bm.displayedEnemyPokemon[0]=target;state.turns.clear();LightningOathClient.accept(state);attack.set(button,electric);check(color.invoke(button).equals(new Color(255,85,85)),"Native Ampharos resistance RED");
   attack.set(button,new Attack("Ten Million Volts"));check(color.invoke(button).equals(new Color(85,255,85)),"Signature versus Ampharos GREEN before Oath");
   target=mon(51);bm.displayedEnemyPokemon[0]=target;state.turns.put(target.pokemonUUID,1);LightningOathClient.accept(state);attack.set(button,electric);check(color.invoke(button).equals(new Color(85,255,85)),"Last Oath turn GREEN");
   bm.displayedEnemyPokemon[0]=mon(51);check(color.invoke(button).equals(new Color(85,85,85)),"Switched unmarked Ground not affected");bm.displayedEnemyPokemon[0]=target;check(color.invoke(button).equals(new Color(85,255,85)),"Marked returning Ground GREEN");
   state.turns.clear();LightningOathClient.accept(state);check(color.invoke(button).equals(new Color(85,85,85)),"Expiry restores immunity gray");
   attack.set(button,new Attack("Explosion"));check(color.invoke(button).equals(Color.WHITE),"Unrelated Normal move unchanged");
   state.turns.put(target.pokemonUUID,5);LightningOathClient.accept(state);check(BattleInfoCompat.multiplier(new Attack("Earthquake"),target,user,false)==2,"Opponent Ground attack unchanged");
  }finally{ClientProxy.battleManager=previous;TeraClient.snapshot=snapshot;TeraClient.battle=reply;oathField.set(null,oldOath);}
 }
}
