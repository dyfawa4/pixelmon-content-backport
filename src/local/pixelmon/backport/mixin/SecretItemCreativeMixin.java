package local.pixelmon.backport.mixin;
import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=com.pixelmonmod.pixelmon.items.PixelmonItem.class,remap=false)
public abstract class SecretItemCreativeMixin {
 @Inject(method="func_150895_a",at=@At("HEAD"),cancellable=true,require=1)
 private void backport$secret(net.minecraft.item.ItemGroup group,net.minecraft.util.NonNullList<net.minecraft.item.ItemStack> items,CallbackInfo ci){if(local.pixelmon.backport.SecretItems.hidden((net.minecraft.item.Item)(Object)this))ci.cancel();}
}
