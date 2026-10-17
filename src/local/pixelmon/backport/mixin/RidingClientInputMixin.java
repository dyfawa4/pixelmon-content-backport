package local.pixelmon.backport.mixin;
import local.pixelmon.backport.RidingMotion;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(targets={"com.pixelmonmod.pixelmon.client.keybindings.MovementHandler","com.pixelmonmod.pixelmon.client.keybindings.OldMovementHandler"},remap=false)
public abstract class RidingClientInputMixin {
    @Inject(method="handleRidingMovement",at=@At("HEAD"),cancellable=true)
    private static void backport$oneInputStream(CallbackInfo ci){var p=Minecraft.func_71410_x().field_71439_g;if(p!=null&&RidingMotion.active(p.func_184187_bx()))ci.cancel();}
}
