package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.api.pokemon.ability.Ability;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(value=Attack.class,remap=false)
public interface AuthorAccuracyAccess {
    @Invoker("calculateMoveAccuracy") double backport$liveAccuracy(PixelmonWrapper user,PixelmonWrapper target,Ability ability);
    @Invoker("modifyMoveStats") int[] backport$modifiedStats(PixelmonWrapper user,PixelmonWrapper target,Ability userAbility,Ability targetAbility,int power);
}
