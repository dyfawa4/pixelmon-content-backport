package local.pixelmon.backport.mixin;
import com.google.gson.JsonElement;
import java.util.Map;
import net.minecraft.util.ResourceLocation;
import net.minecraft.resources.IResourceManager;
import net.minecraft.profiler.IProfiler;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import local.pixelmon.backport.Content;
@Mixin(targets="com.pixelmonmod.api.loader.DatapackLoader$Impl", remap=false)
public abstract class DatapackMixin {
    @Shadow @Final private String context;
    @Shadow @Final private String type;
    @Inject(method="apply(Ljava/util/Map;Lnet/minecraft/resources/IResourceManager;Lnet/minecraft/profiler/IProfiler;)V",at=@At("HEAD"))
    private void backport$merge(Map<ResourceLocation,JsonElement> resources, IResourceManager manager, IProfiler profiler, CallbackInfo ci) {
        Content.apply(context, type, resources);
    }
}
