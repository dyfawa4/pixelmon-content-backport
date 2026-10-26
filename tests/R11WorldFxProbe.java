package qa.mixin;
import qa.Update180R11Cases;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value={net.minecraft.world.server.ServerWorld.class,net.minecraft.client.world.ClientWorld.class},remap=false)
public abstract class R11WorldFxProbe {
    @Inject(method="func_217378_a",at=@At("HEAD"))private void record(PlayerEntity p,int event,BlockPos pos,int state,CallbackInfo ci){Update180R11Cases.event((World)(Object)this,event,state);}
}
