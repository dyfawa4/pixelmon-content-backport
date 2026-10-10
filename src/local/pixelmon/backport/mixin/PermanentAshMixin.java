package local.pixelmon.backport.mixin;
import local.pixelmon.backport.PermanentAsh;
import com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.BattleBond;
import com.pixelmonmod.pixelmon.api.pokemon.species.Stats;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=BattleBond.class,remap=false)
public abstract class PermanentAshMixin {
    // Only reached after every native knockout/participant check succeeds.
    @ModifyArg(method="tookDamageUser",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/battles/controller/participants/PixelmonWrapper;setForm(Ljava/lang/String;)V"),index=0)
    private String backport$earnedAsh(String form){return "ash".equals(form)?PermanentAsh.FORM:form;}
    @Inject(method="tookDamageUser",at=@At("HEAD"),cancellable=true)
    private void backport$alreadyEarned(int damage,PixelmonWrapper user,PixelmonWrapper target,Attack attack,CallbackInfo ci){if(PermanentAsh.isPermanent(user))ci.cancel();}
    @Inject(method="applyEndOfBattleEffect",at=@At("HEAD"),cancellable=true)
    private void backport$keepAfterBattle(PixelmonWrapper user,CallbackInfo ci){if(PermanentAsh.isPermanent(user))ci.cancel();}
    @Inject(method="onSelfFaint",at=@At("HEAD"),cancellable=true)
    private void backport$keepAfterFaint(PixelmonWrapper user,PixelmonWrapper source,CallbackInfo ci){if(PermanentAsh.isPermanent(user))ci.cancel();}
    @Redirect(method="modifyPowerAndAccuracyUser",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/api/pokemon/species/Stats;isTemporary()Z"))
    private boolean backport$waterShurikenPower(Stats form){return form.isTemporary() || form.isForm(PermanentAsh.FORM);}
}
