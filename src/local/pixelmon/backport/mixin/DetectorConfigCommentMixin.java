package local.pixelmon.backport.mixin;

import local.pixelmon.backport.client.DetectorConfigText;
import net.minecraftforge.common.ForgeConfigSpec;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Translate tooltips only for detector settings; leave other mods' comments intact. */
@Pseudo @Mixin(targets="com.mrcrayfish.configured.impl.ForgeValue",remap=false)
public abstract class DetectorConfigCommentMixin {
    @Shadow @Final public ForgeConfigSpec.ValueSpec valueSpec;
    @Inject(method="getComment",at=@At("HEAD"),cancellable=true)
    private void backport$comment(CallbackInfoReturnable<String> cir){
        String key=valueSpec.getTranslationKey();
        String value=key==null?null:DetectorConfigText.text(key+".tooltip");
        if(value!=null)cir.setReturnValue(value);
    }
}
