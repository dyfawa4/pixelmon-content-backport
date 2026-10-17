package local.pixelmon.backport.mixin;
import java.util.ArrayList;
import com.pixelmonmod.pixelmon.api.events.raids.RaidDropsEvent;
import com.pixelmonmod.pixelmon.battles.raids.RaidData;
import local.pixelmon.backport.RewardSanitizer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=RaidData.RaidPlayer.class,remap=false)
public abstract class RewardRaidMixin {
    @Redirect(method="winRaid",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/api/events/raids/RaidDropsEvent;getDrops()Ljava/util/ArrayList;"))
    private ArrayList<ItemStack> backport$sharedRaidRewards(RaidDropsEvent event){return new ArrayList<>(RewardSanitizer.stacks(event.getDrops()));}
}
