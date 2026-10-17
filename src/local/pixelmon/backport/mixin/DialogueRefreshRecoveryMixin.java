package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.client.gui.custom.dialogue.DialogueScreen;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(targets="com.pixelmonmod.pixelmon.comm.packetHandlers.dialogue.SetDialogueDataPacket",remap=false)
public abstract class DialogueRefreshRecoveryMixin {
    @Shadow private boolean openGui;
    @Inject(method="handlePacket",at=@At("TAIL"))
    private void backport$refreshCurrent(CallbackInfo ci){var screen=Minecraft.func_71410_x().field_71462_r;if(!openGui&&screen instanceof DialogueScreen)((DialogueScreen)screen).next();}
}
