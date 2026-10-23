package local.pixelmon.backport.mixin;
import java.util.Set;
import com.pixelmonmod.pixelmon.api.pokemon.boss.*;
import com.pixelmonmod.pixelmon.entities.pixelmon.AbstractHoldsItemsEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=BossTier.class,remap=false)
public abstract class DrownedBossDropsMixin {
 @Inject(method="getDrops",at=@At("HEAD"),cancellable=true,require=1)
 private void backport$drownedPrizes(ServerPlayerEntity player,AbstractHoldsItemsEntity entity,CallbackInfoReturnable<Set<DropSet.Drop>> cir){if("drowned".equals(((BossTier)(Object)this).getID())&&entity!=null)cir.setReturnValue(local.pixelmon.backport.DrownedBosses.rewards(player,entity));}
 @Inject(method="getDrops",at=@At("RETURN"),cancellable=true)
 private void backport$authorOnlyStone(ServerPlayerEntity player,AbstractHoldsItemsEntity entity,CallbackInfoReturnable<Set<DropSet.Drop>> cir){var drops=cir.getReturnValue();if(drops!=null){var clean=new java.util.LinkedHashSet<>(drops);clean.removeIf(drop->{var stack=((RewardDropAccess)(Object)drop).backport$item();return stack!=null&&local.pixelmon.backport.DrownedBosses.exclusive(stack.func_77973_b());});cir.setReturnValue(clean);}}
}
