package local.pixelmon.backport.mixin;
import java.util.List;
import local.pixelmon.backport.client.ExpandedItemTooltips;
import net.minecraft.item.*;
import net.minecraft.world.World;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(ItemStack.class)
public abstract class ExpandedItemTooltipMixin {
 @Redirect(method="func_82840_a",at=@At(value="INVOKE",target="Lnet/minecraft/item/Item;func_77624_a(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Ljava/util/List;Lnet/minecraft/client/util/ITooltipFlag;)V"),require=1)
 private void backport$expandedItem(Item item,ItemStack stack,World world,List<ITextComponent> lines,ITooltipFlag flag){ExpandedItemTooltips.during(()->{item.func_77624_a(stack,world,lines,flag);return null;});}
 @Redirect(method="func_82840_a",at=@At(value="INVOKE",target="Lnet/minecraftforge/event/ForgeEventFactory;onItemTooltip(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/PlayerEntity;Ljava/util/List;Lnet/minecraft/client/util/ITooltipFlag;)Lnet/minecraftforge/event/entity/player/ItemTooltipEvent;",remap=false),require=1)
 private ItemTooltipEvent backport$expandedExtensions(ItemStack stack,PlayerEntity player,List<ITextComponent> lines,ITooltipFlag flag){return ExpandedItemTooltips.during(()->ForgeEventFactory.onItemTooltip(stack,player,lines,flag));}
}
