package qa;
import java.util.*;
import com.pixelmonmod.pixelmon.api.battles.BattleAIMode;
import com.pixelmonmod.pixelmon.api.pokemon.boss.BossTierRegistry;
import com.pixelmonmod.pixelmon.battles.api.rules.BattleRules;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.ai.*;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import com.pixelmonmod.pixelmon.entities.npcs.NPCTrainer;
import local.pixelmon.backport.*;
import net.minecraft.entity.player.ServerPlayerEntity;

public final class Update180AdviceCases {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void run(ServerPlayerEntity player)throws Exception{
        var npc=new NPCTrainer(player.field_70170_p);npc.isGymLeader=true;npc.setBossTier(BossTierRegistry.getBossTierOrNotBoss("equal"));
        for(int i=0;i<6;i++)npc.getPokemonStorage().set(i,null);npc.getPokemonStorage().set(0,Update180Cases.mon(150,"Taunt","Knock Off","Rain Dance","Trick Room"));
        var trainer=new TrainerParticipant(npc,1);var enemy=new WildPixelmonParticipant(Update180Cases.mon(493,"Recover","Calm Mind","Earthquake"));
        var battle=new BattleController(new BattleParticipant[]{trainer},new BattleParticipant[]{enemy},new BattleRules());battle.sendMessages=false;battle.battleTurn=3;
        for(var side:battle.participants){side.controlledPokemon.clear();side.controlledPokemon.add(side.allPokemon[0]);for(var mon:side.allPokemon){mon.bc=battle;mon.onBattlefield=mon==side.allPokemon[0];mon.setHealth(mon.getMaxHealth());}}
        trainer.setBattleAI(BattleAIMode.ADVANCED.createAI(trainer));battle.modifyStats();var own=trainer.allPokemon[0];var foe=enemy.allPokemon[0];own.choiceLocked=null;
        own.getMoveset().clear();for(var m:List.of("Taunt","Knock Off","Rain Dance","Trick Room"))own.getMoveset().add(new Attack(m));
        for(boolean author:new boolean[]{false,true}){
            npc.getPersistentData().func_74757_a(AuthorBossTeams.MARKER,author);
            check(NpcGimmicks.enabled(npc)&&NpcTurnKnowledge.enabled(own),"Actual equal-level planning enabled, author="+author);
            foe.attack=new Attack("Recover");foe.lastAttack=foe.attack;
            foe.pokemon.setHeldItem(Update180Cases.stack("pixelmon:leftovers"));
            var choices=Update180MomentumCases.pool(own);var taunt=Update180MomentumCases.named(choices,"Knock Off");
            float score=NpcMatchupAdvice.score(own,taunt);check(score>0,"Live removable item gives Knock Off advice, author="+author+" tier="+taunt.tier+" weight="+taunt.weight+" result="+taunt.result.result);
            int tier=taunt.tier;float weight=taunt.weight;NpcMatchupAdvice.weight(own,List.of(taunt));check(taunt.tier==tier&&taunt.weight==weight+score&&score<=20,"Bounded score only, native tier retained");
            foe.pokemon.setHeldItem(net.minecraft.item.ItemStack.field_190927_a);check(NpcMatchupAdvice.score(own,taunt)==0,"Reference re-read after item changes, author="+author);
            taunt.result.result=com.pixelmonmod.pixelmon.battles.controller.log.AttackResult.failed;check(NpcMatchupAdvice.score(own,taunt)==0,"Failed move not revived");
            var actual=own.getBattleAI().getNextMove(own);check(actual!=null&&actual.attack!=null&&own.getMoveset().stream().anyMatch(a->a.getMove().equals(actual.attack.getMove())),"Transformed native AdvancedAI uses legal existing move, author="+author);
        }
        System.out.println("ADVICE_180_PASS ordinary equal-level leader and author actual AdvancedAI entry; native legal pool; held item re-evaluated; bounded weights preserve tiers and failed moves");
    }
}
