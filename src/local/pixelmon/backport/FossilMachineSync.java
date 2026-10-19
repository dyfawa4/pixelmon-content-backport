package local.pixelmon.backport;

import com.pixelmonmod.pixelmon.blocks.tileentity.FossilMachineTileEntity;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

/** Synchronize native progress checkpoints without changing revival or collection. */
public final class FossilMachineSync {
    private FossilMachineSync(){}
    public static void checkpoint(FossilMachineTileEntity machine,World world,BlockPos pos,BlockState before,BlockState after){
        world.func_225319_b(pos,before,after);
        if(world instanceof ServerWorld){
            machine.func_70296_d();
            // The old notification above is a render hook, a no-op on the server.
            // Use the same queued tile update path as native machine interaction.
            ((ServerWorld)world).func_72863_F().func_217217_a(pos);
        }
    }
}
