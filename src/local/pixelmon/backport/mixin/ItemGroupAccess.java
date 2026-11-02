package local.pixelmon.backport.mixin;
import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value=net.minecraft.item.Item.class,remap=false)
public interface ItemGroupAccess {
 @Accessor("field_77701_a") @Mutable void backport$group(net.minecraft.item.ItemGroup group);
}
