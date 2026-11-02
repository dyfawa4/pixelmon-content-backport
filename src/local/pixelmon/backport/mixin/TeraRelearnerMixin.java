package local.pixelmon.backport.mixin;
import java.util.List;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.battles.attacks.ImmutableAttack;
import com.pixelmonmod.pixelmon.entities.npcs.NPCRelearner;
import local.pixelmon.backport.TeraMachineLearning;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=NPCRelearner.class,remap=false)
public abstract class TeraRelearnerMixin {
    @Inject(method="getRelearnableMoves",at=@At("RETURN"),cancellable=true)
    private static void backport$machineOnly(Pokemon pokemon,CallbackInfoReturnable<List<ImmutableAttack>> cir){var moves=TeraMachineLearning.reminders(cir.getReturnValue());
        moves.removeIf(move->local.pixelmon.backport.NinjaTraining.MOVE.equals(move.getAttackName()));
        if(local.pixelmon.backport.NinjaTraining.eligible(pokemon)&&!pokemon.getMoveset().stream().anyMatch(local.pixelmon.backport.NinjaTraining::move))
            com.pixelmonmod.pixelmon.api.battles.attack.AttackRegistry.getAttackBase(local.pixelmon.backport.NinjaTraining.MOVE).ifPresent(moves::add);
        cir.setReturnValue(moves);}
}
