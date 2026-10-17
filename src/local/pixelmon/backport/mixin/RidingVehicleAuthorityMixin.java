package local.pixelmon.backport.mixin;
import local.pixelmon.backport.RidingMotion;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.ServerPlayNetHandler;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=ServerPlayNetHandler.class,remap=false)
public abstract class RidingVehicleAuthorityMixin {
    @Shadow public ServerPlayerEntity field_147369_b;
    @Inject(method="func_184338_a",at=@At("HEAD"),cancellable=true)
    private void backport$serverMovesMount(CallbackInfo ci){if(RidingMotion.active(field_147369_b.func_184187_bx()))ci.cancel();}
}
