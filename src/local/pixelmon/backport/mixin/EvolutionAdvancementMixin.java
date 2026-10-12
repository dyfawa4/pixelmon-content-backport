package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.entities.pixelmon.helpers.EvolutionQuery;
import local.pixelmon.backport.LegendAdvancements;
import net.minecraft.entity.player.ServerPlayerEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A completed native Kubfu evolution is an acquisition, without a capture event. */
@Mixin(value=EvolutionQuery.class,remap=false)
public abstract class EvolutionAdvancementMixin {
    @Shadow Pokemon pokemon;
    @Shadow ServerPlayerEntity player;
    @Shadow boolean normalEvolution;
    @Unique private int backport$beforeDex;
    @Inject(method="doEvoSwitch",at=@At("HEAD"))
    private void backport$beforeEvolution(CallbackInfo ci){backport$beforeDex=pokemon==null?-1:pokemon.getSpecies().getDex();}
    @Inject(method="doEvoSwitch",at=@At("RETURN"))
    private void backport$afterEvolution(CallbackInfo ci){if(normalEvolution)LegendAdvancements.urshifuEvolution(player,pokemon,backport$beforeDex);}
}
