package local.pixelmon.backport.mixin;
import local.pixelmon.backport.FieldMega;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import net.minecraft.nbt.CompoundNBT;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=Pokemon.class,remap=false)
public abstract class FieldMegaPokemonMixin {
    @ModifyVariable(method="setGender(Lcom/pixelmonmod/pixelmon/api/pokemon/species/gender/Gender;)V",at=@At("HEAD"),argsOnly=true)
    private com.pixelmonmod.pixelmon.api.pokemon.species.gender.Gender backport$capMale(com.pixelmonmod.pixelmon.api.pokemon.species.gender.Gender gender){
        Pokemon pokemon=(Pokemon)(Object)this;
        return pokemon.getSpecies()!=null && pokemon.getSpecies().getDex()==local.pixelmon.backport.CapSpecies.DEX
            ?com.pixelmonmod.pixelmon.api.pokemon.species.gender.Gender.MALE:gender;
    }
    @Inject(method="setStorage",at=@At("RETURN"))
    private void backport$acquired(CallbackInfo ci){local.pixelmon.backport.AcquisitionQueue.stored((Pokemon)(Object)this);}
    @Inject(method="setSpecies(Lcom/pixelmonmod/pixelmon/api/pokemon/species/Species;Z)V",at=@At("RETURN"))
    private void backport$speciesAcquired(CallbackInfo ci){local.pixelmon.backport.AcquisitionQueue.stored((Pokemon)(Object)this);}
    @Inject(method="setEggCycles",at=@At("RETURN"))
    private void backport$hatched(CallbackInfo ci){local.pixelmon.backport.AcquisitionQueue.stored((Pokemon)(Object)this);}
    @Inject(method="writeToNBT",at=@At("RETURN"))
    private void backport$saveField(CompoundNBT nbt,CallbackInfoReturnable<CompoundNBT> ci){FieldMega.saved((Pokemon)(Object)this,ci.getReturnValue());}
    @Inject(method="readFromNBT",at=@At("RETURN"))
    private void backport$loadField(CompoundNBT nbt,CallbackInfo ci){FieldMega.restore((Pokemon)(Object)this);local.pixelmon.backport.CapPikachu.sync((Pokemon)(Object)this);}
}
