package local.pixelmon.backport.mixin;
import local.pixelmon.backport.NpcLocalization;
import com.pixelmonmod.pixelmon.entities.npcs.NPCTrainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=NPCTrainer.class,remap=false)
public abstract class NpcTrainerLocaleMixin {
    @Inject(method={"getName","getGreeting","getWinMessage","getLoseMessage"},at=@At("RETURN"),cancellable=true)
    private void backport$trainerText(String locale,CallbackInfoReturnable<String> ci){ci.setReturnValue(NpcLocalization.translate(ci.getReturnValue(),locale));}
}
