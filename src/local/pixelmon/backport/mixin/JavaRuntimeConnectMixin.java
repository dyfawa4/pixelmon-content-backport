package local.pixelmon.backport.mixin;

import local.pixelmon.backport.client.JavaRuntimeGate;
import net.minecraft.client.gui.screen.ConnectingScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=ConnectingScreen.class,remap=false)
public abstract class JavaRuntimeConnectMixin {
    @Inject(method="func_146367_a",at=@At("HEAD"),cancellable=true)
    private void backport$java11Connect(CallbackInfo ci){if(JavaRuntimeGate.blockEntry())ci.cancel();}
}
