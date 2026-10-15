package local.pixelmon.backport;

import com.pixelmonmod.pixelmon.api.pokemon.PokemonFactory;
import com.pixelmonmod.pixelmon.api.pokemon.species.Species;
import com.pixelmonmod.pixelmon.api.pokemon.species.Stats;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.api.util.helpers.RandomHelper;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import java.util.*;

/** Lucky block pools are derived from loaded species, never holes in the national dex. */
public final class LuckySpecies {
    private LuckySpecies(){}
    private static final Set<String> DISABLED=new HashSet<>();
    static {for(var name:Content.DATA.getAsJsonArray("disabledPreviewSpecies"))DISABLED.add(name.getAsString());}
    public static boolean allowed(Species species){
        if(species==null || species.getDex()<=0 || "MissingNo".equalsIgnoreCase(species.getName()) || DISABLED.contains(species.getName()))return false;
        if(PixelmonSpecies.fromDex(species.getDex()).orElse(null)!=species)return false;
        Stats form=species.getDefaultForm();
        return form!=null && !form.isTemporary() && !form.hasTag("undexable") && !form.hasTag("unimplemented") && !form.hasTag("disabled");
    }
    public static List<Species> pool(int category){
        List<Species> result=new ArrayList<>();
        Set<Integer> legends=category==2?PixelmonSpecies.getLegendaries(false):Collections.emptySet();
        for(Species species:PixelmonSpecies.getAll())if(allowed(species)
            && (category!=1 || species.isUltraBeast()) && (category!=2 || legends.contains(species.getDex())))result.add(species);
        return result;
    }
    public static void spawn(World world,BlockPos pos,int category){
        if(world==null || world.field_72995_K)return;
        List<Species> choices=pool(category);
        if(choices.isEmpty())return;
        Species species=choices.get(RandomHelper.rand.nextInt(choices.size()));
        PixelmonEntity entity=new PixelmonEntity(world,PokemonFactory.create(species));
        entity.func_70107_b(pos.func_177958_n()+0.5,pos.func_177956_o(),pos.func_177952_p()+0.5);
        entity.canDespawn=false;world.func_217376_c(entity);
    }
}
