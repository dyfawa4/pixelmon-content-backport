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
    @Redirect(method="weightOffensiveMove(Lcom/pixelmonmod/pixelmon/battles/controller/participants/PixelmonWrapper;Lcom/pixelmonmod/pixelmon/battles/controller/ai/MoveChoice;Ljava/util/List;)V",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/battles/attacks/Attack;use(Lcom/pixelmonmod/pixelmon/battles/controller/participants/PixelmonWrapper;Lcom/pixelmonmod/pixelmon/battles/controller/participants/PixelmonWrapper;Lcom/pixelmonmod/pixelmon/battles/controller/log/MoveResults;)Z"),require=2)
    private boolean backport$signature(Attack attack,PixelmonWrapper user,PixelmonWrapper target,MoveResults result){
        PixelmonWrapper projected=BackportConfig.erosionEnabled()&&AshWaterBoost.eligible(user)&&!user.usingZ&&attack.getMove().getAttackName().equals("Water Shuriken")&&!user.isAlly(target)?Erosion.preview(target):target;
        return attack.use(user,projected,result);
    }
}
