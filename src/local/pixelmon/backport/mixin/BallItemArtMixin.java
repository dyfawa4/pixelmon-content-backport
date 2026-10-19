package local.pixelmon.backport.mixin;
import local.pixelmon.backport.BallItemArt;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(targets="com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.PokeBall",remap=false)
public abstract class BallItemArtMixin {
    @Inject(method={"getBallSprite","getLidSprite","getGUISprite"},at=@At("RETURN"),cancellable=true)
    private void backport$referenceArt(CallbackInfoReturnable<ResourceLocation> cir){cir.setReturnValue(BallItemArt.sprite(cir.getReturnValue()));}
}
