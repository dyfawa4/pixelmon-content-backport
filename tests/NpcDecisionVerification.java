import java.util.*;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.pokemon.boss.BossTierRegistry;
import com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType;
import com.pixelmonmod.pixelmon.battles.api.rules.BattleRules;
import com.pixelmonmod.pixelmon.battles.BattleRegistry;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.api.registries.*;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.ai.*;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import com.pixelmonmod.pixelmon.entities.npcs.NPCTrainer;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import local.pixelmon.backport.*;

/** Registered Pokemon and native AI decisions, including saved ace builds and item turns. */
public final class NpcDecisionVerification {
    static void require(boolean value,String label){if(!value)throw new AssertionError(label);}
    static List<String> moves(Pokemon p){List<String> names=new ArrayList<>();p.getMoveset().forEach(a->names.add(a.getMove().getAttackName()));return names;}
    static void legal(Pokemon p){
        require(p.getMoveset().size()==4,"Ace must have four moves: "+p.getSpecies().getName()+moves(p));
        require(new HashSet<>(moves(p)).size()==4,"Duplicate ace moves");
        for(Attack a:p.getMoveset())require(p.getForm().getMoves().canLearn(a.getMove()),"Illegal ace move "+a);
    }
    static PixelmonWrapper wrapper(ServerPlayerEntity player,int own,String move,int enemy,String enemyMove){
        BattleController b=FeedbackVerification.battle(player,FeedbackVerification.mon(own,move),FeedbackVerification.mon(enemy,enemyMove));
        for(var side:b.participants)for(var member:side.allPokemon)require(member.bc==b,"Every original and appended member must keep the native battle controller");
        return b.participants.get(0).allPokemon[0];
    }
    public static void run(ServerPlayerEntity player){
        int builds=0,specials=0;
        for(int dex:PixelmonSpecies.getLegendaries()){
            if(!NpcMoves.viable(PixelmonSpecies.fromNationalDex(dex)))continue;
            Pokemon p=TeraVerification.mon(dex);int level=p.getPokemonLevel();String iv=p.getIVs().toString(),ev=p.getEVs().toString();
            p.getPersistentData().func_74757_a(NpcMoves.ACE,true);NpcMoves.configure(p,new UUID(0,dex));legal(p);
            require(p.getPokemonLevel()==level && iv.equals(p.getIVs().toString()) && ev.equals(p.getEVs().toString()),"Ace stats inflated");
            List<String> original=moves(p);Pokemon saved=PokemonFactory.create(p.writeToNBT(new CompoundNBT()));NpcMoves.configure(saved,new UUID(0,999));
            require(original.equals(moves(saved)) && saved.getUUID().equals(p.getUUID()),"Ace moves rerolled on reload");++builds;
        }
        for(int level:new int[]{5,30,70,100}){
            Pokemon p=PokemonFactory.create(PixelmonSpecies.fromNationalDex(890));p.setLevelNum(level);p.initialize();
            p.getPersistentData().func_74757_a(NpcMoves.ACE,true);p.setHeldItem(new ItemStack(Backport.REGISTERED.get("eternatus_meteorite").get()));
            NpcMoves.configure(p,new UUID(0,890));legal(p);
            require(moves(p).equals(Arrays.asList("Dynamax Cannon","Sludge Bomb","Flamethrower","Recover")),"Special ace lacks coordinated moves");
            require(p.getForm().isForm("eternamax") && p.getForm().getBattleStats().getStat(BattleStatsType.HP)==255,"Special ace is cosmetic only");
            require(p.getPokemonLevel()==level,"Special ace level raised");
        }
        for(long seed=1;seed<=64;seed++){
            NPCTrainer npc=new NPCTrainer(player.field_70170_p);npc.isGymLeader=true;npc.setBossTier(BossTierRegistry.getBossTierOrNotBoss("equal"));npc.getPersistentData().func_74778_a(GymCities.TYPE,"poison");npc.func_184221_a(new UUID(0,seed));
            npc.getPokemonStorage().set(0,FeedbackVerification.mon(94,"Shadow Ball"));TrainerParticipant tp=new TrainerParticipant(npc,1);NpcGimmicks.augment(tp);
            require(tp.allPokemon.length==2,"Poison trainer missing ace");Pokemon ace=tp.allPokemon[1].pokemon;legal(ace);
            if(ace.getSpecies().getDex()==890){++specials;require(ace.getHeldItem().func_77973_b() instanceof ReferenceItems.Meteorite && ace.getForm().isForm("eternamax"),"Special form missing required item");}
        }
        require(specials>=30 && specials<63,"Ace diversity/probability lost: "+specials);
        NPCTrainer legacy=new NPCTrainer(player.field_70170_p);legacy.isGymLeader=true;legacy.setBossTier(BossTierRegistry.getBossTierOrNotBoss("equal"));legacy.getPersistentData().func_74778_a(GymCities.TYPE,"poison");legacy.getPersistentData().func_74757_a("BackportNpcRoster",true);
        legacy.getPokemonStorage().set(0,FeedbackVerification.mon(94,"Shadow Ball"));Pokemon ace=FeedbackVerification.mon(890,"Poison Tail");
        ace.setHeldItem(new ItemStack(Backport.REGISTERED.get("eternatus_meteorite").get()));ace.getPersistentData().func_74757_a(NpcGimmicks.PROFILE,true);
        legacy.getPokemonStorage().set(1,ace);UUID identity=ace.getUUID();TrainerParticipant tp=new TrainerParticipant(legacy,1);NpcGimmicks.augment(tp);NpcMoves.configure(ace,legacy.func_110124_au());
        legal(ace);require(ace.getUUID().equals(identity) && ace.getPokemonLevel()==70,"Saved ace replaced");
        Pokemon custom=FeedbackVerification.mon(890,"Toxic");NpcMoves.configure(custom,new UUID(0,1));require(moves(custom).equals(Arrays.asList("Toxic")),"Custom unmarked roster rewritten");

        PixelmonWrapper p=wrapper(player,3,"Energy Ball",143,"Tackle");PixelmonWrapper enemy=p.getOpponentPokemon().get(0);
        require(p.getBattleAI() instanceof AdvancedAI,"Enhanced NPC lacks native advanced AI");
        Tera.changeType(p.pokemon,Element.WATER);NpcGimmicks.Choice before=NpcGimmicks.decide(p,new Attack("Energy Ball"));
        enemy.pokemon.getMoveset().clear();enemy.pokemon.getMoveset().add(new Attack("Thunderbolt"));
        require(NpcGimmicks.decide(p,new Attack("Energy Ball"))==before,"NPC read unrevealed coverage move");
        require(NpcTactics.knownMoves(p,enemy).stream().noneMatch(a->a.getMove().getAttackName().equals("Thunderbolt")),"Hidden move leaked to native memory");
        enemy.attack=new Attack("Thunderbolt");p.getBattleAI().registerMove(enemy);
        require(NpcTactics.knownMoves(p,enemy).stream().anyMatch(a->a.getMove().getAttackName().equals("Thunderbolt")),"NPC ignored revealed coverage");

        p=wrapper(player,6,"Flamethrower",1,"Tackle");enemy=p.getOpponentPokemon().get(0);p.setHealth(20);enemy.setHealth(3);
        MoveChoice finisher=p.getParticipant().getMove(p);require(finisher.isOffensiveMove() && finisher.tier>=3,"Native AI missed finishing attack");
        require(!NpcMedicine.plan(p,finisher),"Wasted heal instead of fast KO");
        p=wrapper(player,143,"Body Slam",68,"Close Combat");p.setHealth(20);
        PixelmonWrapper lethalEnemy=p.getOpponentPokemon().get(0);lethalEnemy.getBattleStats().modifyStat(6,BattleStatsType.ATTACK);
        lethalEnemy.attack=new Attack("Close Combat");p.getBattleAI().registerMove(lethalEnemy);
        require(!NpcMedicine.plan(p),"Wasted potion into certain KO after healing");
        p=wrapper(player,890,"Recover",143,"Tackle");p.setHealth(20);
        MoveChoice recover=new MoveChoice(p,new Attack("Recover"),Collections.singletonList(p));
        require(!NpcMedicine.plan(p,recover),"Overrode planned recovery with potion");
        p=wrapper(player,143,"Body Slam",6,"Tackle");p.setHealth(20);p.bc.simulateMode=true;
        require(!NpcMedicine.plan(p),"AI simulation spent medicine");p.bc.simulateMode=false;
        p=wrapper(player,3,"Recover",6,"Flamethrower");Tera.changeType(p.pokemon,Element.WATER);
        require(NpcGimmicks.decide(p,new Attack("Recover"))==NpcGimmicks.Choice.TERA,"Defensive Tera blocked while recovering");
        Pokemon trained=FeedbackVerification.mon(890,"Poison Tail");trained.getPersistentData().func_74757_a(NpcMoves.ACE,true);
        trained.setHeldItem(new ItemStack(Backport.REGISTERED.get("eternatus_meteorite").get()));NpcMoves.configure(trained,new UUID(0,890));
        BattleController trainedBattle=FeedbackVerification.battle(player,trained,FeedbackVerification.mon(36,"Moonblast"));
        p=trainedBattle.participants.get(0).allPokemon[0];MoveChoice coverage=p.getParticipant().getMove(p);
        require(coverage.isOffensiveMove() && "Sludge Bomb".equals(coverage.attack.getMove().getAttackName()),"Native ace AI failed fairy coverage: "+coverage);
        require(p.getMaxHealth()>FeedbackVerification.mon(890,"Tackle").getMaxHealth(),"Native ace wrapper lost special form stats");
        trainedBattle=FeedbackVerification.battle(player,trained,FeedbackVerification.mon(143,"Tackle"));p=trainedBattle.participants.get(0).allPokemon[0];p.setHealth(20);
        // Make this a demonstrably safe recovery turn rather than relying on random
        // native opponent-memory guesses and IVs for an unobserved Snorlax. This
        // case tests recovery without a viable reserve; switching is tested below.
        for(PixelmonWrapper reserve:p.getParticipant().allPokemon)if(reserve!=p)reserve.setHealth(0);
        PixelmonWrapper recoveryEnemy=p.getOpponentPokemon().get(0);
        recoveryEnemy.getBattleStats().modifyStat(-6,BattleStatsType.ATTACK,BattleStatsType.SPECIAL_ATTACK,BattleStatsType.SPEED);
        recoveryEnemy.attack=new Attack("Tackle");p.getBattleAI().registerMove(recoveryEnemy);
        MoveChoice nativeRecovery=p.getParticipant().getMove(p);
        require(p.onBattlefield && p.getParticipant().controlledPokemon.contains(p),"Native switch preview removed the active member");
        require(NpcMoves.recovery(nativeRecovery.attack),"Native ace AI failed recovery: "+nativeRecovery+"; health="+p.getHealth()+"/"+p.getMaxHealth()+"; field="+p.onBattlefield+"; lock="+p.choiceLocked+"; risk="+NpcTactics.incoming(p,NpcTactics.threats(p))+"; moves="+p.getMoveset());
        require(!NpcMedicine.plan(p,nativeRecovery),"Native recovery overridden by medicine");
        contextualTurns(player);counterMatchups(player);
        System.out.println("BACKPORT_NPC_REVIEW_PASS legal saved ace builds="+builds+"; native AI memory, finishing attacks, recovery/item timing, defensive transformation, unchanged levels/IV/EV");
    }
    static void contextualTurns(ServerPlayerEntity player){
        PixelmonWrapper own=wrapper(player,6,"Flamethrower",9,"Surf");
        own.pokemon.getMoveset().add(new Attack("Dragon Dance"));
        PixelmonWrapper enemy=own.getOpponentPokemon().get(0);enemy.attack=new Attack("Surf");own.getBattleAI().registerMove(enemy);own.setHealth(15);
        MoveChoice decision=own.getParticipant().getMove(own);
        require(decision.attack==null || !decision.attack.getMove().getAttackName().equals("Dragon Dance"),"Boost chosen into observed imminent KO");
        own=wrapper(player,890,"Recover",143,"Tackle");own.pokemon.getMoveset().add(new Attack("Sludge Bomb"));
        decision=own.getParticipant().getMove(own);require(!NpcMoves.recovery(decision.attack),"Recovery wasted at full health");
        own=wrapper(player,149,"Extreme Speed",1,"Tackle");own.pokemon.getMoveset().add(new Attack("Dragon Dance"));
        enemy=own.getOpponentPokemon().get(0);enemy.setHealth(1);
        decision=own.getParticipant().getMove(own);require(decision.isOffensiveMove() && decision.tier>=3,"Safe priority KO lost to setup");
        System.out.println("BACKPORT_NPC_CONTEXT_PASS observed dangerous setup, full-health recovery, priority finishing turn");
    }
    static void counterMatchups(ServerPlayerEntity player){
        String[] themes={"normal","fire","water","electric","grass","ice","fighting","poison","ground","flying","psychic","bug","rock","ghost","dragon","dark","steel","fairy"};
        int[] opponents={68,9,3,445,6,6,36,65,9,248,197,6,9,197,36,68,6,89};
        String[] attacks={"Close Combat","Surf","Energy Ball","Earthquake","Flamethrower","Flamethrower","Moonblast","Psychic","Surf","Stone Edge","Dark Pulse","Flamethrower","Surf","Dark Pulse","Moonblast","Close Combat","Flamethrower","Sludge Bomb"};
        try{for(int i=0;i<themes.length;i++){
            String theme=themes[i],ns=GymCities.DATA.getAsJsonObject("newThemes").has(theme)?"pixelmon_backport":"pixelmon";
            NPCTrainer npc=GymCityVerification.trainer(player.func_71121_q(),ns+"/structures/gyms/"+theme+"/gym_"+theme+"_leader_a.nbt");GymCities.initialize(npc);
            TrainerParticipant ours=new TrainerParticipant(npc,1);Pokemon counter=FeedbackVerification.mon(opponents[i],attacks[i]);
            var party=StorageProxy.getParty(player);Pokemon previous=party.get(0);party.set(0,counter);
            npc.func_70012_b(2,65,3,0,0);player.field_70170_p.func_217376_c(npc);counter.getOrSpawnPixelmon(player);
            BattleController battle=BattleRegistry.startBattle(new BattleParticipant[]{new PlayerParticipant(player,counter)},new BattleParticipant[]{ours},new BattleRules());
            require(battle!=null,"Counter test native battle did not start "+theme);battle.sendMessages=false;
            try{
            battle.update();
            PixelmonWrapper lead=ours.allPokemon[0],enemy=lead.getOpponentPokemon().get(0);enemy.attack=new Attack(attacks[i]);lead.getBattleAI().registerMove(enemy);
            final Element offensiveType=enemy.attack.getType();
            require(Arrays.stream(ours.allPokemon).anyMatch(w->Element.getTotalEffectiveness(Collections.singletonList(Tera.type(w.pokemon)),offensiveType)<=.5),"Whole team lacks counter-type defensive option "+theme);
            Map<PixelmonWrapper,boolean[]> beforePreview=new IdentityHashMap<>();
            for(PixelmonWrapper member:ours.allPokemon)beforePreview.put(member,new boolean[]{member.onBattlefield,member.switchedThisTurn,member.takenTurn,member.hasBeenInBattle});
            MoveChoice response=ours.getMove(lead);require(response!=null,"No native counter matchup decision "+theme);
            for(var entry:beforePreview.entrySet()){
                PixelmonWrapper member=entry.getKey();boolean[] state=entry.getValue();
                require(member.onBattlefield==state[0] && member.switchedThisTurn==state[1] && member.takenTurn==state[2] && member.hasBeenInBattle==state[3],"Native AI preview changed actual turn state "+theme);
            }
            require(lead.getBattleAI() instanceof AdvancedAI,"Counter matchup lost advanced AI "+theme);
            for(PixelmonWrapper member:ours.allPokemon){
                require(member.pokemon.getPersistentData().func_74767_n(NpcGimmicks.PROFILE),"Counter profile not saved "+theme);
                if(member.pokemon.getPersistentData().func_74767_n(NpcMoves.ACE))legal(member.pokemon);
            }
            }finally{if(!battle.battleEnded)battle.endBattleWithoutXP();npc.func_70106_y();party.set(0,previous);}
        }}catch(Exception failure){throw new AssertionError("Gym counter matchup review",failure);}
        System.out.println("BACKPORT_NPC_COUNTER_PASS themes=18 native advanced attack/switch decisions and persisted defensive options against counter-types");
    }
}
