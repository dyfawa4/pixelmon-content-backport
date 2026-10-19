package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.client.keybindings.OptionsKey;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=OptionsKey.class,remap=false)
public abstract class PixelmonOptionsMixin {
    @Inject(method="keyDown",at=@At("HEAD"))
    private void backport$options(net.minecraftforge.client.event.InputEvent.KeyInputEvent e,CallbackInfo ci){
        Minecraft mc=Minecraft.func_71410_x();
        if(mc.field_71439_g!=null&&mc.field_71462_r==null&&((KeyBinding)(Object)this).func_151468_f())mc.func_147108_a(new local.pixelmon.backport.BackportConfigScreen(null));
    }
}
