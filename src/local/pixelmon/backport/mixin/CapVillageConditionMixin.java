package local.pixelmon.backport.mixin;

import com.pixelmonmod.pixelmon.api.spawning.SpawnInfo;
import com.pixelmonmod.pixelmon.api.spawning.SpawnLocation;
import com.pixelmonmod.pixelmon.api.spawning.archetypes.entities.pokemon.SpawnInfoPokemon;
import com.pixelmonmod.pixelmon.api.spawning.conditions.SpawnCondition;
import com.pixelmonmod.pixelmon.api.util.helpers.WorldHelper;
import local.pixelmon.backport.CapSpecies;
import local.pixelmon.backport.CapSpawns;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** The same real village references serve native checkspawns and spawning. */
@Mixin(value=SpawnCondition.class,remap=false)
public abstract class CapVillageConditionMixin {
    @Redirect(method="fits",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/api/util/helpers/WorldHelper;insideStructure(Lnet/minecraft/world/server/ServerWorld;Ljava/lang/String;Lnet/minecraft/util/math/BlockPos;)Z"))
    private boolean backport$capVillage(ServerWorld world,String structure,BlockPos pos,SpawnInfo info,SpawnLocation location){
        if(info instanceof SpawnInfoPokemon && ((SpawnInfoPokemon)info).getSpecies()!=null
            && ((SpawnInfoPokemon)info).getSpecies().getDex()==CapSpecies.DEX
            && ("village".equals(structure) || "minecraft:village".equals(structure)))return CapSpawns.village(world,pos);
        return WorldHelper.insideStructure(world,structure,pos);
    }
}
