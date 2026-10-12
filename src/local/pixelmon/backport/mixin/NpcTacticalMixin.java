package local.pixelmon.backport.mixin;
import java.util.List;
import com.pixelmonmod.pixelmon.battles.controller.ai.*;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=TacticalAI.class,remap=false)
public abstract class NpcTacticalMixin {
    @Inject(method="pickBestChoice(Ljava/util/List;Ljava/util/List;)Lcom/pixelmonmod/pixelmon/battles/controller/ai/MoveChoice;",at=@At("HEAD"),cancellable=true)
    private void backport$bestKnown(List<MoveChoice> choices,List<MoveChoice> bestChoices,CallbackInfoReturnable<MoveChoice> cir){
        List<MoveChoice> pool=choices==null?bestChoices:choices;
        if(pool==null || pool.isEmpty())return;
        PixelmonWrapper own=pool.get(0).user;
        if(own==null || own.bc==null || own.bc.isRaid() || !(own.getParticipant() instanceof com.pixelmonmod.pixelmon.battles.controller.participants.TrainerParticipant))return;
        var trainer=((com.pixelmonmod.pixelmon.battles.controller.participants.TrainerParticipant)own.getParticipant()).trainer;
        if(!local.pixelmon.backport.NpcGimmicks.enabled(trainer) || !local.pixelmon.backport.GymCities.isLeader(trainer))return;
        pool.stream().filter(c->c.tier>0 && Float.isFinite(c.weight))
            .max(java.util.Comparator.comparingInt((MoveChoice c)->c.tier).thenComparingDouble(c->c.weight)).ifPresent(cir::setReturnValue);
    }
    @Inject(method="getWeightedAttackChoices",at=@At("RETURN"))
    private void backport$context(PixelmonWrapper own,CallbackInfoReturnable<List<MoveChoice>> cir){
        local.pixelmon.backport.NpcTactics.weight(own,cir.getReturnValue());
    }
}
