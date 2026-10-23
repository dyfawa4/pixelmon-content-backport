package local.pixelmon.backport.mixin;
import java.util.Map;
import com.google.gson.JsonElement;
import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.resources.IResourceManager;
import net.minecraft.profiler.IProfiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(RecipeManager.class)
public abstract class RecolorRecipeMixin {
 @Inject(method="func_212853_a_(Ljava/util/Map;Lnet/minecraft/resources/IResourceManager;Lnet/minecraft/profiler/IProfiler;)V",at=@At("HEAD"),require=1)
 private void backport$removeRecolors(Map<ResourceLocation,JsonElement> recipes,IResourceManager resources,IProfiler profiler,CallbackInfo ci){
  local.pixelmon.backport.RecolorRecipes.remove(recipes);
  recipes.remove(new ResourceLocation("pixelmon:pokeball/ball/park_ball"));
  recipes.remove(new ResourceLocation("pixelmon:machines/computer/pc"));
 }
}
