package local.pixelmon.backport.mixin;
import java.util.*;
import com.pixelmonmod.pixelmon.entities.pixelmon.AbstractHoldsItemsEntity;
import com.pixelmonmod.pixelmon.entities.pixelmon.drops.*;
import com.pixelmonmod.pixelmon.comm.packetHandlers.itemDrops.*;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.TranslationTextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=DropItemQueryList.class,remap=false)
public abstract class DrownedRewardTitleMixin {
 @Redirect(method="register(Lcom/pixelmonmod/pixelmon/entities/pixelmon/AbstractHoldsItemsEntity;Ljava/util/ArrayList;Lnet/minecraft/entity/player/ServerPlayerEntity;)V",at=@At(value="NEW",target="com/pixelmonmod/pixelmon/comm/packetHandlers/itemDrops/ItemDropPacket",ordinal=0),require=1)
 private static ItemDropPacket backport$drownedTitle(ItemDropMode mode,List<DroppedItem> drops,AbstractHoldsItemsEntity source,ArrayList<DroppedItem> original,ServerPlayerEntity player){
  return local.pixelmon.backport.DrownedBosses.is(source)?new ItemDropPacket(mode,new TranslationTextComponent("pixelmon.backport.drowned.rewards.title"),drops):new ItemDropPacket(mode,drops);
 }
}
