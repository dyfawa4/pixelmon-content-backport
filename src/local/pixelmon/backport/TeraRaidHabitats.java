package local.pixelmon.backport;

import java.util.*;
import com.pixelmonmod.pixelmon.api.pokemon.species.*;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.api.spawning.SpawnSet;
import com.pixelmonmod.pixelmon.api.spawning.archetypes.entities.pokemon.SpawnInfoPokemon;
import com.pixelmonmod.pixelmon.init.registry.RaidRegistry;
import com.pixelmonmod.pixelmon.spawning.PixelmonSpawning;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.registry.Registry;

/** Same live biome-to-raid registry used by native Max RaidRandomizer. */
public final class TeraRaidHabitats {
    private TeraRaidHabitats(){}
    private static final Set<Integer> ULTRA_BEASTS=Set.of(793,794,795,796,797,798,799,803,804,805,806);
    public static boolean ultraBeast(int dex){return ULTRA_BEASTS.contains(dex);}
    /** Ordinary Max entries retain their native table; only Ultra Beasts require this extra gate. */
    public static boolean allowedNative(World world,BlockPos pos,int dex,String form){
        return !ultraBeast(dex)||new Context(world,pos).allowed(dex+" form:"+(form==null?"":form));
    }
    public static final class Context {
        public final ResourceLocation biome,dimension;
        private final boolean swamp;
        private final Set<String> nativeHere=new HashSet<>(),nativeKnown=new HashSet<>();
        private final Map<Integer,List<SpawnInfoPokemon>> wild=new HashMap<>();
        private final Map<String,Boolean> decisions=new HashMap<>();
        public Context(World world,BlockPos pos){
            biome=world.func_226691_t_(pos).getRegistryName();dimension=world.func_234923_W_().func_240901_a_();
            swamp=biome!=null&&BiomeDictionary.hasType(RegistryKey.func_240903_a_(Registry.field_239720_u_,biome),BiomeDictionary.Type.SWAMP);
            for(var collection:RaidRegistry.getAllRaids())for(var list:collection.getRaids().getGroupings().values())for(var raid:list)nativeKnown.add(key(raid.getSpecies(),raid.getForm()));
            var group=biome==null?null:RaidRegistry.getBiomeToRaids().get(biome.toString());
            if(group!=null)for(var list:group.getRaids().getGroupings().values())for(var raid:list)nativeHere.add(key(raid.getSpecies(),raid.getForm()));
            List<SpawnSet> sets=new ArrayList<>(PixelmonSpawning.standard);sets.addAll(PixelmonSpawning.legendaries);
            for(SpawnSet set:sets)for(var raw:set.spawnInfos)if(raw instanceof SpawnInfoPokemon){
                var info=(SpawnInfoPokemon)raw;
                if(info.getSpecies()!=null && info.condition!=null && (info.rarity>0 || (info.percentage!=null&&info.percentage>0)))wild.computeIfAbsent(info.getSpecies().getDex(),d->new ArrayList<>()).add(info);
            }
        }
        public boolean allowed(String spec){
            return decisions.computeIfAbsent(spec,this::check);
        }
        private boolean check(String spec){
            String[] parts=spec.split(" ");var species=resolve(parts[0]);
            if(species==null || !RaidSpawnExclusions.allowed(species.getDex()) || biome==null)return false;
            String form=species.getDefaultForm().getName();for(String part:parts)if(part.startsWith("form:"))form=part.substring(5);
            if(species.getDex()==550&&form.equals("white_striped"))form="white";
            String key=key(species.getDex(),form);
            if(!explicitEnvironment(species.getDex(),dimension.toString(),swamp))return false;
            // Older Max tables contain Blacephalon in Overworld plains/flower fields.
            // Ultra Beasts use their actual Ultra Space habitats, not those legacy entries.
            if(ULTRA_BEASTS.contains(species.getDex()))return wild(species,form,new HashSet<>());
            if(nativeHere.contains(key))return true;
            if(nativeKnown.contains(key))return false;
            return wild(species,form,new HashSet<>());
        }
        private boolean wild(Species species,String form,Set<Integer> seen){
            if(!seen.add(species.getDex()))return false;
            boolean direct=false;
            for(var info:wild.getOrDefault(species.getDex(),Collections.emptyList())){
                String sourceForm=info.getForm()==null?species.getDefaultForm().getName():info.getForm().getName();
                if(!key(species.getDex(),sourceForm).equals(key(species.getDex(),form)))continue;
                direct=true;
                var condition=info.condition;
                if(condition.biomes!=null && !condition.biomes.contains(biome))continue;
                if(condition.cachedDimensions!=null && !condition.cachedDimensions.isEmpty() && !condition.cachedDimensions.contains(dimension))continue;
                return true;
            }
            // Evolution-only additions inherit their family's habitat, never a global pool.
            if(direct)return false;
            for(Species parent:species.getForm(form).getPreEvolutions()){
                String parentForm=parent.getDefaultForm().getName(),parentKey=key(parent.getDex(),parentForm);
                if(ULTRA_BEASTS.contains(species.getDex())?wild(parent,parentForm,seen):nativeHere.contains(parentKey) || (!nativeKnown.contains(parentKey)&&wild(parent,parentForm,seen)))return true;
            }
            return false;
        }
    }
    private static Species resolve(String token){
        // Native fromNameOrDex accepts Integer dex values, but a numeric String
        // is treated as a species NAME. Saved raid validation passes "152 form:".
        if(token.matches("[0-9]+")){
            try{return PixelmonSpecies.fromDex(Integer.parseInt(token)).orElse(null);}
            catch(NumberFormatException invalid){return null;}
        }
        return PixelmonSpecies.fromNameOrDex(token).orElse(null);
    }
    public static String key(int dex,String form){
        Species species=PixelmonSpecies.fromNationalDex(dex);
        if(form==null || form.isEmpty() || (species!=null&&species.isDefaultForm(form)))form="";
        return dex+":"+form.toLowerCase(Locale.ROOT);
    }
    /** User-requested boundaries override the older native End Eternatus entry. */
    public static boolean explicitEnvironment(int dex,String dimension,boolean swamp){
        if(dex==1026)return false;
        if(ULTRA_BEASTS.contains(dex))return dimension.equals("pixelmon:ultra_space");
        if(dex==1005)return dimension.equals("minecraft:the_end");
        if(dex==890)return dimension.equals("minecraft:overworld")&&swamp;
        return true;
    }
}
