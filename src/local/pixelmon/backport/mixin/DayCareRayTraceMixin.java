package local.pixelmon.backport.mixin;

import net.minecraft.world.IBlockReader;
import org.spongepowered.asm.mixin.Mixin;

/** Interface injection is applied by BackportMixinPlugin (Mixin 0.8 forbids @Inject here). */
@Mixin(IBlockReader.class)
public interface DayCareRayTraceMixin {}
