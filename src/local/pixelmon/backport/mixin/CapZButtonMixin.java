package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.client.gui.battles.*;
import com.pixelmonmod.pixelmon.api.registries.PixelmonItems;
import local.pixelmon.backport.CapSpecies;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Native UI checks only whether a crystal is held; reject the incompatible ordinary Pikachu crystal. */
@Mixin(value=ClientBattleManager.class,remap=false)
public abstract class CapZButtonMixin {
 @Inject(method="canUseZMove(Lcom/pixelmonmod/pixelmon/client/gui/battles/PixelmonClientData;)Z",at=@At("HEAD"),cancellable=true)
 private void backport$ordinaryPikachuCrystal(PixelmonClientData pokemon,CallbackInfoReturnable<Boolean> cir){if(pokemon!=null&&pokemon.species!=null&&pokemon.species.getDex()==CapSpecies.DEX&&pokemon.heldItem==PixelmonItems.pikanium_z)cir.setReturnValue(false);}
}
