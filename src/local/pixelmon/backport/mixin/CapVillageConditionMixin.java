package local.pixelmon.backport.mixin;

import com.pixelmonmod.pixelmon.api.spawning.SpawnInfo;
import com.pixelmonmod.pixelmon.api.spawning.SpawnLocation;
import com.pixelmonmod.pixelmon.api.spawning.archetypes.entities.pokemon.SpawnInfoPokemon;
import com.pixelmonmod.pixelmon.api.spawning.conditions.SpawnCondition;
import com.pixelmonmod.pixelmon.api.util.helpers.WorldHelper;
import local.pixelmon.backport.CapSpecies;
import local.pixelmon.backport.CapSpawns;
import local.pixelmon.backport.MagearnaSpawns;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Real structure references serve native checkspawns and spawning alike. */
@Mixin(value=SpawnCondition.class,remap=false)
public abstract class CapVillageConditionMixin {
    @Inject(method="fits",at=@At("HEAD"),cancellable=true)
    private void backport$capDryPlains(SpawnInfo info,SpawnLocation location,org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir){if(info instanceof SpawnInfoPokemon&&((SpawnInfoPokemon)info).getSpecies()!=null&&((SpawnInfoPokemon)info).getSpecies().getDex()==CapSpecies.DEX&&(!(location.location.world instanceof ServerWorld)||!CapSpawns.eligible((ServerWorld)location.location.world,location.location.pos)))cir.setReturnValue(false);}
    @Redirect(method="fits",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/api/util/helpers/WorldHelper;insideStructure(Lnet/minecraft/world/server/ServerWorld;Ljava/lang/String;Lnet/minecraft/util/math/BlockPos;)Z"))
    private boolean backport$capVillage(ServerWorld world,String structure,BlockPos pos,SpawnInfo info,SpawnLocation location){
        if(info instanceof SpawnInfoPokemon && ((SpawnInfoPokemon)info).getSpecies()!=null
            && ((SpawnInfoPokemon)info).getSpecies().getDex()==CapSpecies.DEX
            && ("village".equals(structure) || "minecraft:village".equals(structure)))return CapSpawns.village(world,pos);
        if(info instanceof SpawnInfoPokemon && ((SpawnInfoPokemon)info).getSpecies()!=null
            && ((SpawnInfoPokemon)info).getSpecies().getDex()==801
            && ("mansion".equals(structure) || "minecraft:mansion".equals(structure)))return MagearnaSpawns.mansion(world,pos);
        return WorldHelper.insideStructure(world,structure,pos);
    }
}
