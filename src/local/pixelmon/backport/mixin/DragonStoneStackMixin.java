package local.pixelmon.backport.mixin;
import local.pixelmon.backport.Backport;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
/** Migrate named NBT stacks too, including Pixelmon's separately saved held items. */
@Mixin(value=ItemStack.class,remap=false)
public abstract class DragonStoneStackMixin {
    @ModifyVariable(method="func_199557_a",at=@At("HEAD"),argsOnly=true)
    private static CompoundNBT backport$oldStone(CompoundNBT nbt){
        if(!nbt.func_74779_i("id").equals("pixelmon_backport:dragonitite"))return nbt;
        CompoundNBT copy=nbt.func_74737_b();copy.func_74778_a("id",Backport.item("dragoninite").getRegistryName().toString());return copy;
    }
}
