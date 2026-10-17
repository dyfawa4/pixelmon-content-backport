package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.quests.QuestProgress;
import com.pixelmonmod.pixelmon.quests.objectives.Objective;
import com.pixelmonmod.pixelmon.quests.quest.*;
import com.pixelmonmod.pixelmon.storage.playerData.QuestData;
import local.pixelmon.backport.QuestStructures;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(targets="com.pixelmonmod.pixelmon.quests.objectives.objectives.world.StructureObjective",remap=false)
public abstract class NpcQuestStructureMixin {
    @Inject(method="test",at=@At("RETURN"),cancellable=true)
    private void backport$structure(Stage s,QuestData d,QuestProgress p,Objective o,Arguments a,Context c,CallbackInfoReturnable<Boolean> cir) {
        if(cir.getReturnValue() || d.getPlayer()==null)return;
        String requested=a.value(0,p);
        ResourceLocation id=ResourceLocation.func_208304_a(requested);
        if(id!=null && QuestStructures.inside(d.getPlayer().func_71121_q(),d.getPlayer().func_233580_cy_(),id))cir.setReturnValue(true);
    }
}
