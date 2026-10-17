package local.pixelmon.backport.mixin;
import local.pixelmon.backport.client.DialogueRecovery;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(targets="com.pixelmonmod.pixelmon.comm.packetHandlers.dialogue.DialogueNextActionPacket",remap=false)
public abstract class DialogueReplyRecoveryMixin {
    @Inject(method="handlePacket",at=@At("TAIL"))
    private void backport$releaseChoice(CallbackInfo ci){var screen=Minecraft.func_71410_x().field_71462_r;if(screen instanceof DialogueRecovery)((DialogueRecovery)screen).backport$reply();}
}
