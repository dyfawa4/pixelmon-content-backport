package qa;
import java.util.*;
import local.pixelmon.backport.*;
import com.pixelmonmod.pixelmon.api.battles.BattleAIMode;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.*;
import com.pixelmonmod.pixelmon.api.pokemon.boss.BossTierRegistry;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.battles.api.rules.BattleRules;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.ai.*;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import com.pixelmonmod.pixelmon.entities.npcs.NPCTrainer;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
public final class Stall190Cases {
    static void check(boolean b,String text){if(!b)throw new AssertionError(text);}
    static MoveChoice choice(PixelmonWrapper own,PixelmonWrapper foe,String name){var c=NpcIntentResponse.raw(own.getBattleAI(),own,new Attack(name),foe);c.tier=2;c.weight=100;return c;}
    public static void run(ServerPlayerEntity player)throws Exception{
        var party=StorageProxy.getParty(player);var saved=party.getAll().clone();
        try{
            var wall=Update180Cases.mon(242,"Soft-Boiled","Toxic","Protect","Seismic Toss");wall.setHeldItem(Update180Cases.stack("pixelmon:leftovers"));
            var regen=Update180Cases.mon(748,"Recover","Haze","Toxic","Scald");regen.setAbility(new Regenerator());
            for(int i=0;i<6;i++)party.set(i,i==0?wall:i==1?regen:null);
            var npc=new NPCTrainer(player.field_70170_p);npc.isGymLeader=true;npc.getPersistentData().func_74778_a(GymCities.TYPE,"psychic");npc.setBossTier(BossTierRegistry.getBossTierOrNotBoss("equal"));
            var member=Update180Cases.mon(151,"Psychic","Ice Beam","Taunt","Recover");for(int i=0;i<6;i++)npc.getPokemonStorage().set(i,i==0?member:null);
            var human=new PlayerParticipant(player,wall);var trainer=new TrainerParticipant(npc,1);var b=new BattleController(new BattleParticipant[]{trainer},new BattleParticipant[]{human},new BattleRules());b.sendMessages=false;
            try{
                for(var side:b.participants){side.controlledPokemon.clear();side.controlledPokemon.add(side.allPokemon[0]);for(var w:side.allPokemon){w.bc=b;w.onBattlefield=w==side.allPokemon[0];w.setHealth(w.getMaxHealth());}}
                trainer.setBattleAI(BattleAIMode.ADVANCED.createAI(trainer));var own=trainer.allPokemon[0];var foe=human.allPokemon[0];b.modifyStats();
                NpcTeamKnowledge.capture(trainer);check(NpcTeamKnowledge.plan(b,NpcTeamKnowledge.Plan.STALL),"Detect recovery and Regenerator defensive core");
                NpcTeamKnowledge.configure(trainer);check(member.getMoveset().stream().anyMatch(a->a.getMove().getAttackName().equals("Taunt")),"Retain Taunt while assigning Knock Off");check(member.getMoveset().stream().anyMatch(a->a.getMove().getAttackName().equals("Knock Off")),"Assign legal Knock Off");check(member.getMoveset().stream().filter(a->a.getAttackCategory()!=com.pixelmonmod.pixelmon.api.battles.AttackCategory.STATUS).count()>=2,"Preserve two offensive slots");
                var knock=choice(own,foe,"Knock Off");NpcStallControl.weight(own,List.of(knock));check(knock.weight==120,"Remove recovery item for progress");
                foe.tempAbility=new StickyHold();knock=choice(own,foe,"Knock Off");NpcStallControl.weight(own,List.of(knock));check(knock.weight==100,"Do not reward unremovable item");foe.tempAbility=null;
                var taunt=choice(own,foe,"Taunt");NpcStallControl.weight(own,List.of(taunt));check(taunt.weight==124,"Pressure known recovery");
                for(var ability:List.of(new MagicBounce(),new Oblivious(),new AromaVeil())){foe.tempAbility=ability;check(!NpcStallControl.tauntable(own,foe),"Taunt immunity guard "+ability);taunt=choice(own,foe,"Taunt");NpcStallControl.weight(own,List.of(taunt));check(taunt.weight==100,"No boosted immune Taunt");}
                foe.tempAbility=null;foe.pokemon.setHeldItem(Update180Cases.stack("pixelmon:mental_herb"));check(!NpcStallControl.tauntable(own,foe),"Mental Herb guard");foe.pokemon.setHeldItem(ItemStack.field_190927_a);
                foe.tempAbility=new Unaware();var boost=choice(own,own,"Nasty Plot");NpcStallControl.weight(own,List.of(boost));check(boost.tier==1&&boost.weight==25,"Do not blindly boost into Unaware");own.tempAbility=new MoldBreaker();boost=choice(own,own,"Nasty Plot");NpcStallControl.weight(own,List.of(boost));check(boost.tier==2&&boost.weight==100,"Ability bypass keeps setup option");own.tempAbility=null;
                foe.tempAbility=new PoisonHeal();var toxic=choice(own,foe,"Toxic");NpcStallControl.weight(own,List.of(toxic));check(toxic.tier==1,"Do not enable opposing Poison Heal");foe.tempAbility=null;
                taunt=choice(own,foe,"Taunt");taunt.result.result=com.pixelmonmod.pixelmon.battles.controller.log.AttackResult.failed;NpcStallControl.weight(own,List.of(taunt));check(taunt.weight==100,"Native failed result never rewarded");
                // A late reset must not keep a fake tier-3 priority against a lethal committed attack.
                Update180R6Cases.action(foe,"Seismic Toss",own);own.setHealth(1);own.getBattleStats().modifyStat(-6,com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType.SPEED);foe.getBattleStats().modifyStat(6,com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType.SPEED,com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType.ATTACK);b.modifyStats();b.battleTurn=5;Update180R6Cases.bind(b);
                try{var haze=choice(own,foe,"Haze");haze.tier=3;System.out.println("STALL190_HAZE_DEBUG active="+NpcTurnKnowledge.active(b)+" pressure="+NpcMomentum.pressure(foe)+" first="+NpcIntentResponse.first(own,haze.attack,foe,foe.attack)+" risk="+NpcTactics.incoming(own,NpcTactics.threats(own,haze))+" hp="+own.getHealth()+" status="+own.getStatuses());NpcSetupControl.weight(own,List.of(haze));check(haze.tier<=1,"Lethal hit before Haze loses tactical priority tier="+haze.tier);}finally{Update180R6Cases.context.remove();}
                own.setHealth(own.getMaxHealth());own.getBattleStats().clearBattleStats(true);foe.getBattleStats().clearBattleStats(true);own.choiceLocked=null;own.getMoveset().clear();for(String move:List.of("Snatch","Haze","Psychic","Ice Beam"))own.getMoveset().add(new Attack(move));b.modifyStats();
                Update180R6Cases.action(foe,"Calm Mind",foe);Update180R6Cases.bind(b);
                try{var response=trainer.getMove(own);check(response.attack!=null&&response.attack.getMove().getAttackName().equals("Snatch"),"Committed legal Calm Mind still triggers Snatch");Update180R6Cases.action(own,"Snatch",own);own.useAttack(true);foe.useAttack(true);check(foe.getBattleStats().getStage(com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType.SPECIAL_ATTACK)==0&&own.getBattleStats().getStage(com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType.SPECIAL_ATTACK)>0,"Actual Snatch steals native Calm Mind");}finally{Update180R6Cases.context.remove();}
                Update180R6Cases.action(foe,"Seismic Toss",own);Update180R6Cases.bind(b);
                try{var response=trainer.getMove(own);check(response.attack==null||!response.attack.getMove().getAttackName().equals("Snatch"),"No blind Snatch into an attack");}finally{Update180R6Cases.context.remove();}
                BossBalance190Probe.log("STALL190_PASS recovery/Regenerator recognition, legal two-attack anti-stall roles; item progress/StickyHold; Taunt immunity/herb guards; Unaware/bypass; PoisonHeal; failed actions; slow lethal Haze guard");
            }finally{b.battleEnded=true;NpcTeamKnowledge.end(b);NpcTurnKnowledge.end(b);AuthorForesight.end(b);GymChallenge.end(b);Erosion.end(b);CalamityBattle.end(b);npc.func_70106_y();}
        }finally{for(int i=0;i<6;i++)party.set(i,saved[i]);}
    }
}
