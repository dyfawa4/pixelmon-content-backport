package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.api.registries.PixelmonForms;
import com.pixelmonmod.pixelmon.api.pokemon.Element;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Signature-specific Z crystals have no affiliated element. The legacy item
 * event passed that null into Arceus' form switch and crashed. */
@Mixin(value=PixelmonForms.class,remap=false)
public abstract class FormTypeMixin {
    @Inject(method="getFromType",at=@At("HEAD"),cancellable=true)
    private static void backport$noElement(Element type,CallbackInfoReturnable<String> cir){if(type==null)cir.setReturnValue(PixelmonForms.NONE);}
}
