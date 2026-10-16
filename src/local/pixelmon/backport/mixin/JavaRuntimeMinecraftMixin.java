package local.pixelmon.backport.mixin;

import local.pixelmon.backport.client.JavaRuntimeGate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=Minecraft.class,remap=false)
public abstract class JavaRuntimeMinecraftMixin {
    @ModifyVariable(method="func_147108_a",at=@At("HEAD"),argsOnly=true)
    private Screen backport$java11Screen(Screen requested){return JavaRuntimeGate.filter(requested);}
    @Inject(method={"func_238191_a_","func_238192_a_"},at=@At("HEAD"),cancellable=true)
    private void backport$java11World(CallbackInfo ci){if(JavaRuntimeGate.blockEntry())ci.cancel();}
}
