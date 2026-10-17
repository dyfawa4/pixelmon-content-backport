package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.quests.QuestProgress;
import com.pixelmonmod.pixelmon.quests.objectives.Objective;
import com.pixelmonmod.pixelmon.quests.quest.*;
import com.pixelmonmod.pixelmon.storage.playerData.QuestData;
import local.pixelmon.backport.*;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets="com.pixelmonmod.pixelmon.quests.objectives.objectives.player.ItemObjective", remap=false)
public abstract class NpcQuestItemMixin {
    @Shadow public abstract String identifier();
    @Inject(method="compare", at=@At("HEAD"), cancellable=true)
    private static void backport$match(ItemStack stack, Item item, int count, int damage, String name, CompoundNBT nbt, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(NpcQuestItems.matches(stack,item,damage,name,nbt) && stack.func_190916_E() >= Math.max(1,count));
    }
    @Inject(method="test", at=@At("HEAD"), cancellable=true)
    private void backport$test(Stage s, QuestData d, QuestProgress p, Objective o, Arguments a, Context c, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(NpcQuestLogic.itemTest(identifier(),a,p,c));
    }
    @Inject(method="quantity", at=@At("RETURN"), cancellable=true)
    private void backport$positive(Stage s, QuestData d, QuestProgress p, Objective o, Arguments a, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(Math.max(1,cir.getReturnValue()));
    }
}
