package local.pixelmon.backport.mixin;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Client presentation only; the optional quest mod still grants XP itself. */
@Pseudo @Mixin(targets="dev.ftb.mods.ftbquests.gui.RewardToast",remap=false)
public abstract class QuestExperienceToastIconMixin {
 @Shadow @Final private ITextComponent text;
 @Unique private static Object backport$experienceBottle;
 @Inject(method="getIcon",at=@At("RETURN"),cancellable=true)
 private void backport$xpIcon(CallbackInfoReturnable<Object> ci){
  if(!(text instanceof TranslationTextComponent))return;
  String key=((TranslationTextComponent)text).func_150268_i();
  if(!key.equals("ftbquests.reward.ftbquests.xp")&&!key.equals("ftbquests.reward.ftbquests.xp_levels"))return;
  if(backport$experienceBottle==null)try{
   var bottle=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new ResourceLocation("minecraft:experience_bottle"));
   backport$experienceBottle=Class.forName("dev.ftb.mods.ftblibrary.icon.ItemIcon").getMethod("getItemIcon",ItemStack.class).invoke(null,new ItemStack(bottle));
  }catch(ReflectiveOperationException ex){throw new IllegalStateException("Cannot create optional quest XP icon",ex);}
  ci.setReturnValue(backport$experienceBottle);
 }
}
