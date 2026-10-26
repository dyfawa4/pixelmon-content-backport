package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.entities.npcs.NPCEntity;
import local.pixelmon.backport.client.NpcNameplates;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Optional presentation bridge; never rewrites native indexed names or levels. */
@Pseudo @Mixin(targets="mcp.mobius.waila.addons.core.HUDHandlerEntities",remap=false)
public abstract class JadeNpcNameMixin {
    @Inject(method="getEntityName",at=@At("HEAD"),cancellable=true)
    private static void backport$npcName(Entity entity,CallbackInfoReturnable<String> ci){if(entity instanceof NPCEntity)ci.setReturnValue(NpcNameplates.tooltip((NPCEntity)entity));}
}
