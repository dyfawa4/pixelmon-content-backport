package local.pixelmon.backport.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Keep vanilla layout and amplitude; only halve the splash pulse speed, not the menu fade. */
@Mixin(targets="net.minecraft.client.gui.screen.MainMenuScreen",remap=false)
public abstract class TitleSplashMotionMixin {
    @ModifyConstant(method="func_230430_a_",constant=@Constant(longValue=1000L),
        slice=@Slice(from=@At(value="FIELD",target="Lnet/minecraft/client/gui/screen/MainMenuScreen;field_73975_c:Ljava/lang/String;",ordinal=0)),require=1)
    private long backport$pulsePeriod(long original){return 2000L;}
    @ModifyConstant(method="func_230430_a_",constant=@Constant(floatValue=1000F),
        slice=@Slice(from=@At(value="FIELD",target="Lnet/minecraft/client/gui/screen/MainMenuScreen;field_73975_c:Ljava/lang/String;",ordinal=0)),require=1)
    private float backport$pulseDivisor(float original){return 2000F;}
}
