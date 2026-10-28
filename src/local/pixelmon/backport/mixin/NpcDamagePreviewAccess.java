package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.battles.controller.ai.*;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(value=BattleAIBase.class,remap=false)
public interface NpcDamagePreviewAccess {
    @Invoker("getAttackChoices") java.util.List<MoveChoice> backport$legalChoices(PixelmonWrapper own);
    @Invoker("weightOffensiveMove") void backport$damage(PixelmonWrapper user,MoveChoice choice);
}
