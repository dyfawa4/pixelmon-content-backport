package local.pixelmon.backport.mixin;

import com.pixelmonmod.pixelmon.battles.tasks.BattleMessagePacket;
import local.pixelmon.backport.client.AshMoveDisplay;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=BattleMessagePacket.class,remap=false)
public abstract class AshBattleMessageMixin {
    @Inject(method="getMessage",at=@At("RETURN"),cancellable=true)
    private void backport$ashMessages(CallbackInfoReturnable<ITextComponent> cir){
        cir.setReturnValue(AshMoveDisplay.battleMessage(cir.getReturnValue(),Minecraft.func_71410_x().field_71474_y.field_74363_ab));
    }
}
