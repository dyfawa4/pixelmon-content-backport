package qa;
import java.util.*;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.battles.attack.AttackRegistry;
import com.pixelmonmod.pixelmon.api.events.npc.TeachMoveEvent;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.entities.npcs.NPCRelearner;
import com.pixelmonmod.pixelmon.enums.TMType;
import com.pixelmonmod.pixelmon.items.TechnicalMoveItem;
import local.pixelmon.backport.*;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.nbt.*;
public final class Update180R8Cases {
    static void check(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
    public static void run(ServerPlayerEntity player)throws Exception {
        var move=AttackRegistry.getAttackBase("Tera Blast").orElseThrow();var tm=DisposableTeraDisc.move();int speciesCount=0,forms=0,compatible=0;
        for(var species:PixelmonSpecies.getAll()){
            speciesCount++;for(var form:species.getForms()){
                var moves=form.getMoves();if(moves==null)continue;forms++;
                check(moves.getAllLevelUpMoves().stream().noneMatch(TeraMachineLearning::isTera),"Level-up Tera Blast: "+species.getName()+"/"+form.getName());
                check(moves.getTutorMoves().stream().noneMatch(TeraMachineLearning::isTera),"Tutor Tera Blast");check(moves.getEggMoves().stream().noneMatch(TeraMachineLearning::isTera),"Egg Tera Blast");check(moves.getTransferMoves().stream().noneMatch(TeraMachineLearning::isTera),"Transfer Tera Blast");
                if(moves.canLearn(tm))compatible++;
            }
        }
        check(speciesCount>=1026&&compatible>500,"All species and machine coverage loaded");
        var party=StorageProxy.getParty(player);var saved=party.getAll().clone();var inventory=player.field_71071_by.func_70442_a(new ListNBT());
        try{
            player.field_71071_by.func_174888_l();player.func_71033_a(net.minecraft.world.GameType.SURVIVAL);var mon=Update180Cases.mon(6,"Tackle");party.set(0,mon);
            var tackle=AttackRegistry.getAttackBase("Tackle").orElseThrow();mon.getMoveset().getReminderMoves().add(move);mon.getMoveset().getReminderMoves().add(tackle);
            check(!mon.getRelearnableMoves().contains(move)&&mon.getRelearnableMoves().contains(tackle),"Old individual forgotten record filtered, other reminder retained");check(!NPCRelearner.getRelearnableMoves(mon).contains(move),"Native final relearner list excludes Tera Blast");
            var restored=PokemonFactory.create(mon.writeToNBT(new CompoundNBT()));check(!NPCRelearner.getRelearnableMoves(restored).contains(move),"Old forgotten record still excluded after native NBT reload");
            var available=new TeachMoveEvent.CanLearnMove(player,mon,move);Pixelmon.EVENT_BUS.post(available);check(available.isCanceled(),"NPC availability event blocked");var pre=new TeachMoveEvent.MoveLearnt.Pre(player,mon,new NPCRelearner(player.func_71121_q()),move);Pixelmon.EVENT_BUS.post(pre);check(pre.isCanceled(),"NPC confirmation event blocked");var normal=new TeachMoveEvent.CanLearnMove(player,mon,tackle);Pixelmon.EVENT_BUS.post(normal);check(!normal.isCanceled(),"Other move event preserved");
            mon.getMoveset().add(new Attack(move));check(mon.getMoveset().hasAttack(move),"Already known Tera Blast preserved");mon.getMoveset().removeAttack(move);
            mon.setLevelNum(49);mon.getPokemonLevelContainer().awardEXP(mon.getExperienceToLevelUp());check(mon.getPokemonLevel()>=50&&!mon.getMoveset().hasAttack(move),"Native 49 to 50 does not teach Tera Blast");LearnMoveController.clearLearnMoves(player);
            check(DisposableTeraDisc.compatible(mon)&&mon.getForm().getMoves().canLearn(TechnicalMoveItem.getMove(TechnicalMoveItem.of(TMType.TM9,tm))),"One-use and native TM9-171 compatibility retained");
            var entity=mon.getOrCreatePixelmon(player.func_71121_q(),20,75,20);var disc=new ItemStack(Backport.item("tera_blast_disc_single_use"),2);player.func_184611_a(Hand.MAIN_HAND,disc);check(DisposableTeraDisc.interact(entity,player,Hand.MAIN_HAND,disc)&&mon.getMoveset().hasAttack(move)&&disc.func_190916_E()==1,"Actual one-use disc teaches and consumes exactly one");
            mon.getMoveset().removeAttack(move);var nativeTm=TechnicalMoveItem.of(TMType.TM9,tm);player.func_184611_a(Hand.MAIN_HAND,nativeTm);entity.func_233661_c_(player,Hand.MAIN_HAND);check(mon.getMoveset().hasAttack(move),"Actual original TM9 item still teaches Tera Blast");
            System.out.println("R8_TM_ONLY_PASS species="+speciesCount+" forms="+forms+" machineCompatible="+compatible+"; all level/tutor/egg/transfer tables, old reminder NBT, final NPC list, NPC events, actual49->50, known move preserved, original TM9 and consuming disc actual teaching");
        }finally{LearnMoveController.clearLearnMoves(player);for(int i=0;i<6;i++)party.set(i,saved[i]);player.field_71071_by.func_70443_b(inventory);}
    }
}
