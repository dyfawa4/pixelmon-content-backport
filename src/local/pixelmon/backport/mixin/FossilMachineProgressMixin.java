package local.pixelmon.backport.mixin;

import com.pixelmonmod.pixelmon.blocks.tileentity.FossilMachineTileEntity;
import local.pixelmon.backport.FossilMachineSync;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(value=FossilMachineTileEntity.class,remap=false)
public abstract class FossilMachineProgressMixin {
    @Redirect(method="func_73660_a",at=@At(value="INVOKE",target="Lnet/minecraft/world/World;func_225319_b(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/block/BlockState;)V"),require=2,allow=2)
    private void backport$syncProgress(World world,BlockPos pos,BlockState before,BlockState after){
        FossilMachineSync.checkpoint((FossilMachineTileEntity)(Object)this,world,pos,before,after);
    }
}
