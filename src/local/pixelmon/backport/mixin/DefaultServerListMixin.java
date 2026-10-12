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
    private static void backport$noAds(Runnable complete,CallbackInfo ci){if(complete!=null)complete.run();ci.cancel();}
    @Inject(method="fetchDefaultServers",at=@At("HEAD"),cancellable=true)
    private static void backport$noDownload(CallbackInfoReturnable<CompletableFuture<?>> cir){cir.setReturnValue(CompletableFuture.completedFuture(null));}
}
