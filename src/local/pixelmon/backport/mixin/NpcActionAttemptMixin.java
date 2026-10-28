package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import local.pixelmon.backport.AuthorForesight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** A failed status check is still an attempted action; Instruct can grant another in the same turn. */
@Mixin(value=PixelmonWrapper.class,remap=false)
public abstract class NpcActionAttemptMixin {
    @Inject(method="canAttackThisTurn",at=@At("HEAD"))
    private void backport$beginStatus(Attack attack,CallbackInfoReturnable<Boolean> cir){AuthorForesight.beginStatus((PixelmonWrapper)(Object)this);}
    @Inject(method="canAttackThisTurn",at=@At("RETURN"))
    private void backport$endStatus(Attack attack,CallbackInfoReturnable<Boolean> cir){AuthorForesight.endStatus((PixelmonWrapper)(Object)this);}
}
