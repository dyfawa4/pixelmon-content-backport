package local.pixelmon.backport;

import java.util.*;
import com.pixelmonmod.pixelmon.api.events.battles.BagItemEvent;
import com.pixelmonmod.pixelmon.api.util.helpers.NetworkHelper;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import com.pixelmonmod.pixelmon.comm.packetHandlers.battles.BackToMainMenuPacket;
import com.pixelmonmod.pixelmon.items.PokeBallPart;
import net.minecraftforge.eventbus.api.*;

/** Recheck native wild Boss tiers before a stale bag selection commits an action. */
public final class BossCaptureGuard {
    public static boolean blocked(PlayerParticipant player){
        if(player==null || player.bc==null || player.bc.simulateMode || player.bc.battleEnded
            || player.bc.isRaid() || TeraRaids.get(player.bc)!=null)return false;
        for(var enemy:player.getOpponents())if(enemy instanceof WildPixelmonParticipant)
            for(var target:enemy.controlledPokemon)if(target.onBattlefield && target.isAlive()
                && target.entity!=null && target.entity.getBossTier().isBoss())return true;
        return false;
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public void choose(BagItemEvent.SelectItem.Pre event){
        var battle=event.getBattleController();var owner=event.getPlayer();var stack=event.getFoundItem();
        if(event.isCanceled() || battle==null || owner==null || stack==null || stack.func_190926_b()
            || !PokeBallPart.getPokeBall(stack).isPresent())return;
        var participant=battle.getParticipantForEntity(owner);
        if(!(participant instanceof PlayerParticipant) || ((PlayerParticipant)participant).player!=owner
            || !blocked((PlayerParticipant)participant))return;
        event.setCanceled(true); // Native BagPacket returns before setUseItem/inventory/turn changes.
        owner.func_146105_b(new net.minecraft.util.text.TranslationTextComponent("gui.choosebag.nopokeballs"),false);
        if(!battle.isWaiting())return;
        List<UUID> picking=new ArrayList<>();List<Boolean> switching=new ArrayList<>();boolean flee=true;
        for(var member:participant.controlledPokemon)if(member.wait && member.onBattlefield && member.isAlive()){
            boolean[] allowed=PlayerParticipant.canSwitch(member);
            picking.add(member.getPokemonUUID());switching.add(allowed[0]);flee&=allowed[1];
        }
        if(!picking.isEmpty())NetworkHelper.sendPacket(new BackToMainMenuPacket(switching,flee,picking),owner);
    }
}
