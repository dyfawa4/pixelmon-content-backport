package local.pixelmon.backport.mixin;

import com.pixelmonmod.pixelmon.api.pokemon.boss.BossTier;
import com.pixelmonmod.pixelmon.comm.ChatHandler;
import com.pixelmonmod.pixelmon.entities.pokeballs.EmptyPokeBallEntity;
import com.pixelmonmod.pixelmon.entities.pokeballs.PokeBallEntity;
import local.pixelmon.backport.CapBallGuard;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Reuse the original Boss rejection/drop/removal block; never add a second refund. */
@Mixin(value=EmptyPokeBallEntity.class,remap=false)
public abstract class CapFieldBallMixin extends PokeBallEntity {
    protected CapFieldBallMixin(EntityType<? extends PokeBallEntity> type,World world){super(type,world);}
    @Unique private boolean backport$forbiddenCap;
    @Unique private boolean backport$returnBall;
    @Inject(method="func_70227_a",at=@At("HEAD"))
    private void backport$reset(RayTraceResult hit,CallbackInfo ci){backport$forbiddenCap=false;backport$returnBall=dropItem;}
    @Redirect(method="func_70227_a",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/api/pokemon/boss/BossTier;isBoss()Z"))
    private boolean backport$rejectLikeBoss(BossTier tier){
        if(tier.isBoss())return true;
        backport$forbiddenCap=CapBallGuard.fieldBlocked(getBallType(),pixelmon);
        // Native impact clears dropItem before reaching its Boss branch. Restore
        // only the eligibility this rejected physical projectile had on entry.
        if(backport$forbiddenCap)dropItem=backport$returnBall;
        return backport$forbiddenCap;
    }
    @Redirect(method="func_70227_a",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/comm/ChatHandler;sendChat(Lnet/minecraft/entity/Entity;Ljava/lang/String;[Ljava/lang/Object;)V"))
    private void backport$reason(Entity recipient,String key,Object[] arguments){
        ChatHandler.sendChat(recipient,backport$forbiddenCap && "pixelmon.pokeballs.boss".equals(key)?CapBallGuard.MESSAGE:key,arguments);
    }
}
