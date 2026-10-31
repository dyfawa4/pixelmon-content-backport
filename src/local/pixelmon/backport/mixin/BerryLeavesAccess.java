package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.blocks.BerryLeavesBlock;
import com.pixelmonmod.pixelmon.enums.BerryType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value=BerryLeavesBlock.class,remap=false)
public interface BerryLeavesAccess {@Accessor("berry") BerryType backport$berry();}
