package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.pokemon.ability.Ability;
import com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Repair the invalid null/placeholder left by r6's moveset replacement, including existing saves.
 * Valid abilities (including temporary battle abilities) are never changed. */
@Mixin(value=Pokemon.class,remap=false)
public abstract class DeltaRayquazaAbilityMixin {
 @Inject(method="getAbility",at=@At("RETURN"),cancellable=true)
 private void backport$repair(CallbackInfoReturnable<Ability> ci){
  var pokemon=(Pokemon)(Object)this;
  if((ci.getReturnValue()==null||ci.getReturnValue() instanceof ComingSoon)&&pokemon.getSpecies()!=null&&pokemon.getSpecies().getDex()==384){
   Ability ability=pokemon.getForm().isForm("mega")?new DeltaStream():new AirLock();pokemon.setAbility(ability);ci.setReturnValue(ability);
  }
 }
}
