package local.pixelmon.backport.mixin;
import local.pixelmon.backport.NinjaTraining;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.battles.attack.AttackRegistry;
import com.pixelmonmod.pixelmon.battles.attacks.ImmutableAttack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=Pokemon.class,remap=false)
public abstract class NinjaReminderMixin {
    @Inject(method="getMoveset",at=@At("RETURN"))
    private void backport$ashMovePP(CallbackInfoReturnable<com.pixelmonmod.pixelmon.api.pokemon.stats.Moveset> cir){local.pixelmon.backport.AshShurikenPP.apply(cir.getReturnValue(),(Pokemon)(Object)this);}
    @Inject(method="getRelearnableMoves",at=@At("RETURN"),cancellable=true)
    private void backport$trainingReminder(CallbackInfoReturnable<java.util.List<ImmutableAttack>> cir){
        if(!NinjaTraining.eligible((Pokemon)(Object)this))return;
        AttackRegistry.getAttackBase(NinjaTraining.MOVE).ifPresent(move->{var rows=new java.util.ArrayList<>(cir.getReturnValue());if(!rows.contains(move))rows.add(move);cir.setReturnValue(rows);});
    }
}
