package local.pixelmon.backport.mixin;

import local.pixelmon.backport.client.DetectorConfigText;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo @Mixin(targets="com.mrcrayfish.configured.client.screen.ConfigScreen",remap=false)
public abstract class DetectorConfigLabelMixin {
    @Inject(method="createLabelFromHolder",at=@At("HEAD"),cancellable=true)
    private static void backport$label(@Coerce Object holder,CallbackInfoReturnable<String> cir){
        String value=DetectorConfigText.label(holder);
        if(value!=null)cir.setReturnValue(value);
    }
}
