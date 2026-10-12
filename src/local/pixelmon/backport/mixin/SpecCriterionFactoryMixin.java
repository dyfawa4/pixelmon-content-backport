package local.pixelmon.backport.mixin;
import com.google.gson.JsonObject;
import com.pixelmonmod.pixelmon.api.advancements.triggers.SpecTrigger;
import local.pixelmon.backport.SpeciesCriterionGuard;
import net.minecraft.loot.ConditionArrayParser;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=SpecTrigger.class,remap=false)
public abstract class SpecCriterionFactoryMixin {
    @Inject(method="createInstance",at=@At("RETURN"))
    private void backport$species(JsonObject json,ConditionArrayParser parser,CallbackInfoReturnable<SpecTrigger.Instance> cir){
        if(json.has("backport_expected_dex"))((SpeciesCriterionGuard)cir.getReturnValue()).backport$expectedDex(json.get("backport_expected_dex").getAsInt());
    }
}
