package local.pixelmon.backport.mixin;
import com.google.common.collect.ImmutableList;
import com.pixelmonmod.pixelmon.api.events.DropEvent;
import com.pixelmonmod.pixelmon.entities.pixelmon.drops.*;
import local.pixelmon.backport.RewardSanitizer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=DropItemQueryList.class,remap=false)
public abstract class RewardQueryMixin {
    @Inject(method={"takeAllItems","dropAllItems"},at=@At("HEAD"))
    private static void backport$claimAllScope(net.minecraft.entity.player.ServerPlayerEntity player,CallbackInfo ci){local.pixelmon.backport.AuthorSettlement.claiming(player);}
    @Inject(method="takeItem",at=@At("HEAD"))
    private static void backport$claimOneScope(net.minecraft.entity.player.ServerPlayerEntity player,int id,CallbackInfo ci){local.pixelmon.backport.AuthorSettlement.claiming(player);}
    @Inject(method={"takeAllItems","dropAllItems"},at=@At("RETURN"))
    private static void backport$claimedAll(net.minecraft.entity.player.ServerPlayerEntity player,CallbackInfo ci){local.pixelmon.backport.AuthorSettlement.claimed(player,true);}
    @Inject(method="takeItem",at=@At("RETURN"))
    private static void backport$claimedOne(net.minecraft.entity.player.ServerPlayerEntity player,int id,CallbackInfo ci){local.pixelmon.backport.AuthorSettlement.claimed(player,false);}
    @Redirect(method={"register(Lcom/pixelmonmod/pixelmon/entities/pixelmon/AbstractHoldsItemsEntity;Ljava/util/ArrayList;Lnet/minecraft/entity/player/ServerPlayerEntity;)V","register(Lcom/pixelmonmod/pixelmon/entities/npcs/NPCTrainer;Ljava/util/ArrayList;Lnet/minecraft/entity/player/ServerPlayerEntity;)V"},at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/api/events/DropEvent;getDrops()Lcom/google/common/collect/ImmutableList;"),require=2)
    private static ImmutableList<DroppedItem> backport$sharedRewards(DropEvent event){return RewardSanitizer.query(event.getDrops());}
}
