package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.battles.controller.ai.BattleAIBase;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.controller.log.MoveResults;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import local.pixelmon.backport.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=BattleAIBase.class,remap=false)
public abstract class NpcErosionPreviewMixin {
    @Redirect(method="weightOffensiveMove(Lcom/pixelmonmod/pixelmon/battles/controller/participants/PixelmonWrapper;Lcom/pixelmonmod/pixelmon/battles/controller/ai/MoveChoice;Ljava/util/List;)V",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/battles/controller/participants/PixelmonWrapper;setAttack(Lcom/pixelmonmod/pixelmon/battles/attacks/Attack;Ljava/util/List;Z)V"),require=1)
    private void backport$mutablePreviewTargets(PixelmonWrapper user,Attack attack,java.util.List<PixelmonWrapper> targets,boolean alreadySelected){
        // Dragon Darts removes/replaces its current target between strikes.
        // Forecast choices may use singleton/fixed lists and must remain intact
        // for weighting and later execution. Native weighting restores user.targets.
        user.setAttack(attack,new java.util.ArrayList<>(targets),alreadySelected);
    }
    @Redirect(method="weightOffensiveMove(Lcom/pixelmonmod/pixelmon/battles/controller/participants/PixelmonWrapper;Lcom/pixelmonmod/pixelmon/battles/controller/ai/MoveChoice;Ljava/util/List;)V",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/battles/attacks/Attack;use(Lcom/pixelmonmod/pixelmon/battles/controller/participants/PixelmonWrapper;Lcom/pixelmonmod/pixelmon/battles/controller/participants/PixelmonWrapper;Lcom/pixelmonmod/pixelmon/battles/controller/log/MoveResults;)Z"),require=2)
    private boolean backport$signature(Attack attack,PixelmonWrapper user,PixelmonWrapper target,MoveResults result){
        if(!AshWaterBoost.eligible(user)||user.usingZ||!attack.getMove().getAttackName().equals("Water Shuriken")||user.isAlly(target))return attack.use(user,target,result);
        PixelmonWrapper projected=BackportConfig.erosionEnabled()?Erosion.preview(target):NpcSimulationCopy.of(target);
        PixelmonWrapper actor=NpcSimulationCopy.of(user);
        actor.attack=attack;actor.selectedAttack=attack;actor.targets=new java.util.ArrayList<>(java.util.Collections.singletonList(projected));
        // MultipleHit resolves its later strikes through user.targets. All hits
        // must see the same detached opponent, not the original live wrapper.
        AshWaterBoost.start(actor);
        try(var capture=NpcSignaturePreview.begin(actor)){
            boolean hit=attack.use(actor,projected,result);capture.apply(result,target,projected);return hit;
        }
    }
}
