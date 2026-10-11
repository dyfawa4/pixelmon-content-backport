import java.util.*;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.storage.*;
import com.pixelmonmod.pixelmon.api.registries.*;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.api.rules.BattleRules;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.ai.MoveChoice;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import com.pixelmonmod.pixelmon.entities.npcs.NPCTrainer;
import com.pixelmonmod.pixelmon.entities.pixelmon.interactions.InteractionTechnicalMove;
import com.pixelmonmod.pixelmon.items.TechnicalMoveItem;
import com.pixelmonmod.pixelmon.tools.Quadstate;
import com.pixelmonmod.pixelmon.api.pokemon.boss.BossTierRegistry;
import com.pixelmonmod.pixelmon.battles.BattleRegistry;
import com.pixelmonmod.pixelmon.enums.EnumMegaItemsUnlocked;
import local.pixelmon.backport.*;
import net.minecraft.item.ItemStack;
import com.pixelmonmod.pixelmon.enums.TMType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Hand;
import net.minecraft.entity.player.ServerPlayerEntity;

/** Real registered items, native learning queue and native trainer/battle wrappers. */
public final class FeedbackVerification {
    static void require(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static Pokemon mon(int dex,String move){Pokemon p=TeraVerification.mon(dex);p.getMoveset().clear();p.getMoveset().add(new Attack(move));return p;}
    public static void run(ServerPlayerEntity player){
        for(var entry:Backport.REGISTERED.values())require(ItemHelp.details(new ItemStack(entry.get())).size()>=2,"Missing purpose/source "+entry.getId());
        PlayerPartyStorage party=StorageProxy.getParty(player);Pokemon previous=party.get(0);ItemStack hand=player.func_184614_ca();
        try{
            Pokemon p=mon(6,"Scratch");party.set(0,p);
            var entity=p.getOrCreatePixelmon(player.field_70170_p,0,65,0);
            ItemStack disc=new ItemStack(Backport.REGISTERED.get("tera_blast_disc_single_use").get(),3);player.func_184611_a(Hand.MAIN_HAND,disc);
            require(TechnicalMoveItem.getMove(disc)==DisposableTeraDisc.move(),"Fixed move missing without NBT");
            disc.func_196082_o().func_74777_a("tm",(short)1);require(TechnicalMoveItem.getMove(disc)==DisposableTeraDisc.move(),"Tampered disc changed move");
            new InteractionTechnicalMove().processInteract(entity,player,Hand.MAIN_HAND,disc);
            require(p.getMoveset().hasAttack(DisposableTeraDisc.move().getAttack()) && disc.func_190916_E()==2,"Single-use native interaction learning/payment");
            new InteractionTechnicalMove().processInteract(entity,player,Hand.MAIN_HAND,disc);require(disc.func_190916_E()==2,"Already-known disc consumed");
            p.getMoveset().clear();for(String name:new String[]{"Scratch","Growl","Ember","Leer"})p.getMoveset().add(new Attack(name));
            new InteractionTechnicalMove().processInteract(entity,player,Hand.MAIN_HAND,disc);require(disc.func_190916_E()==2,"Replacement prompt prepaid");
            LearnMoveController.clearLearnMoves(player);require(disc.func_190916_E()==2,"Cancelled replacement consumed");
            new InteractionTechnicalMove().processInteract(entity,player,Hand.MAIN_HAND,disc);
            require(LearnMoveController.canLearnMove(player,p.getUUID(),DisposableTeraDisc.move().getAttack()) && disc.func_190916_E()==1,"Replacement confirmation did not pay once");
            new InteractionTechnicalMove().processInteract(entity,player,Hand.MAIN_HAND,disc);player.func_184611_a(Hand.MAIN_HAND,ItemStack.field_190927_a);
            try(PokeInventory inventory=new PokeInventory(player)){for(ItemStack stack:inventory.stacks)if(stack.func_77973_b() instanceof DisposableTeraDisc)stack.func_190920_e(0);}
            require(!LearnMoveController.canLearnMove(player,p.getUUID(),DisposableTeraDisc.move().getAttack()),"Missing disc allowed queued teaching");
            p.getMoveset().clear();ItemStack permanent=TechnicalMoveItem.of(TMType.TM9,DisposableTeraDisc.move());
            player.func_184611_a(Hand.MAIN_HAND,permanent);new InteractionTechnicalMove().processInteract(entity,player,Hand.MAIN_HAND,permanent);
            require(p.getMoveset().hasAttack(DisposableTeraDisc.move().getAttack()) && permanent.func_190916_E()==1,"Native reusable TM altered");
            p.setEggCycles(10);require(!DisposableTeraDisc.compatible(p),"Egg accepts disc");p.setEggCycles(-1);
            for(int stars=1;stars<=6;stars++)require(TeraRaids.rewards(stars,0,new Random(1)).stream().anyMatch(s->s.func_77973_b() instanceof DisposableTeraDisc),"Guaranteed new disc R"+stars);
            npc(player);nativeTrainer(player);NpcDecisionVerification.run(player);
            System.out.println("BACKPORT_FEEDBACK_PASS catalogue, one-use/permanent TM, cancellation/payment, R1-R6 rewards, NPC four mechanics/medicine/persistence");
        }finally{LearnMoveController.clearLearnMoves(player);party.set(0,previous);player.func_184611_a(Hand.MAIN_HAND,hand);}
    }
    static void nativeTrainer(ServerPlayerEntity player){
        NPCTrainer npc=new NPCTrainer(player.field_70170_p);npc.isGymLeader=true;npc.setBossTier(BossTierRegistry.getBossTierOrNotBoss("equal"));
        npc.setBossTier(BossTierRegistry.getBossTierOrNotBoss("equal"));
        npc.getPersistentData().func_74757_a("BackportNpcRoster",true);
        Pokemon original=mon(3,"Energy Ball");int level=original.getPokemonLevel();npc.getPokemonStorage().set(0,original);
        npc.func_70012_b(2,65,2,0,0);player.field_70170_p.func_217376_c(npc);
        Pokemon enemy=mon(6,"Flamethrower");StorageProxy.getParty(player).set(0,enemy);enemy.getOrSpawnPixelmon(player);
        BattleController b=null;
        try{
            TrainerParticipant participant=new TrainerParticipant(npc,1);
            b=BattleRegistry.startBattle(new BattleParticipant[]{new PlayerParticipant(player,enemy)},new BattleParticipant[]{participant},new BattleRules());
            require(b!=null,"Native trainer battle rejected");b.update();
            require(npc.getMegaItem()==EnumMegaItemsUnlocked.Both,"Native trainer start mixin not prepared");
            require(original.getPersistentData().func_74767_n(NpcGimmicks.PROFILE),"Native start profile not saved");
            PixelmonWrapper wrapper=participant.allPokemon[0];MoveChoice chosen=participant.getMove(wrapper);
            require(chosen!=null,"Native advanced AI returned no choice");wrapper.chooseMove(chosen);
            require(Tera.selected(wrapper),"Native AI getMove/chooseMove hooks missed defensive Tera");
            require(wrapper.megaEvolve() && Tera.active(wrapper),"Native NPC Tera execution failed");
            require(original.getPokemonLevel()==level,"Permanent NPC level changed");
        }finally{if(b!=null && !b.battleEnded)b.endBattleWithoutXP();npc.func_70106_y();}
    }
    static BattleController battle(ServerPlayerEntity player,Pokemon own,Pokemon enemy){
        NPCTrainer npc=new NPCTrainer(player.field_70170_p);npc.isGymLeader=true;npc.setBossTier(BossTierRegistry.getBossTierOrNotBoss("equal"));
        npc.getPersistentData().func_74757_a("BackportNpcRoster",true);
        // NPC construction supplies a random party. This fixture models a single
        // specified Pokemon; unrelated reserves make its recovery decisions random.
        for(int slot=0;slot<6;slot++)npc.getPokemonStorage().set(slot,null);
        own.getPersistentData().func_74757_a(NpcGimmicks.PROFILE,true);npc.getPokemonStorage().set(0,own);
        TrainerParticipant ours=new TrainerParticipant(npc,1);
        BattleController b=new BattleController(new BattleParticipant[]{ours},new BattleParticipant[]{new WildPixelmonParticipant(enemy)},new BattleRules());b.sendMessages=false;NpcGimmicks.prepare(ours);
        ours.setBattleAI(npc.getBattleAIMode().createAI(ours));b.modifyStats();return b;
    }
    static void select(PixelmonWrapper p,Attack attack,NpcGimmicks.Choice expected){

        require(NpcGimmicks.decide(p,attack)==expected,"NPC decision expected "+expected+" got "+NpcGimmicks.decide(p,attack));
        NpcGimmicks.plan(p,new MoveChoice(p,attack,p.getOpponentPokemon()));p.setAttack(attack,p.getOpponentPokemon(),true);
    }
    static void npc(ServerPlayerEntity player){
        UUID seed=new UUID(0,33);Pokemon grass=mon(3,"Energy Ball"),fire=mon(6,"Flamethrower");
        NpcGimmicks.profile(grass,seed,Arrays.asList(fire));Element type=Tera.type(grass);
        require(Element.getTotalEffectiveness(Collections.singletonList(type),Element.FIRE)<Element.getTotalEffectiveness(grass.getForm().getTypes(),Element.FIRE),"No defensive grass vs fire profile");
        Pokemon copy=PokemonFactory.create(grass.writeToNBT(new CompoundNBT()));String held=copy.getHeldItem().toString();
        NpcGimmicks.profile(copy,new UUID(0,999),Arrays.asList(mon(9,"Surf")));
        require(Tera.type(copy)==type && copy.getHeldItem().toString().equals(held) && copy.getUUID().equals(grass.getUUID()),"Profile rerolled after NBT reload");
        Pokemon own=mon(3,"Energy Ball");Tera.changeType(own,Element.WATER);
        BattleController b=battle(player,own,fire);PixelmonWrapper p=b.participants.get(0).allPokemon[0];
        select(p,new Attack("Energy Ball"),NpcGimmicks.Choice.TERA);require(Tera.selected(p) && p.megaEvolve() && Tera.active(p),"NPC Tera didn't execute");
        require(NpcGimmicks.decide(p,new Attack("Energy Ball"))==NpcGimmicks.Choice.NONE,"NPC spent another gimmick");Tera.end(b);
        own=mon(6,"Flamethrower");own.setHeldItem(new ItemStack(PixelmonItems.charizardite_x));Tera.changeType(own,Element.WATER);
        b=battle(player,own,mon(143,"Body Slam"));b.oldGen=Quadstate.YES;p=b.participants.get(0).allPokemon[0];
        select(p,new Attack("Flamethrower"),NpcGimmicks.Choice.MEGA);require(p.megaEvolve() && p.isMega,"NPC Mega didn't execute");
        own=mon(143,"Body Slam");Tera.changeType(own,Element.NORMAL);b=battle(player,own,mon(68,"Close Combat"));b.oldGen=Quadstate.NO;p=b.participants.get(0).allPokemon[0];
        select(p,new Attack("Body Slam"),NpcGimmicks.Choice.DYNAMAX);require(p.megaEvolve() && p.isDynamax>0,"NPC Dynamax didn't execute");
        own=mon(6,"Flamethrower");own.setHeldItem(new ItemStack(PixelmonItems.firium_z));Tera.changeType(own,Element.WATER);
        b=battle(player,own,mon(3,"Tackle"));b.oldGen=Quadstate.YES;p=b.participants.get(0).allPokemon[0];
        select(p,new Attack("Flamethrower"),NpcGimmicks.Choice.Z);require(p.usingZ && !p.willEvolve,"NPC Z not selected");p.registerZMove();require(p.getParticipant().usedZ,"Z budget not marked");
        require(NpcGimmicks.decide(p,new Attack("Growl"))==NpcGimmicks.Choice.NONE,"Status forced mechanic");
        // Medicines use native wrapper healing, consume a whole item turn, and exhaust the finite bag.
        b=battle(player,mon(143,"Body Slam"),mon(6,"Tackle"));p=b.participants.get(0).allPokemon[0];
        for(int i=0;i<3;i++){b.battleTurn=i;p.setHealth(20);boolean planned=NpcMedicine.plan(p);require(planned==(i<2),"Finite potion budget "+i);if(planned){p.useItem();require(p.getHealth()>20 && p.attack==null,"Potion heal/action failed");}}
        TrainerParticipant trainer=(TrainerParticipant)p.getParticipant();Pokemon reserve=mon(9,"Surf");trainer.trainer.getPokemonStorage().set(1,reserve);
        trainer.allPokemon=Arrays.copyOf(trainer.allPokemon,2);trainer.allPokemon[1]=new PixelmonWrapper(trainer,reserve,1);trainer.allPokemon[1].bc=b;trainer.allPokemon[1].setHealth(0);
        b.battleTurn=4;p.setHealth(p.getMaxHealth());require(NpcMedicine.plan(p),"No revive decision");p.useItem();require(trainer.allPokemon[1].isAlive(),"Native revive failed");
        trainer.allPokemon[1].setHealth(0);b.battleTurn=5;require(!NpcMedicine.plan(p),"Infinite revives");
        NPCTrainer npc=new NPCTrainer(player.field_70170_p);npc.isGymLeader=true;npc.setBossTier(BossTierRegistry.getBossTierOrNotBoss("equal"));npc.getPokemonStorage().set(0,mon(3,"Energy Ball"));
        // Choose a deterministic eligible ace roll, then persist through the actual NPC NBT methods.
        for(long value=1;value<30;value++){npc.func_184221_a(new UUID(0,value));if(new Random(value).nextInt(3)!=0)break;}
        TrainerParticipant tp=new TrainerParticipant(npc,1);NpcGimmicks.augment(tp);
        require(tp.allPokemon.length==2 && PixelmonSpecies.isLegendary(tp.allPokemon[1].getSpecies()),"No themed legendary ace");
        UUID ace=tp.allPokemon[1].getPokemonUUID();CompoundNBT saved=new CompoundNBT();npc.func_213281_b(saved);
        NPCTrainer restored=new NPCTrainer(player.field_70170_p);restored.func_70037_a(saved);TrainerParticipant replay=new TrainerParticipant(restored,1);NpcGimmicks.augment(replay);
        require(replay.allPokemon.length==2 && replay.allPokemon[1].getPokemonUUID().equals(ace),"NPC roster rerolled on reload");
    }
}


