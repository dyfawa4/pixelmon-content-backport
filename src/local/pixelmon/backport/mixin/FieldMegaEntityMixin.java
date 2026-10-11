package local.pixelmon.backport.mixin;
import local.pixelmon.backport.FieldMega;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=PixelmonEntity.class,remap=false)
public abstract class FieldMegaEntityMixin {
    @Inject(method={"retrieve","func_241204_bJ_"},at=@At("HEAD"))
    private void backport$recallField(CallbackInfo ci){FieldMega.restore(((PixelmonEntity)(Object)this).getPokemon());}
}
