package local.pixelmon.backport.mixin;
import java.lang.ref.WeakReference;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.world.WorldEvent;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
/** Invalidate the shared preview on item identity changes, even if UUIDs are equal. */
@Pseudo @Mixin(targets="net.p3pp3rf1y.sophisticatedbackpacks.client.render.BackpackTooltipRenderer",remap=false)
public abstract class BackpackTooltipIsolationMixin {
 @Shadow private static boolean shouldRefreshContents;
 @Shadow private static java.util.UUID backpackUuid;
 @Unique private static WeakReference<ItemStack> backport$previous=new WeakReference<>(null);
 @Inject(method="renderBackpackTooltip",at=@At("HEAD"))
 private static void backport$fresh(ItemStack stack,net.minecraft.client.Minecraft mc,net.minecraft.client.entity.player.ClientPlayerEntity player,com.mojang.blaze3d.matrix.MatrixStack matrix,int x,int y,net.minecraft.client.gui.FontRenderer font,CallbackInfoReturnable<Boolean> ci){if(backport$previous.get()!=stack){shouldRefreshContents=true;backport$previous=new WeakReference<>(stack);}}
 @Inject(method="onWorldLoad",at=@At("RETURN"))
 private static void backport$world(WorldEvent.Load event,CallbackInfo ci){backpackUuid=null;backport$previous.clear();shouldRefreshContents=true;}
}
