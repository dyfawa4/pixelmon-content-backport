package local.pixelmon.backport.mixin;
import local.pixelmon.backport.LegendAdvancements;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=PixelmonWrapper.class,remap=false)
public abstract class PrimalAdvancementMixin {
    @Unique private boolean backport$wasPrimal;
    @Inject(method="checkPrimalReversion",at=@At("HEAD"))
    private void backport$beforePrimal(CallbackInfo ci){backport$wasPrimal=((PixelmonWrapper)(Object)this).getForm().isForm("primal");}
    @Inject(method="checkPrimalReversion",at=@At("RETURN"))
    private void backport$completedPrimal(CallbackInfo ci){if(!backport$wasPrimal)LegendAdvancements.primal((PixelmonWrapper)(Object)this);}
}
