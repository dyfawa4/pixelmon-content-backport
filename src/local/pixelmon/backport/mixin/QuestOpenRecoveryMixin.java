package local.pixelmon.backport.mixin;
import local.pixelmon.backport.client.QuestRecoveryClient;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets="dev.ftb.mods.ftbquests.client.FTBQuestsClient",remap=false)
public abstract class QuestOpenRecoveryMixin {
    @Inject(method="openGui",at=@At("HEAD"),cancellable=true)
    private void backport$resyncMissingFile(CallbackInfo ci){if(QuestRecoveryClient.request())ci.cancel();}
}
