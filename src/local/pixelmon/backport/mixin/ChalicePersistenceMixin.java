package local.pixelmon.backport.mixin;
import java.util.UUID;
import com.pixelmonmod.pixelmon.blocks.tileentity.ArcChaliceTileEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.block.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=ArcChaliceTileEntity.class,remap=false)
public abstract class ChalicePersistenceMixin {
    @Shadow private UUID lastUsedPlayer;
    @Inject(method="func_189515_b",at=@At("RETURN"))
    private void backport$save(CompoundNBT nbt,CallbackInfoReturnable<CompoundNBT> ci){if(lastUsedPlayer!=null)ci.getReturnValue().func_186854_a("BackportLastUsedPlayer",lastUsedPlayer);}
    @Inject(method="func_230337_a_",at=@At("RETURN"))
    private void backport$load(BlockState state,CompoundNBT nbt,CallbackInfo ci){lastUsedPlayer=nbt.func_186855_b("BackportLastUsedPlayer")?nbt.func_186857_a("BackportLastUsedPlayer"):null;}
}
