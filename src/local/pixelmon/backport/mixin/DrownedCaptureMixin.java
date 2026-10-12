package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.api.events.CaptureEvent;
import com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.PokeBallRegistry;
import com.pixelmonmod.pixelmon.world.dimension.drowned.DrownedWorld;
import com.pixelmonmod.pixelmon.world.listener.DrownedWorldListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replace only the dimension's own restriction; never un-cancel another listener. */
@Mixin(value=DrownedWorldListener.class,remap=false)
public abstract class DrownedCaptureMixin {
    @Inject(method="onCapture",at=@At("HEAD"),cancellable=true)
    private static void backport$duskOnly(CaptureEvent.StartCapture event,CallbackInfo ci){
        if(event.getPlayer()!=null && DrownedWorld.is(event.getPlayer().func_71121_q())){
            if(event.getPokeBall()==null || !event.getPokeBall().getBallType().is(PokeBallRegistry.DUSK_BALL))event.setCanceled(true);
            ci.cancel();
        }
    }
}
