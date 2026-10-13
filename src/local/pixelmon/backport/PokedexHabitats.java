package local.pixelmon.backport;

import java.util.*;
import com.pixelmonmod.pixelmon.api.pokedex.PlayerPokedex.PokedexSpawnData;
import com.pixelmonmod.pixelmon.api.spawning.SpawnSet;
import com.pixelmonmod.pixelmon.api.spawning.archetypes.entities.pokemon.SpawnInfoPokemon;
import com.pixelmonmod.pixelmon.spawning.PixelmonSpawning;

/** Read current loaded rules by actual species, not a spawn-set label or cached category. */
public final class PokedexHabitats {
    private PokedexHabitats(){}
    public static boolean handles(int dex){return dex>=906 && dex<=CapSpecies.DEX;}
    public static List<PokedexSpawnData> data(int dex){
        List<SpawnSet> sets=new ArrayList<>();sets.addAll(PixelmonSpawning.standard);sets.addAll(PixelmonSpawning.legendaries);
        List<PokedexSpawnData> result=new ArrayList<>();Set<String> seen=new HashSet<>();
        for(SpawnSet set:sets)for(var raw:set.spawnInfos){
            if(!(raw instanceof SpawnInfoPokemon) || raw.condition==null || raw.condition.biomes==null)continue;
            var info=(SpawnInfoPokemon)raw;var species=info.getSpecies();
            if(species==null || species.getDex()!=dex)continue;
            // An explicitly disabled rule is not a wild habitat. Do not invent
            // locations for evolution/raid-only species with no loaded rules.
            if(info.rarity<=0 && (info.percentage==null || info.percentage<=0))continue;
            for(var biome:info.condition.biomes){
                if(biome==null)continue;
                String key=biome+"|"+info.condition.times+"|"+info.condition.cachedWeathers+"|"+info.condition.minY+"|"+info.condition.maxY;
                if(!seen.add(key))continue;
                var row=new PokedexSpawnData(biome.toString());
                if(info.condition.times!=null)for(var time:info.condition.times)row.addTime(time);
                if(info.condition.cachedWeathers!=null)for(var weather:info.condition.cachedWeathers)row.addWeather(weather);
                if(info.condition.minY!=null)row.setMinY(info.condition.minY);
                if(info.condition.maxY!=null)row.setMaxY(info.condition.maxY);
                result.add(row);
            }
        }
        return result;
    }
}
