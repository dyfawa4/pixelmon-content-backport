package local.pixelmon.backport.mixin;
import java.util.Map;
import com.google.gson.JsonElement;
import net.minecraft.loot.LootTableManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.resources.IResourceManager;
import net.minecraft.profiler.IProfiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LootTableManager.class)
public abstract class MusicLootMixin {
 @Inject(method="func_212853_a_(Ljava/util/Map;Lnet/minecraft/resources/IResourceManager;Lnet/minecraft/profiler/IProfiler;)V",at=@At("HEAD"),require=1)
 private void backport$teraDisc(Map<ResourceLocation,JsonElement> tables,IResourceManager resources,IProfiler profiler,CallbackInfo ci){local.pixelmon.backport.MusicLoot.apply(tables);}
}
