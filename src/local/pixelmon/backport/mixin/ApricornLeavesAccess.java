package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.blocks.ApricornLeavesBlock;
import com.pixelmonmod.pixelmon.enums.items.ApricornType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value=ApricornLeavesBlock.class,remap=false)
public interface ApricornLeavesAccess {@Accessor("apricorn") ApricornType backport$apricorn();}
