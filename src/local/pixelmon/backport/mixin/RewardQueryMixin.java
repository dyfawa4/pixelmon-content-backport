package local.pixelmon.backport.mixin;
import com.google.common.collect.ImmutableList;
import com.pixelmonmod.pixelmon.api.events.DropEvent;
import com.pixelmonmod.pixelmon.entities.pixelmon.drops.*;
import local.pixelmon.backport.RewardSanitizer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=DropItemQueryList.class,remap=false)
public abstract class RewardQueryMixin {
    @Redirect(method={"register(Lcom/pixelmonmod/pixelmon/entities/pixelmon/AbstractHoldsItemsEntity;Ljava/util/ArrayList;Lnet/minecraft/entity/player/ServerPlayerEntity;)V","register(Lcom/pixelmonmod/pixelmon/entities/npcs/NPCTrainer;Ljava/util/ArrayList;Lnet/minecraft/entity/player/ServerPlayerEntity;)V"},at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/api/events/DropEvent;getDrops()Lcom/google/common/collect/ImmutableList;"),require=2)
    private static ImmutableList<DroppedItem> backport$sharedRewards(DropEvent event){return RewardSanitizer.query(event.getDrops());}
}
