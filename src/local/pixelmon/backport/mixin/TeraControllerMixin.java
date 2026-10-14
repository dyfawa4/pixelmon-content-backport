package local.pixelmon.backport.mixin;
import local.pixelmon.backport.Tera;
import local.pixelmon.backport.TeraNetwork;
import local.pixelmon.backport.TeraRaids;
import local.pixelmon.backport.TeraRaidParticipant;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.participants.BattleParticipant;
import com.pixelmonmod.pixelmon.api.battles.BattleEndCause;
import com.pixelmonmod.pixelmon.api.battles.BattleResults;
import java.util.HashMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=BattleController.class,remap=false)
public abstract class TeraControllerMixin {
    @Inject(method="endTurn",at=@At("RETURN"))
    private void backport$ashWaterExpires(CallbackInfo ci){local.pixelmon.backport.AshWaterBoost.expire((BattleController)(Object)this);}
    @Inject(method="isEvolving",at=@At("HEAD"))
    private void backport$deadEvolution(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir){local.pixelmon.backport.BattleEvolutionGuard.recover((BattleController)(Object)this);}
    @Inject(method="update",at=@At("RETURN"))
    private void backport$refreshTypes(CallbackInfo ci){TeraNetwork.refresh((BattleController)(Object)this);}
    @Inject(method="update",at=@At("HEAD"))
    private void backport$raidClock(CallbackInfo ci){TeraRaidParticipant raid=TeraRaids.get((BattleController)(Object)this);if(raid!=null)raid.tick();}
    @Inject(method="isTeamDefeated",at=@At("HEAD"),cancellable=true)
    private void backport$waitForRevive(BattleParticipant p,CallbackInfoReturnable<Boolean> cir){TeraRaidParticipant raid=TeraRaids.get((BattleController)(Object)this);if(raid!=null && raid.team!=p.team && raid.pendingRevives())cir.setReturnValue(false);}
    @Inject(method="addSpectator",at=@At("RETURN"))
    private void backport$spectator(com.pixelmonmod.pixelmon.battles.controller.participants.Spectator spectator,CallbackInfo ci){TeraNetwork.sync((BattleController)(Object)this,false);}
    @Inject(method="endBattle(Lcom/pixelmonmod/pixelmon/api/battles/BattleEndCause;Ljava/util/HashMap;)Ljava/util/HashMap;",at=@At("RETURN"))
    private void backport$cleanup(BattleEndCause cause,HashMap<BattleParticipant,BattleResults> result,CallbackInfoReturnable<HashMap<BattleParticipant,BattleResults>> cir){Tera.end((BattleController)(Object)this);local.pixelmon.backport.GymChallenge.end((BattleController)(Object)this);local.pixelmon.backport.AshWaterBoost.end((BattleController)(Object)this);}
}
