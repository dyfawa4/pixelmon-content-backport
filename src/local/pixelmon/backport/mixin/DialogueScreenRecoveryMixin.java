package local.pixelmon.backport.mixin;
import local.pixelmon.backport.client.DialogueRecovery;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(targets="com.pixelmonmod.pixelmon.client.gui.custom.dialogue.DialogueScreen",remap=false)
public abstract class DialogueScreenRecoveryMixin implements DialogueRecovery {
    @Shadow private boolean pause;
    @Unique private long backport$waiting;
    @Override public void backport$reply(){pause=false;backport$waiting=0;}
    @Inject(method="next",at=@At("HEAD"))
    private void backport$next(CallbackInfo ci){backport$reply();}
    @Inject(method="func_231044_a_",at=@At("TAIL"))
    private void backport$clicked(double x,double y,int button,CallbackInfoReturnable<Boolean> ci){if(pause&&backport$waiting==0)backport$waiting=System.nanoTime();}
    @Inject(method="func_231178_ax__",at=@At("RETURN"),cancellable=true)
    private void backport$escapeAfterLostReply(CallbackInfoReturnable<Boolean> ci){
        // Let the user leave a stalled request; never resend it or advance a quest locally.
        if(pause&&backport$waiting!=0&&System.nanoTime()-backport$waiting>8_000_000_000L)ci.setReturnValue(true);
    }
}
