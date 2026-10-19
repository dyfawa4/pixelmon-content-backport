package local.pixelmon.backport.mixin;
import local.pixelmon.backport.BattleMechanics;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.battles.status.StatusBase;
import net.minecraft.util.text.TranslationTextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=PixelmonWrapper.class,remap=false)
public abstract class BattleStatusSourceMixin {
    @Inject(method="addStatus(Lcom/pixelmonmod/pixelmon/battles/status/StatusBase;Lcom/pixelmonmod/pixelmon/battles/controller/participants/PixelmonWrapper;Lnet/minecraft/util/text/TranslationTextComponent;)Z",at=@At("RETURN"))
    private void backport$statusSource(StatusBase status,PixelmonWrapper source,TranslationTextComponent message,CallbackInfoReturnable<Boolean> cir){
        BattleMechanics.statusAdded(status,(PixelmonWrapper)(Object)this,source,cir.getReturnValue());
    }
}
