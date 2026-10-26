package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.items.HeldItem;
import local.pixelmon.backport.AuthorHeldItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
/** Native effect implementations, including direct getHeldItem call sites. */
@Mixin(targets={"com.pixelmonmod.pixelmon.items.heldItems.BerryEnigmaItem","com.pixelmonmod.pixelmon.items.heldItems.BerryStatIncreaseItem","com.pixelmonmod.pixelmon.items.heldItems.BerryJuiceItem","com.pixelmonmod.pixelmon.items.heldItems.BerryRestoreHPItem"},remap=false)
public abstract class AuthorHeldTookDamageMixin {
    @Inject(method="tookDamage",at=@At("HEAD"),cancellable=true,require=1)
    private void backport$gate(PixelmonWrapper a0, PixelmonWrapper a1, float a2, com.pixelmonmod.pixelmon.battles.attacks.DamageTypeEnum a3, CallbackInfo ci){if(AuthorHeldItems.blocked(a1,(HeldItem)(Object)this,"tookDamage" + ":" + a0.getPokemonUUID()))ci.cancel();}
}
