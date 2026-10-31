package local.pixelmon.backport.mixin;
import local.pixelmon.backport.LightningOath;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Native messages only recognize exactly 2/4; custom super-effective multipliers share their message. */
@Mixin(value=Attack.class,remap=false,priority=1100)
public abstract class OathEffectiveChatMixin {
 @Inject(method="sendEffectiveChat",at=@At("HEAD"),cancellable=true,require=1)
 private void backport$oathMessage(PixelmonWrapper user,PixelmonWrapper target,CallbackInfo ci){
  Attack attack=(Attack)(Object)this;if(user.bc==null||(!LightningOath.affects(user,target,attack)&&!local.pixelmon.backport.AuthorPiercing.affects(user,target,attack))||attack.getTypeEffectiveness(user,target)<=1)return;
  if(user.targets!=null&&user.targets.size()>1)user.bc.sendToAll("pixelmon.battletext.supereffectivetarget",target.getNickname());
  else user.bc.sendToAll("pixelmon.battletext.supereffective");
  ci.cancel();
 }
}
