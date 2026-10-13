package local.pixelmon.backport.mixin;
import local.pixelmon.backport.BattleMotion;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.util.MovementInput;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Neutralize only locomotion before sprint/flight/jump prediction, not keyboard/chat events. */
@Mixin(value=ClientPlayerEntity.class,remap=false)
public abstract class BattleMotionClientInputMixin {
    @Shadow public MovementInput field_71158_b;
    @Inject(method="func_70636_d",at=@At("HEAD"))
    private void backport$oldMotionInput(CallbackInfo ci){backport$neutralize();}
    @Inject(method="func_70636_d",at=@At(value="INVOKE",target="Lnet/minecraft/util/MovementInput;func_225607_a_(Z)V",shift=At.Shift.AFTER))
    private void backport$newMotionInput(CallbackInfo ci){backport$neutralize();}
    @Unique private void backport$neutralize(){
        var self=(ClientPlayerEntity)(Object)this;
        if(!BattleMotion.frozen(self) || field_71158_b==null)return;
        field_71158_b.field_78902_a=0;field_71158_b.field_192832_b=0;field_71158_b.field_78901_c=false;
        field_71158_b.field_187255_c=false;field_71158_b.field_187256_d=false;
        field_71158_b.field_187257_e=false;field_71158_b.field_187258_f=false;
        self.field_70702_br=0;self.field_191988_bg=0;
        self.func_70637_d(false);
        BattleMotion.stop(self);
    }
}
