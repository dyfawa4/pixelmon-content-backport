package local.pixelmon.backport.mixin;
import local.pixelmon.backport.NinjaTraining;
import com.pixelmonmod.pixelmon.battles.controller.GlobalStatusController;
import com.pixelmonmod.pixelmon.battles.status.Terrain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=Terrain.class,remap=false)
public abstract class NinjaTerrainClockMixin {
    @Inject(method="applyRepeatedEffect(Lcom/pixelmonmod/pixelmon/battles/controller/GlobalStatusController;)V",at=@At("HEAD"),cancellable=true)
    private void backport$freezeSharedClock(GlobalStatusController global,CallbackInfo ci){if(NinjaTraining.personalActor(global.bc)!=null)ci.cancel();}
}
