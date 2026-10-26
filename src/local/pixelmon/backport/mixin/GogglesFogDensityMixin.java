package local.pixelmon.backport.mixin;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=com.pixelmonmod.pixelmon.world.listener.UltraSpaceClientListener.class,remap=false)
public abstract class GogglesFogDensityMixin {
 @Redirect(method="onRenderFog",at=@At(value="INVOKE",target="Lnet/minecraft/entity/player/PlayerInventory;func_70440_f(I)Lnet/minecraft/item/ItemStack;"))
 private ItemStack backport$head(PlayerInventory inventory,int index){return local.pixelmon.backport.GogglesCurios.fogEquipment(inventory.func_70440_f(index),net.minecraft.client.Minecraft.func_71410_x().field_71439_g);}
}
