package local.pixelmon.backport.mixin;

import local.pixelmon.backport.client.DetectorConfigText;
import net.minecraftforge.common.ForgeConfigSpec;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Identify the detector by its spec, so common folder names in other mods are untouched. */
@Pseudo @Mixin(targets="com.mrcrayfish.configured.impl.ForgeFolderEntry",remap=false)
public abstract class DetectorConfigFolderMixin {
    @Shadow @Final private ForgeConfigSpec spec;
    @Shadow @Final private boolean root;
    @Inject(method="getEntryName",at=@At("RETURN"),cancellable=true)
    private void backport$folder(CallbackInfoReturnable<String> cir){
        String farmer=local.pixelmon.backport.client.FarmersDelightConfigText.folder(spec,root,cir.getReturnValue());
        if(farmer!=null){cir.setReturnValue(farmer);return;}
        Object value=spec.getRaw(java.util.Arrays.asList("general","allowDetector"));
        if(!(value instanceof ForgeConfigSpec.ValueSpec))return;
        if(!"pixelmondetector.config.allowDetector".equals(((ForgeConfigSpec.ValueSpec)value).getTranslationKey()))return;
        String key="pixelmondetector.config.category."+(root?"root":cir.getReturnValue());
        String translated=DetectorConfigText.text(key);
        if(translated!=null)cir.setReturnValue(translated);
    }
}
