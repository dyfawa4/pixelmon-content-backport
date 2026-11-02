package local.pixelmon.backport.mixin;
import net.minecraft.item.*;
import net.minecraft.util.ActionResultType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** The upstream jade orb is a QuestItem, not an OrbItem. */
@Mixin(value=Item.class,remap=false)
public abstract class DeltaOrbItemMixin {
 @Inject(method="func_195939_a",at=@At("HEAD"),cancellable=true)
 private void backport$place(ItemUseContext context,CallbackInfoReturnable<ActionResultType> ci){if(context.func_195996_i().func_77973_b()==com.pixelmonmod.pixelmon.api.registries.PixelmonItems.jade_orb)ci.setReturnValue(local.pixelmon.backport.DeltaOrb.placeOrb(context));}
}
