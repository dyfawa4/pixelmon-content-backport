package local.pixelmon.backport.mixin;
import net.minecraft.client.audio.*;
import net.minecraft.util.SoundCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=LocatableSound.class,remap=false)
public abstract class PokemonMusicCategoryMixin {
    @Inject(method="func_184365_d",at=@At("RETURN"),cancellable=true)
    private void backport$musicCategory(CallbackInfoReturnable<SoundCategory> cir){
        if(local.pixelmon.backport.PokemonMusic.isMusic((ISound)this))cir.setReturnValue(SoundCategory.MASTER);
    }
}
