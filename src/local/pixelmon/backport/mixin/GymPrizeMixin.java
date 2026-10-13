package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.entities.npcs.NPCTrainer;
import local.pixelmon.backport.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Replace the level-scaled base, before legitimate native prize-item bonuses. */
@Mixin(value=NPCTrainer.class,remap=false)
public abstract class GymPrizeMixin {
    @ModifyVariable(method="loseBattle",index=3,at=@At("STORE"),slice=@Slice(
        from=@At(value="FIELD",target="Lcom/pixelmonmod/pixelmon/entities/npcs/NPCTrainer;winMoney:I",ordinal=0),
        to=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/battles/controller/participants/PlayerParticipant;getPrizeMoneyMultiplier()I")))
    private int backport$leaderPrize(int amount){return GymRewardPolicy.base(GymCities.isLeader((NPCTrainer)(Object)this),amount);}
}
