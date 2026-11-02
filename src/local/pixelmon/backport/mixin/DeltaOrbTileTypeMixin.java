package local.pixelmon.backport.mixin;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** The native ORB type omitted jade_orb, so the client discarded its renderer. */
@Mixin(TileEntityType.class)
public abstract class DeltaOrbTileTypeMixin {
 @Inject(method="func_223045_a",at=@At("RETURN"),cancellable=true)
 private void backport$jade(Block block,CallbackInfoReturnable<Boolean> ci){if(!ci.getReturnValue()&&block==com.pixelmonmod.pixelmon.api.registries.PixelmonBlocks.jade_orb&&com.pixelmonmod.pixelmon.init.registry.TileEntityRegistration.ORB.isPresent()&&(Object)this==com.pixelmonmod.pixelmon.init.registry.TileEntityRegistration.ORB.get())ci.setReturnValue(true);}
}
