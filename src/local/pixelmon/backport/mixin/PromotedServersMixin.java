package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.client.gui.override.multiplayer.CustomServerList;
import com.pixelmonmod.pixelmon.client.gui.override.multiplayer.CustomServerData;
import java.util.List;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Existing native gold-border promotions are hidden; ordinary saved entries remain. */
@Mixin(value=CustomServerList.class,remap=false)
public abstract class PromotedServersMixin {
    @Shadow @Final private List<CustomServerData> serverList;
    @Shadow @Final private List<CustomServerData> favouritedServers;
    @Inject(method="func_78853_a",at=@At("RETURN"))
    private void backport$removePromotions(CallbackInfo ci){
        if(serverList!=null)serverList.removeIf(s->s.goldBorder);
        if(favouritedServers!=null)favouritedServers.removeIf(s->s.goldBorder);
    }
}
