package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.api.pokemon.boss.DropSet;
import com.pixelmonmod.pixelmon.api.util.WeightedSet;
import local.pixelmon.backport.RewardSanitizer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=DropSet.class,remap=false)
public abstract class RewardBossPoolMixin {
    @Shadow @Final @Mutable private WeightedSet<DropSet.Drop> drops;
    @Inject(method={"<init>(DILcom/pixelmonmod/pixelmon/api/util/WeightedSet;)V","<init>(Lcom/google/gson/JsonObject;Lcom/google/gson/JsonDeserializationContext;)V","<init>(Lnet/minecraft/network/PacketBuffer;)V"},at=@At("RETURN"),require=3)
    private void backport$loaded(CallbackInfo ci){drops=RewardSanitizer.pool(drops);}
    @Inject(method="getDrops",at=@At("HEAD"))
    private void backport$beforeRoll(CallbackInfoReturnable<java.util.Set<DropSet.Drop>> ci){drops=RewardSanitizer.pool(drops);}
}
