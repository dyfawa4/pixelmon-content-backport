package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.api.daycare.impl.requirement.AbstractRequirement;
import com.pixelmonmod.pixelmon.api.daycare.impl.requirement.ItemStackRequirement;
import net.minecraft.util.text.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Also repairs old serialized display text, using the exact native stacks actually consumed. */
@Mixin(value=AbstractRequirement.class,remap=false)
public abstract class DayCareRequirementTextMixin {
    @Inject(method="getError",at=@At("RETURN"),cancellable=true)
    private void backport$actualMaterials(CallbackInfoReturnable<ITextComponent> cir){
        if(!((Object)this instanceof ItemStackRequirement))return;
        ItemStackRequirement requirement=(ItemStackRequirement)(Object)this;
        if(!"itemstack".equals(requirement.getTypeId())||requirement.getRequiredItems()==null||requirement.getRequiredItems().isEmpty())return;
        IFormattableTextComponent result=new StringTextComponent("");boolean first=true;
        for(var stack:requirement.getRequiredItems()){
            if(stack==null||stack.func_190926_b())return;
            if(!first)result.func_240702_b_(", ");first=false;
            result.func_240702_b_(stack.func_190916_E()+"x ").func_230529_a_(stack.func_200301_q());
        }
        cir.setReturnValue(result);
    }
}
