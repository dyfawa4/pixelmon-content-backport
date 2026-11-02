package local.pixelmon.backport.mixin;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResultType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(targets="com.pixelmonmod.pixelmon.items.heldItems.OrbItem",remap=false)
public abstract class OrbPlacementSoundMixin {
 @Inject(method="func_195939_a",at=@At("RETURN"))
 private void backport$sound(ItemUseContext c,CallbackInfoReturnable<ActionResultType> ci){if(ci.getReturnValue()==ActionResultType.SUCCESS&&!c.func_195991_k().field_72995_K&&c.func_195991_k().func_180495_p(c.func_195995_a().func_177984_a()).func_177230_c() instanceof com.pixelmonmod.pixelmon.blocks.OrbBlock)local.pixelmon.backport.DeltaOrb.placementSound(c.func_195991_k(),c.func_195995_a().func_177984_a());}
}
