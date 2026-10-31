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
    private static void backport$npcName(Entity entity,CallbackInfoReturnable<String> ci){if(entity instanceof com.pixelmonmod.pixelmon.entities.DenEntity)ci.setReturnValue(new net.minecraft.util.text.TranslationTextComponent(entity instanceof local.pixelmon.backport.TeraDenEntity?"pixelmon.backport.den.tera":"pixelmon.backport.den.max").getString());else if(entity instanceof NPCEntity)ci.setReturnValue(NpcNameplates.tooltip((NPCEntity)entity));else if(entity instanceof com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity){var pokemon=((com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity)entity).getPokemon();ci.setReturnValue(pokemon.getDisplayName());}}
}
