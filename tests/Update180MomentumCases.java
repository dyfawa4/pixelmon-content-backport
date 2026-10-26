package qa;
import java.util.*;
import com.pixelmonmod.pixelmon.api.battles.*;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.pokemon.boss.BossTierRegistry;
import com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType;
import com.pixelmonmod.pixelmon.battles.api.rules.BattleRules;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.ai.*;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import com.pixelmonmod.pixelmon.entities.npcs.NPCTrainer;
import local.pixelmon.backport.*;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;

/** Focused native move previews for the new, species-independent response. */
public final class Update180MomentumCases {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static List<MoveChoice> pool(PixelmonWrapper own)throws Exception{
        var method=TacticalAI.class.getDeclaredMethod("getWeightedAttackChoices",PixelmonWrapper.class);method.setAccessible(true);
        return (List<MoveChoice>)method.invoke(own.getBattleAI(),own);
    }
    static MoveChoice named(List<MoveChoice> pool,String name){return pool.stream().filter(c->c.attack!=null&&c.attack.getMove().getAttackName().equals(name)).findFirst().orElseThrow();}
    public static void run(ServerPlayerEntity player)throws Exception{
        var npc=new NPCTrainer(player.field_70170_p);npc.isGymLeader=true;npc.setBossTier(BossTierRegistry.getBossTierOrNotBoss("equal"));
        for(int i=0;i<6;i++)npc.getPokemonStorage().set(i,null);
        npc.getPokemonStorage().set(0,Update180Cases.mon(717,"Sucker Punch","Dark Pulse","Toxic","Roost"));
        var calyrex=Update180Cases.mon(898,"Astral Barrage","Nasty Plot","Psychic","Draining Kiss");calyrex.setForm("shadowrider");calyrex.heal();
        var trainer=new TrainerParticipant(npc,1);var enemy=new WildPixelmonParticipant(calyrex);
        var battle=new BattleController(new BattleParticipant[]{trainer},new BattleParticipant[]{enemy},new BattleRules());battle.sendMessages=false;battle.battleTurn=2;
        for(var side:battle.participants){side.controlledPokemon.clear();side.controlledPokemon.add(side.allPokemon[0]);for(var w:side.allPokemon){w.bc=battle;w.onBattlefield=w==side.allPokemon[0];w.pokemon.setHeldItem(ItemStack.field_190927_a);w.setHealth(w.getMaxHealth());}}
        trainer.setBattleAI(BattleAIMode.ADVANCED.createAI(trainer));battle.modifyStats();var own=trainer.allPokemon[0];var foe=enemy.allPokemon[0];
        own.choiceLocked=null;own.getMoveset().clear();for(var m:List.of("Sucker Punch","Dark Pulse","Toxic","Roost"))own.getMoveset().add(new Attack(m));
        foe.getBattleStats().modifyStat(3,BattleStatsType.SPECIAL_ATTACK);foe.setHealth(1);
        check(NpcMomentum.pressure(foe)>=3,"Actual special boost detected");
        var choices=pool(own);var sucker=named(choices,"Sucker Punch");
        var selected=NpcTactics.finisher(own,choices);
        check(selected!=null&&selected.attack.getMove().getAttackName().equals("Sucker Punch"),"Native faster priority cleanup after sash consumed: "+(selected==null?"none":selected.attack));
        check(NpcMomentum.safeEntry(own,own.getHealth()*.6,sucker,NpcTactics.threats(own)),"Can take one entry hit then priority KO; not rejected as generic2HKO");
        check(!NpcMomentum.safeEntry(own,own.getHealth(),sucker,NpcTactics.threats(own)),"Cannot switch into lethal entry damage");
        sucker.attack.pp=0;check(!NpcMomentum.safeEntry(own,1,sucker,List.of()),"No PP cannot counter");sucker.attack.pp=5;
        sucker.result.result=com.pixelmonmod.pixelmon.battles.controller.log.AttackResult.failed;check(!NpcMomentum.safeEntry(own,1,sucker,List.of()),"Blocked priority is not a counter");
        // Ghost typing must stop a tempting Extreme Speed, even on a one-HP foe.
        own.getMoveset().clear();own.getMoveset().add(new Attack("Extreme Speed"));own.getMoveset().add(new Attack("Dark Pulse"));
        var immune=NpcIntentResponse.raw(own.getBattleAI(),own,own.getMoveset().get(0),foe);
        check(immune.result==null||immune.result.damage==0||immune.result.result==com.pixelmonmod.pixelmon.battles.controller.log.AttackResult.failed,"No normal priority into Ghost");
        // The same policy recognises a different special sweeper, not the Calyrex dex.
        foe.getBattleStats().clearBattleStats(true);check(!NpcMomentum.active(own),"No boost retains previous policy");
        foe.getMoveset().clear();foe.getMoveset().add(new Attack("Psychic"));foe.getBattleStats().modifyStat(2,BattleStatsType.ATTACK);
        check(NpcMomentum.pressure(foe)==0,"Irrelevant physical boost ignored for a special-only attacker");
        foe.getBattleStats().modifyStat(2,BattleStatsType.SPECIAL_ATTACK);foe.setHealth(foe.getMaxHealth());
        own.getMoveset().clear();own.getMoveset().add(new Attack("Haze"));own.getMoveset().add(new Attack("Toxic"));own.getBattleStats().clearBattleStats(true);foe.getBattleStats().modifyStat(-6,BattleStatsType.SPEED);
        own.setHealth(own.getMaxHealth());choices=pool(own);var haze=named(choices,"Haze");float before=haze.weight;int stages=foe.getBattleStats().getStage(BattleStatsType.SPECIAL_ATTACK);
        NpcMomentum.weight(own,choices);
        check(haze.tier>=2&&haze.weight>before,"Fast Haze that buys survival prioritised");
        check(foe.getBattleStats().getStage(BattleStatsType.SPECIAL_ATTACK)==stages,"Counter forecast does not clear live player boosts");
        System.out.println("MOMENTUM_180_PASS native Calyrex priority cleanup; one-hit entry then first strike; lethal entry/PP/failed/ghost guards; actual relevant boosts; Haze projection leaves real boosts intact");
    }
}
