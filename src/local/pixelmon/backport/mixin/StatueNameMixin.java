package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.entities.pixelmon.AbstractBaseEntity;
import com.pixelmonmod.pixelmon.entities.pixelmon.StatueEntity;
import com.pixelmonmod.pixelmon.api.pokemon.species.Species;
import net.minecraft.util.text.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=AbstractBaseEntity.class,remap=false)
public abstract class StatueNameMixin {
    @Shadow public abstract Species getSpecies();
    @Inject(method="func_200200_C_",at=@At("HEAD"),cancellable=true)
    private void backport$statueName(CallbackInfoReturnable<ITextComponent> ci){
        if((Object)this instanceof StatueEntity){
            Species species=getSpecies();
            ci.setReturnValue(new TranslationTextComponent(species==null?"entity.pixelmon_backport.statue":species.getTranslationKey()));
        }
    }
}
