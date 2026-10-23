package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=MobEntity.class,remap=false)
public abstract class PokemonGoalTickMixin {
    @Redirect(method="func_70626_be",at=@At(value="INVOKE",target="Lnet/minecraft/entity/ai/goal/GoalSelector;func_75774_a()V"))
    private void backport$safeGoals(GoalSelector selector){
        if((Object)this instanceof PixelmonEntity)local.pixelmon.backport.DeferredPokemonAI.tick(selector);
        else selector.func_75774_a();
    }
}
