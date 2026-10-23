package local.pixelmon.backport.mixin;
import local.pixelmon.backport.client.DetectorConfigText;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
@Pseudo @Mixin(targets="com.mrcrayfish.configured.client.screen.EditListScreen$StringEntry",remap=false)
public abstract class DetectorListEntryMixin {
    @ModifyArg(method="func_230432_a_",at=@At(value="INVOKE",target="Lnet/minecraft/util/text/StringTextComponent;<init>(Ljava/lang/String;)V"),index=0)
    private String backport$value(String raw){return DetectorConfigText.entryValue(this,raw);}
    @ModifyArg(method="<init>",at=@At(value="INVOKE",target="Lnet/minecraft/util/text/StringTextComponent;<init>(Ljava/lang/String;)V"),index=0)
    private String backport$edit(String raw){String text=DetectorConfigText.text("pixelmondetector.config.button.edit");return text==null?raw:text;}
}
