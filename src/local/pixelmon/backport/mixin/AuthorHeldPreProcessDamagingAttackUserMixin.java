package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.items.HeldItem;
import local.pixelmon.backport.AuthorHeldItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
/** Native effect implementations, including direct getHeldItem call sites. */
@Mixin(targets={"com.pixelmonmod.pixelmon.items.heldItems.LegendPlateItem","com.pixelmonmod.pixelmon.items.heldItems.MetalCoatItem","com.pixelmonmod.pixelmon.items.heldItems.IncensePowerIncreaseItem","com.pixelmonmod.pixelmon.items.heldItems.TimespaceOrbItem","com.pixelmonmod.pixelmon.items.heldItems.TypeEnhancingItems","com.pixelmonmod.pixelmon.items.heldItems.ExpertBeltItem","com.pixelmonmod.pixelmon.items.heldItems.LifeOrbItem","com.pixelmonmod.pixelmon.items.heldItems.MetronomeItem","com.pixelmonmod.pixelmon.items.heldItems.SoulDewItem","com.pixelmonmod.pixelmon.items.heldItems.GemsItem"},remap=false)
public abstract class AuthorHeldPreProcessDamagingAttackUserMixin {
    @Inject(method="preProcessDamagingAttackUser",at=@At("HEAD"),cancellable=true,require=1)
    private void backport$gate(PixelmonWrapper a0, PixelmonWrapper a1, Attack a2, double a3, CallbackInfoReturnable<Double> ci){if(AuthorHeldItems.blocked(a0,(HeldItem)(Object)this,"preProcessDamagingAttackUser" + ":" + a1.getPokemonUUID()))ci.setReturnValue(a3);}
}
