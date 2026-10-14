package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.client.DefaultServerList;
import java.util.concurrent.CompletableFuture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
/** Block only the bundled advertisement download, never user connections. */
@Mixin(value=DefaultServerList.class,remap=false)
public abstract class DefaultServerListMixin {
    @Inject(method="tryFetchDefaultServers(Ljava/lang/Runnable;)V",at=@At("HEAD"),cancellable=true)
    private static void backport$noAds(Runnable complete,CallbackInfo ci){
        // The native callback opens a NEW CustomMultiplayerScreen. Its init
        // immediately calls this method again, even after a deferred callback.
        // No list was downloaded/changed, so there is nothing to refresh.
        ci.cancel();
    }
    @Inject(method="fetchDefaultServers",at=@At("HEAD"),cancellable=true)
    private static void backport$noDownload(CallbackInfoReturnable<CompletableFuture<?>> cir){cir.setReturnValue(CompletableFuture.completedFuture(null));}
}
