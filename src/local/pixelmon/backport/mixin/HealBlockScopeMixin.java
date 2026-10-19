package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.battles.status.*;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Heal Block is a volatile on a Pokemon, not a side condition. */
@Mixin(value=HealBlock.class,remap=false)
public abstract class HealBlockScopeMixin {
    @Inject(method="isTeamStatus",at=@At("HEAD"),cancellable=true)
    private void backport$individual(CallbackInfoReturnable<Boolean> cir){cir.setReturnValue(false);}
    @Redirect(method="applyEffect",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/battles/controller/participants/PixelmonWrapper;addTeamStatus(Lcom/pixelmonmod/pixelmon/battles/status/StatusBase;Lcom/pixelmonmod/pixelmon/battles/controller/participants/PixelmonWrapper;)Z"))
    private boolean backport$oneTarget(PixelmonWrapper target,StatusBase status,PixelmonWrapper source){return target.addStatus(status,source);}
    @Redirect(method="applyRepeatedEffect",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/battles/controller/participants/PixelmonWrapper;removeTeamStatus(Lcom/pixelmonmod/pixelmon/battles/status/StatusBase;)Z"))
    private boolean backport$oneExpiry(PixelmonWrapper target,StatusBase status){boolean present=target.getStatuses().contains(status);target.removeStatus(status);return present;}
}
