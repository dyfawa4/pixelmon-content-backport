package local.pixelmon.backport.mixin;
import java.util.Set;
import com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.Sharpness;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=Sharpness.class,remap=false)
public abstract class SharpnessMixin {
    @Shadow private Set<String> slicingMoves;
    @Inject(method="<init>",at=@At("RETURN"))
    private void backport$slicing(CallbackInfo ci){slicingMoves.add("Mighty Cleave");slicingMoves.add("Tachyon Cutter");}
}
