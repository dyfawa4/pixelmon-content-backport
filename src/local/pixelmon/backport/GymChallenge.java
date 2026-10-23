package local.pixelmon.backport;

import java.util.*;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import com.pixelmonmod.pixelmon.api.events.battles.UseBattleItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;

/** Incremental gym rules; wild, raid, PvP and ordinary trainers retain native rules. */
public final class GymChallenge {
    public static final int PLAYER_ITEMS=GymItemAllowance.TOTAL;
    private static final Map<BattleController,Map<BattleParticipant,GymItemAllowance>> USED=Collections.synchronizedMap(new WeakHashMap<>());
    private static boolean revival(net.minecraft.item.ItemStack stack){return stack!=null && stack.func_77973_b() instanceof com.pixelmonmod.pixelmon.items.ReviveItem;}
    public static boolean author(BattleController battle){
        return battle!=null&&!battle.simulateMode&&battle.participants.stream().anyMatch(p->p instanceof TrainerParticipant&&AuthorBossTeams.is(((TrainerParticipant)p).trainer));
    }
    public static boolean medicineDenied(BattleController battle,BattleParticipant participant,net.minecraft.item.ItemStack stack){
        return participant instanceof PlayerParticipant&&stack!=null
            &&BattleMedicines.medicine(stack)&&author(battle);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public void before(com.pixelmonmod.pixelmon.api.events.npc.NPCEvent.StartBattle event){
        TrainerParticipant trainer=event.getNPCParticipant();
        if(!event.isCanceled() && trainer!=null && NpcGimmicks.enabled(trainer.trainer) && event.player!=null)
            event.player.func_146105_b(new net.minecraft.util.text.TranslationTextComponent(AuthorBossTeams.is(trainer.trainer)?"pixelmon.backport.author.challenge.rules":"pixelmon.backport.gym.challenge.before"),false);
    }
    public static void prepare(TrainerParticipant trainer){
        BattleController battle=trainer.bc;if(battle==null || battle.simulateMode)return;
        boolean first=!USED.containsKey(battle);USED.computeIfAbsent(battle,b->new IdentityHashMap<>());
        for(BattleParticipant p:battle.participants)for(PixelmonWrapper member:p.allPokemon)member.enableReturnHeldItem();
        if(first)battle.sendToAll(author(battle)?"pixelmon.backport.author.challenge.rules":"pixelmon.backport.gym.challenge.rules",PLAYER_ITEMS,3);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public void choose(com.pixelmonmod.pixelmon.api.events.battles.BagItemEvent.SelectItem.Pre event){
        BattleController battle=event.getBattleController();
        if(event.isCanceled() || battle==null || battle.simulateMode || battle.battleEnded)return;
        Map<BattleParticipant,GymItemAllowance> uses=USED.get(battle);if(uses==null)return;
        for(BattleParticipant participant:battle.participants){
            if(!(participant instanceof PlayerParticipant) || ((PlayerParticipant)participant).player!=event.getPlayer())continue;
            GymItemAllowance allowance=uses.computeIfAbsent(participant,p->new GymItemAllowance());
            int pending=0,pendingRevivals=0;
            // In doubles, the last allowance may already be queued by a teammate.
            for(PixelmonWrapper member:participant.controlledPokemon)if(member.willUseItemInStack!=null){pending++;if(revival(member.willUseItemInStack))pendingRevivals++;}
            GymItemAllowance.Denial denial=allowance.denial(revival(event.getFoundItem()),pending,pendingRevivals);
            boolean forbidden=medicineDenied(battle,participant,event.getFoundItem());
            if(!forbidden&&denial==GymItemAllowance.Denial.NONE)return;
            event.setCanceled(true); // BagPacket returns before setUseItem changes wait/action state.
            String message=forbidden?"pixelmon.backport.author.challenge.no_medicine":denial==GymItemAllowance.Denial.REVIVAL?"pixelmon.backport.gym.challenge.revive_limit"
                :denial==GymItemAllowance.Denial.RESTORE?"pixelmon.backport.gym.challenge.restore_limit"
                :allowance.used()>=PLAYER_ITEMS?"pixelmon.backport.gym.challenge.limit":"pixelmon.backport.gym.challenge.reserved";
            event.getPlayer().func_146105_b(new net.minecraft.util.text.TranslationTextComponent(message,participant.getDisplayName(),PLAYER_ITEMS),false);
            if(battle.isWaiting()){
                List<UUID> picking=new ArrayList<>();List<Boolean> switching=new ArrayList<>();boolean flee=true;
                for(PixelmonWrapper member:participant.controlledPokemon)if(member.wait && member.isAlive() && member.onBattlefield){
                    boolean[] allowed=PlayerParticipant.canSwitch(member);picking.add(member.getPokemonUUID());switching.add(allowed[0]);flee&=allowed[1];
                }
                if(!picking.isEmpty())com.pixelmonmod.pixelmon.api.util.helpers.NetworkHelper.sendPacket(
                    new com.pixelmonmod.pixelmon.comm.packetHandlers.battles.BackToMainMenuPacket(switching,flee,picking),event.getPlayer());
            }
            return;
        }
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public void use(UseBattleItemEvent event){
        PixelmonWrapper user=event.getUser();if(event.isCanceled() || user==null || user.bc==null || user.bc.simulateMode)return;
        if(medicineDenied(user.bc,user.getParticipant(),event.getStack())){
            event.setCanceled(true);user.bc.sendToAll("pixelmon.backport.author.challenge.no_medicine");return;
        }
        Map<BattleParticipant,GymItemAllowance> uses=USED.get(user.bc);
        if(uses!=null && user.getParticipant() instanceof PlayerParticipant){
            GymItemAllowance allowance=uses.computeIfAbsent(user.getParticipant(),p->new GymItemAllowance());
            GymItemAllowance.Denial denial=allowance.denial(revival(event.getStack()),0,0);
            if(denial!=GymItemAllowance.Denial.NONE){
                event.setCanceled(true);user.bc.sendToAll(denial==GymItemAllowance.Denial.REVIVAL?"pixelmon.backport.gym.challenge.revive_limit":denial==GymItemAllowance.Denial.RESTORE?"pixelmon.backport.gym.challenge.restore_limit":"pixelmon.backport.gym.challenge.limit",user.getParticipant().getDisplayName(),PLAYER_ITEMS);
            }
        }
    }
    public static void committed(PixelmonWrapper user,net.minecraft.item.ItemStack stack){
        Map<BattleParticipant,GymItemAllowance> uses=USED.get(user.bc);
        if(uses==null || user.bc.simulateMode || !(user.getParticipant() instanceof PlayerParticipant))return;
        GymItemAllowance allowance=uses.computeIfAbsent(user.getParticipant(),p->new GymItemAllowance());allowance.successfulUse(revival(stack));
        user.bc.sendToAll("pixelmon.backport.gym.challenge.remaining",user.getParticipant().getDisplayName(),allowance.remaining(),allowance.remainingRevivals(),allowance.remainingRestores());
    }
    public static void end(BattleController battle){
        if(USED.remove(battle)==null || battle.simulateMode)return;
        // Restore exact NBT too: native cleanup compares only the HeldItem type.
        // This returns player-owned swaps and removes NPC loans on every end cause.
        for(BattleParticipant p:battle.participants)for(PixelmonWrapper member:p.allPokemon)
            if(member.pokemon!=null && member.initialCopyOfPokemon!=null)
                member.pokemon.setHeldItem(member.initialCopyOfPokemon.getHeldItem().func_77946_l());
    }
}
