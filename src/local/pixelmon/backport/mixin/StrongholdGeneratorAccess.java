package local.pixelmon.backport.mixin;
import java.util.List;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.*;

@Mixin(ChunkGenerator.class)
public interface StrongholdGeneratorAccess {
    @Invoker("func_235958_g_") void backport$initializeStrongholds();
    @Accessor("field_235951_f_") List<ChunkPos> backport$strongholdRings();
}
