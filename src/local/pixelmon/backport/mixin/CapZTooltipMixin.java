package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.items.PixelmonItem;
import com.pixelmonmod.pixelmon.api.registries.PixelmonItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Remove only Pikashunium Z's obsolete native move description. */
@Mixin(value=PixelmonItem.class,remap=false)
public abstract class CapZTooltipMixin {
    @Inject(method="getTooltipText()Ljava/lang/String;",at=@At("HEAD"),cancellable=true)
    private void backport$noObsoleteCapZDescription(CallbackInfoReturnable<String> cir){
        if((Object)this==PixelmonItems.pikashunium_z)cir.setReturnValue("");
    }
    @Inject(method="getTooltipText()Ljava/lang/String;",at=@At("RETURN"),cancellable=true)
    private void backport$lightBallDescription(CallbackInfoReturnable<String> cir){
        if((Object)this==PixelmonItems.light_ball){
            String extra=net.minecraft.client.resources.I18n.func_135052_a("pixelmon.backport.light_ball.cap_description");
            String nativeText=cir.getReturnValue();cir.setReturnValue(nativeText==null||nativeText.isEmpty()?extra:nativeText+"\n"+extra);
        }
    }
}
