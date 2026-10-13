package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.battles.attacks.*;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import local.pixelmon.backport.OriginMercy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
/** Reuse the native FalseSwipe clamp AFTER damage modifiers, only for ordinary wild targets. */
@Mixin(value=PixelmonWrapper.class,remap=false)
public abstract class OriginMercyMixin {
    @Redirect(method="doBattleDamage",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/battles/attacks/ImmutableAttack;hasEffect(Ljava/lang/Class;)Z"))
    private boolean backport$wildMercy(ImmutableAttack move,Class<? extends EffectBase> effect,PixelmonWrapper user,float damage,DamageTypeEnum type){
        return move.hasEffect(effect) || OriginMercy.applies(user,(PixelmonWrapper)(Object)this,move.getAttackName());
    }
}
