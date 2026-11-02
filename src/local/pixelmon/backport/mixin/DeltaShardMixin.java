package local.pixelmon.backport.mixin;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResultType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(targets="com.pixelmonmod.pixelmon.items.ShardItem",remap=false)
public abstract class DeltaShardMixin {
 @Inject(method="func_195939_a",at=@At("HEAD"),cancellable=true)
 private void backport$delta(ItemUseContext context,CallbackInfoReturnable<ActionResultType> ci){if(local.pixelmon.backport.DeltaOrb.shard(context.func_195996_i()))ci.setReturnValue(local.pixelmon.backport.DeltaOrb.place(context));}
}
