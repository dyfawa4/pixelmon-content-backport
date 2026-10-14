package local.pixelmon.backport.mixin;
import java.util.*;
import local.pixelmon.backport.*;
import com.pixelmonmod.pixelmon.spawning.*;
import com.pixelmonmod.pixelmon.api.events.spawning.LegendaryCheckSpawnsEvent;
import com.pixelmonmod.pixelmon.api.config.PixelmonConfigProxy;
import net.minecraft.command.CommandSource;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.text.TextFormatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=com.pixelmonmod.pixelmon.api.spawning.archetypes.algorithms.checkspawns.LegendaryCheckSpawns.class,remap=false)
public abstract class CapSpawnChanceDisplayMixin {
    @ModifyArg(method="lambda$checkSpawns$0",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/api/util/helpers/TextHelper;translate(Lnet/minecraft/util/text/TextFormatting;Ljava/lang/String;)Lnet/minecraft/util/text/IFormattableTextComponent;"),index=1)
    private String backport$chanceLabels(String key){
        return key.equals("spawning.checkspawns.chanceofspawning")?"pixelmon_backport.spawns.global":key.equals("spawning.checkspawns.possiblespawns")?"pixelmon_backport.spawns.weights":key;
    }
    @Inject(method="lambda$checkSpawns$0",at=@At("RETURN"))
    private void backport$capChance(LegendarySpawner spawner,LegendaryCheckSpawnsEvent event,HashMap<String,Double> candidates,List<String> filters,CommandSource source,CallbackInfo ci){
        // SelectionAlgorithm keys are SpawnInfoPokemon.toString(), which uses
        // the server's translated species name, rather than the registry name.
        String localized=new TranslationTextComponent("pixelmon.cappikachu",Locale.ROOT+"").getString();
        if(spawner!=PixelmonSpawning.legendarySpawner || !event.shouldShowChance || !candidates.entrySet().stream().anyMatch(e->
            (CapSpecies.NAME.equalsIgnoreCase(e.getKey()) || localized.equals(e.getKey()) || "pixelmon.cappikachu".equals(e.getKey()))
            && e.getValue()!=null && e.getValue()>0))return;
        double global=PixelmonConfigProxy.getSpawning().getLegendarySpawnChance();
        source.func_197030_a(new TranslationTextComponent("pixelmon_backport.spawns.cap_chance_label").func_240699_a_(TextFormatting.GOLD)
            .func_230529_a_(new TranslationTextComponent("pixelmon_backport.spawns.cap_chance_value",String.format(Locale.ROOT,"%.1f",BurstPolicy.spawnChance(global)*100),String.format(Locale.ROOT,"%.1f",global*100)).func_240699_a_(TextFormatting.GREEN)),false);
    }
}
