package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.battles.attacks.*;
import com.pixelmonmod.pixelmon.battles.controller.log.MoveResults;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.api.util.helpers.RandomHelper;
import com.pixelmonmod.pixelmon.api.pokemon.ability.Ability;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.*;
import local.pixelmon.backport.AuthorForesight;
@Mixin(value=Attack.class,remap=false)
public abstract class AuthorHitRollMixin {
    @Redirect(method="use(Lcom/pixelmonmod/pixelmon/battles/controller/participants/PixelmonWrapper;Lcom/pixelmonmod/pixelmon/battles/controller/participants/PixelmonWrapper;Lcom/pixelmonmod/pixelmon/battles/controller/log/MoveResults;Lcom/pixelmonmod/pixelmon/battles/attacks/ZMove;)Z",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/api/util/helpers/RandomHelper;getRandomChance(I)Z"),require=1)
    private boolean backport$sharedRoll(int percent,PixelmonWrapper user,PixelmonWrapper target,MoveResults result,ZMove z){
        if(AuthorForesight.forecast(user)&&!user.usingZ){if(user.bc.simulateMode)return !AuthorForesight.visible(user)||percent>0;return AuthorForesight.accuracy(user,target,percent);}
        return RandomHelper.getRandomChance(percent);
    }
}
