package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.api.pokemon.boss.DropSet;
import local.pixelmon.backport.*;
import net.minecraft.item.*;
import net.minecraft.util.text.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=DropSet.Drop.class,remap=false)
public abstract class RewardCommandIconMixin {
    @Shadow @Final private String name;
    @Inject(method="getDisplayItem",at=@At("RETURN"),cancellable=true)
    private void backport$commandIcon(CallbackInfoReturnable<ItemStack> ci){
        if(!PokeChestLoot.valid(ci.getReturnValue())&&RewardSanitizer.commands((DropSet.Drop)(Object)this)){
            ItemStack icon=new ItemStack(Items.field_151121_aF);
            icon.func_200302_a(name==null||name.isBlank()?new TranslationTextComponent("pixelmon_backport.reward.command"):new StringTextComponent(name));
            ci.setReturnValue(icon);
        }
    }
}
