package local.pixelmon.backport;

import com.pixelmonmod.pixelmon.api.events.battles.BagItemEvent;
import com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.PokeBall;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import com.pixelmonmod.pixelmon.items.PokeBallPart;
import net.minecraftforge.eventbus.api.*;

/** All field balls use the native Boss rejection; battle ball rules stay separate. */
public final class CapBallGuard {
    public static final String MESSAGE="pixelmon.backport.cap.forbidden_ball";
    public static final String FIELD_MESSAGE="pixelmon.backport.cap.field_capture_blocked";
    public static boolean fieldBlocked(PokeBall ball,PixelmonEntity target){
        return ball!=null && target!=null && !target.hasOwner() && target.getTrainer()==null
            && target.getPokemon()!=null && target.getPokemon().getSpecies().getDex()==CapSpecies.DEX;
    }
    public static boolean blocked(PlayerParticipant participant,PokeBall ball){
        var battle=participant.bc;
        if(battle==null || battle.simulateMode || battle.battleEnded || battle.isRaid() || TeraRaids.get(battle)!=null)return false;
        for(var enemy:participant.getOpponents())if(enemy instanceof WildPixelmonParticipant)
            for(var target:enemy.controlledPokemon)if(target.onBattlefield && target.isAlive() && target.entity!=null
                && BurstCapture.forbidden(ball,target.pokemon))return true;
        return false;
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public void choose(BagItemEvent.SelectItem.Pre event){
        var battle=event.getBattleController();var owner=event.getPlayer();var stack=event.getFoundItem();
        if(event.isCanceled() || battle==null || owner==null || stack==null || stack.func_190926_b())return;
        var ball=PokeBallPart.getPokeBall(stack);if(!ball.isPresent())return;
        var participant=battle.getParticipantForEntity(owner);
        if(!(participant instanceof PlayerParticipant) || ((PlayerParticipant)participant).player!=owner || !blocked((PlayerParticipant)participant,ball.get()))return;
        event.setCanceled(true);
        owner.func_146105_b(new net.minecraft.util.text.TranslationTextComponent(MESSAGE),false);
        BossCaptureGuard.restore((PlayerParticipant)participant);
    }
}
