package local.pixelmon.backport.mixin;
import com.pixelmonmod.tcg.TCG;
import net.minecraftforge.event.RegisterCommandsEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=TCG.class,remap=false)
public abstract class TcgCommandsMixin {
    @Inject(method="serverLoad",at=@At("HEAD"),cancellable=true)
    private void backport$removeCommands(RegisterCommandsEvent event,CallbackInfo ci){ci.cancel();}
}
