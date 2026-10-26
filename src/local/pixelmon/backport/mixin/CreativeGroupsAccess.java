package local.pixelmon.backport.mixin;
import net.minecraft.item.ItemGroup;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value=ItemGroup.class,remap=false)
public interface CreativeGroupsAccess {
    @Mutable @Accessor("field_78032_a") static void backport$groups(ItemGroup[] groups){throw new AssertionError();}
    @Mutable @Accessor("field_78033_n") void backport$index(int index);
}
