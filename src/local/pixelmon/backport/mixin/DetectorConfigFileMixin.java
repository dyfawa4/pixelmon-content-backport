package local.pixelmon.backport.mixin;

import local.pixelmon.backport.client.DetectorConfigText;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo @Mixin(targets="com.mrcrayfish.configured.client.screen.ModConfigSelectionScreen",remap=false)
public abstract class DetectorConfigFileMixin {
    @Inject(method="createLabelFromModConfig",at=@At("HEAD"),cancellable=true)
    private static void backport$file(@Coerce Object config,CallbackInfoReturnable<String> cir){
        String label=DetectorConfigText.configFileLabel(config);if(label!=null)cir.setReturnValue(label);
    }
}
