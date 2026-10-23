package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.api.util.helpers.AIHelper;
import com.pixelmonmod.pixelmon.entities.pixelmon.AbstractInteractionsEntity;
import net.minecraft.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=AIHelper.class,remap=false)
public abstract class PokemonGoalRebuildMixin {
    @Inject(method="populateTasks",at=@At("HEAD"),cancellable=true)
    private void backport$afterGoalIteration(AbstractInteractionsEntity pokemon,GoalSelector selector,CallbackInfo ci){
        if(local.pixelmon.backport.DeferredPokemonAI.defer(selector,()->((AIHelper)(Object)this).populateTasks(pokemon,selector)))ci.cancel();
    }
}
