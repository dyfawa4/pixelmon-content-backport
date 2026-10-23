package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.battles.attacks.*;
import local.pixelmon.backport.AuthorChallengeClient;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(targets="com.pixelmonmod.pixelmon.client.gui.battles.battleScreens.ChooseAttack$MoveButton",remap=false)
public abstract class AuthorAccuracyDisplayMixin {
 @Shadow private Attack attack;
 @Redirect(method="func_230431_b_",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/battles/attacks/ImmutableAttack;getAccuracy()I"))
 private int backport$accuracy(ImmutableAttack move){return AuthorChallengeClient.accuracy(attack,move.getAccuracy());}
 @Redirect(method="func_230431_b_",at=@At(value="INVOKE",target="Lnet/minecraft/client/resources/I18n;func_135052_a(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;"))
 private String backport$successLabel(String key,Object[] args){return AuthorChallengeClient.label(attack,key,args);}
}
