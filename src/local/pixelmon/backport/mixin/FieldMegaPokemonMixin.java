package local.pixelmon.backport.mixin;
import local.pixelmon.backport.FieldMega;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import net.minecraft.nbt.CompoundNBT;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=Pokemon.class,remap=false)
public abstract class FieldMegaPokemonMixin {
    @Inject(method="writeToNBT",at=@At("RETURN"))
    private void backport$saveField(CompoundNBT nbt,CallbackInfoReturnable<CompoundNBT> ci){FieldMega.saved((Pokemon)(Object)this,ci.getReturnValue());}
    @Inject(method="readFromNBT",at=@At("RETURN"))
    private void backport$loadField(CompoundNBT nbt,CallbackInfo ci){FieldMega.restore((Pokemon)(Object)this);local.pixelmon.backport.CapPikachu.sync((Pokemon)(Object)this);}
}
