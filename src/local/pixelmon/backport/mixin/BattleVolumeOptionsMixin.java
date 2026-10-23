package local.pixelmon.backport.mixin;
import local.pixelmon.backport.BattleMusicVolume;
import net.minecraft.client.gui.screen.*;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=OptionsSoundsScreen.class,remap=false)
public abstract class BattleVolumeOptionsMixin extends Screen {
    protected BattleVolumeOptionsMixin(ITextComponent title){super(title);}
    @Inject(method="func_231160_c_",at=@At("RETURN"))
    private void backport$slider(CallbackInfo ci){func_230480_a_(new BattleMusicVolume.Slider(field_230708_k_/2+5,field_230709_l_/6-12+24*5));}
}
