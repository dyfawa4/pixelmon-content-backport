package local.pixelmon.backport.mixin;
import local.pixelmon.backport.LuckySpecies;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo @Mixin(targets="pokelucky.Process",remap=false)
public abstract class LuckySpeciesMixin {
    @Inject(method="pokemon",at=@At("HEAD"),cancellable=true,require=1,allow=1)
    private static void backport$normal(World world,BlockPos pos,CallbackInfo ci){LuckySpecies.spawn(world,pos,0);ci.cancel();}
    @Inject(method="beast",at=@At("HEAD"),cancellable=true,require=1,allow=1)
    private static void backport$beast(World world,BlockPos pos,CallbackInfo ci){LuckySpecies.spawn(world,pos,1);ci.cancel();}
    @Inject(method="legend",at=@At("HEAD"),cancellable=true,require=1,allow=1)
    private static void backport$legend(World world,BlockPos pos,CallbackInfo ci){LuckySpecies.spawn(world,pos,2);ci.cancel();}
}
