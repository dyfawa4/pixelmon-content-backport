package local.pixelmon.backport.mixin;
import local.pixelmon.backport.RidingMotion;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.network.play.client.CInputPacket;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Preserve the standard touch/controller rider input alongside the optional custom key stream. */
@Mixin(value=ServerPlayNetHandler.class,remap=false)
public abstract class RidingStandardInputMixin {
    @Shadow public ServerPlayerEntity field_147369_b;
    @Inject(method="func_147358_a",at=@At("TAIL"))
    private void backport$standardRiding(CInputPacket packet,CallbackInfo ci){RidingMotion.standardInput(field_147369_b,packet.func_149620_c(),packet.func_192620_b(),packet.func_149618_e());}
}
