package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import local.pixelmon.backport.EndlessHeldItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
/** Booster Energy uses a direct removal path, separately from ordinary consumeItem. */
@Mixin(targets="com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.ParadoxBoostAbility",remap=false)
public abstract class EndlessBoosterMixin {
 @Redirect(method="activateViaItem",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/battles/controller/participants/PixelmonWrapper;removeHeldItem()V"))
 private void backport$retain(PixelmonWrapper p){if(!EndlessHeldItems.trigger(p))p.removeHeldItem();}
 @Redirect(method="activateViaItem",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/battles/controller/participants/PixelmonWrapper;enableReturnHeldItem()V"))
 private void backport$noDuplicateReturn(PixelmonWrapper p){if(!(p.getHeldItem() instanceof EndlessHeldItems.Endless))p.enableReturnHeldItem();}
}
