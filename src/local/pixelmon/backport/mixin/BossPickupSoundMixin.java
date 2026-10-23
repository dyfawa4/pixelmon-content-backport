package local.pixelmon.backport.mixin;
import java.util.ArrayList;
import com.pixelmonmod.pixelmon.entities.pixelmon.AbstractHoldsItemsEntity;
import com.pixelmonmod.pixelmon.entities.pixelmon.drops.*;
import local.pixelmon.backport.BossPickupSound;
import net.minecraft.entity.player.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=DropItemQueryList.class,remap=false)
public abstract class BossPickupSoundMixin {
 @Inject(method="register(Lcom/pixelmonmod/pixelmon/entities/pixelmon/AbstractHoldsItemsEntity;Ljava/util/ArrayList;Lnet/minecraft/entity/player/ServerPlayerEntity;)V",at=@At("RETURN"))
 private static void backport$bossQuery(AbstractHoldsItemsEntity source,ArrayList<DroppedItem> drops,ServerPlayerEntity player,CallbackInfo ci){BossPickupSound.register(player,source.getBossTier().isBoss());}
 @Inject(method="takeAllItems",at=@At("HEAD"))
 private static void backport$before(ServerPlayerEntity player,CallbackInfo ci){BossPickupSound.before(player);}
 @Inject(method="takeAllItems",at=@At("RETURN"))
 private static void backport$after(ServerPlayerEntity player,CallbackInfo ci){BossPickupSound.after(player);}
 @Inject(method="takeItem",at=@At("HEAD"))
 private static void backport$beforeOne(ServerPlayerEntity player,int id,CallbackInfo ci){BossPickupSound.before(player);}
 @Inject(method="takeItem",at=@At("RETURN"))
 private static void backport$afterOne(ServerPlayerEntity player,int id,CallbackInfo ci){BossPickupSound.after(player);}
 @Inject(method="removeQuery",at=@At("RETURN"))
 private static void backport$forget(ServerPlayerEntity player,CallbackInfo ci){BossPickupSound.remove(player);}
}
