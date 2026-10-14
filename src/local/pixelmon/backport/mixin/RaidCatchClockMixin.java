package local.pixelmon.backport.mixin;

import com.pixelmonmod.pixelmon.client.gui.raids.RaidCatchScreen;
import local.pixelmon.backport.client.RaidCatchClock;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(value=RaidCatchScreen.class,remap=false)
public abstract class RaidCatchClockMixin {
    @Shadow private int ticker;
    @Unique private final RaidCatchClock backport$catchClock=new RaidCatchClock();
    @Unique private long backport$catchMillis;
    @Inject(method="setShakes(IZ)V",at=@At(value="FIELD",target="Lcom/pixelmonmod/pixelmon/client/gui/raids/RaidCatchScreen;ticker:I",opcode=Opcodes.PUTFIELD),require=1,allow=1)
    private void backport$beginCatch(int shakes,boolean success,CallbackInfo ci){backport$catchClock.reset();backport$catchMillis=0;}
    @Inject(method="getMillis()J",at=@At("RETURN"),require=2,allow=2)
    private void backport$elapsed(CallbackInfoReturnable<Long> cir){backport$catchMillis=cir.getReturnValueJ();}
    @Redirect(method="func_230430_a_(Lcom/mojang/blaze3d/matrix/MatrixStack;IIF)V",at=@At(value="FIELD",target="Lcom/pixelmonmod/pixelmon/client/gui/raids/RaidCatchScreen;ticker:I",opcode=Opcodes.PUTFIELD,ordinal=0),require=1,allow=1)
    private void backport$resetShake(RaidCatchScreen screen,int value){ticker=value;backport$catchClock.reset();}
    @Redirect(method="func_230430_a_(Lcom/mojang/blaze3d/matrix/MatrixStack;IIF)V",at=@At(value="FIELD",target="Lcom/pixelmonmod/pixelmon/client/gui/raids/RaidCatchScreen;ticker:I",opcode=Opcodes.PUTFIELD,ordinal=1),require=1,allow=1)
    private void backport$advanceCatch(RaidCatchScreen screen,int ignored){ticker=backport$catchClock.advance(ticker,backport$catchMillis);}
}
