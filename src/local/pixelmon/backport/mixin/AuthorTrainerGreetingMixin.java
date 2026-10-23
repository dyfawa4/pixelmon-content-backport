package local.pixelmon.backport.mixin;

import com.pixelmonmod.pixelmon.entities.npcs.NPCTrainer;
import local.pixelmon.backport.AuthorBossTeams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Applies to saved author trainers as well as new ones; ordinary trainer greetings are retained. */
@Mixin(value=NPCTrainer.class,remap=false)
public abstract class AuthorTrainerGreetingMixin {
    @Inject(method="getGreeting",at=@At("HEAD"),cancellable=true)
    private void backport$authorGreeting(String locale,CallbackInfoReturnable<String> cir){
        if(AuthorBossTeams.is((NPCTrainer)(Object)this))cir.setReturnValue("你太弱了，还没有资格挑战我！");
    }
}
