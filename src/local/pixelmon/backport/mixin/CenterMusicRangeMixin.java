package local.pixelmon.backport.mixin;
import local.pixelmon.backport.CenterMusic;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Clamp saved center blocks too, including old native tiles without custom marker NBT. */
@Mixin(value=com.pixelmonmod.pixelmon.blocks.tileentity.MusicBlockTileEntity.class,remap=false)
public abstract class CenterMusicRangeMixin {
 @Shadow private ResourceLocation track;
 @Shadow private BlockPos size;
 @Inject(method="func_230337_a_",at=@At("RETURN"))
 private void backport$centerRange(BlockState state,CompoundNBT tag,CallbackInfo ci){size=CenterMusic.limit(track,size);}
}
