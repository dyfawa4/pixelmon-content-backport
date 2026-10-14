package local.pixelmon.backport.mixin;

import local.pixelmon.backport.client.DetectorConfigText;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Pseudo @Mixin(targets="com.mrcrayfish.configured.client.screen.ConfigScreen$ConfigItem",remap=false)
public abstract class DetectorConfigTooltipMixin {
    @Redirect(method="createToolTip",at=@At(value="INVOKE",target="Lcom/mrcrayfish/configured/api/IConfigValue;getPath()Ljava/lang/String;"))
    private String backport$header(@Coerce Object holder){return DetectorConfigText.tooltipHeader(holder);}
}
