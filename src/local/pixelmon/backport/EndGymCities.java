package local.pixelmon.backport;
import java.util.*;
import com.pixelmonmod.pixelmon.world.structure.type.GymConfig;
import net.minecraft.util.math.*;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.provider.EndBiomeProvider;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.feature.structure.*;

public final class EndGymCities {
    private EndGymCities(){}
    public static boolean psychic(GymConfig config){return GymThemePaths.poolTypes(config.func_242810_c().get().func_214947_b().toString()).contains("psychic");}
    public static boolean eligible(ChunkGenerator generator,Biome biome,int chunkX,int chunkZ){
        return generator.func_202090_b() instanceof EndBiomeProvider&&biome.func_201856_r()==Biome.Category.THEEND
            &&EndCityPolicy.outsideCenter(chunkX*16,chunkZ*16,chunkX*16+15,chunkZ*16+15);
    }
    public static int houses(List<? extends StructurePiece> pieces){return (int)pieces.stream().filter(p->p.toString().contains("/gym_psychic_house_")).count();}
    public static int roads(List<? extends StructurePiece> pieces){return (int)pieces.stream().filter(p->p.toString().contains("/gym_psychic_road_")&&!p.toString().contains("_terminator_")).count();}
    /** Decorations and NPCs must not turn a handful of houses into a full district. */
    public static boolean district(List<? extends StructurePiece> pieces){
        return houses(pieces)>=8&&roads(pieces)>=8
            &&pieces.stream().anyMatch(p->p.toString().contains("/gym_psychic]"))
            &&pieces.stream().anyMatch(p->p.toString().contains("/gym_psychic_pokecenter]"))
            &&pieces.stream().anyMatch(p->p.toString().contains("/gym_psychic_pokemart]"));
    }
    private static boolean foundation(StructurePiece piece){
        String path=piece.toString();
        if(!path.contains("pixelmon_backport:gyms/psychic/"))return false;
        // Lamps, rooftop decorations and attached NPCs rest on other city
        // pieces. Their attachment height is not a separate terrain floor.
        return path.contains("/gym_psychic]")||path.contains("/gym_psychic_tc_")||path.contains("/gym_psychic_house_")
            ||path.contains("/gym_psychic_road_")||path.contains("/gym_psychic_pokecenter]")||path.contains("/gym_psychic_pokemart]")
            ||path.contains("/gym_psychic_blacksmith]")||path.contains("/gym_psychic_church]")||path.contains("/gym_psychic_tcg_shop]")||path.contains("/gym_psychic_daycare]")
            ||path.contains("/gym_psychic_farm_")||path.contains("/gym_psychic_berry_");
    }
    private static boolean projected(StructurePiece piece){
        return ((AbstractVillagePiece)piece).func_214826_b().func_214854_c()==net.minecraft.world.gen.feature.jigsaw.JigsawPattern.PlacementBehaviour.TERRAIN_MATCHING;
    }
    public static boolean complete(ChunkGenerator generator,List<StructurePiece> pieces){
        if(!district(pieces))return false;
        MutableBoundingBox bounds=MutableBoundingBox.func_78887_a();int floor=Integer.MAX_VALUE;
        for(var piece:pieces){
            bounds.func_78888_b(piece.func_74874_b());
            if(!(piece instanceof AbstractVillagePiece))return false;
            var village=(AbstractVillagePiece)piece;
            if(foundation(piece)&&!projected(piece))floor=Math.min(floor,village.func_214828_c().func_177956_o()+village.func_214830_d()-1);
        }
        if(floor<16||floor>200)return false;
        long width=(long)bounds.field_78893_d-bounds.field_78897_a+1,depth=(long)bounds.field_78892_f-bounds.field_78896_c+1;
        if(width<=0||depth<=0||width>512||depth>512||!EndCityPolicy.outsideCenter(bounds.field_78897_a,bounds.field_78896_c,bounds.field_78893_d,bounds.field_78892_f))return false;
        // The island's underside is uneven too: a single horizontal support
        // plane can pass below solid terrain in otherwise supported columns.
        // Check each column locally, allowing native entrance/road offsets.
        final int columns=(int)depth;int[] foundations=new int[(int)(width*depth)];Arrays.fill(foundations,Integer.MIN_VALUE);
        boolean[] occupied=new boolean[foundations.length];
        for(var piece:pieces){
            var footprint=piece.func_74874_b();
            for(int x=footprint.field_78897_a;x<=footprint.field_78893_d;x++)for(int z=footprint.field_78896_c;z<=footprint.field_78892_f;z++)
                occupied[(x-bounds.field_78897_a)*columns+z-bounds.field_78896_c]=true;
            // Attached trainer/villager templates stand on buildings, not on
            // separate terrain foundations (some stand on an upper floor).
            // Native gravity processors project roads onto each local terrain
            // column; their stored attachment Y is not a horizontal floor.
            if(!foundation(piece)||projected(piece))continue;
            var village=(AbstractVillagePiece)piece;int base=village.func_214828_c().func_177956_o()+village.func_214830_d()-1;
            if(base<16||base>200)return false;var box=piece.func_74874_b();
            for(int x=box.field_78897_a;x<=box.field_78893_d;x++)for(int z=box.field_78896_c;z<=box.field_78892_f;z++){
                int index=(x-bounds.field_78897_a)*columns+z-bounds.field_78896_c;foundations[index]=Math.max(foundations[index],base);
            }
        }
        // Query terrain columns from the generator, never load/generate nearby
        // chunks or flatten/fill the island to make an unsuitable start fit.
        return EndCityPolicy.supported(bounds.field_78897_a,bounds.field_78896_c,bounds.field_78893_d,bounds.field_78892_f,(x,z)->{
            int index=(x-bounds.field_78897_a)*columns+z-bounds.field_78896_c;
            // Empty corners between branches contain no part of the city.
            // Every occupied building, street and decoration column is checked.
            if(!occupied[index])return true;
            var column=generator.func_230348_a_(x,z);
            int base=foundations[index];
            if(base==Integer.MIN_VALUE){
                base=generator.func_222532_b(x,z,net.minecraft.world.gen.Heightmap.Type.WORLD_SURFACE_WG);
                if(base<18||base>200)return false;
                return column.func_180495_p(new BlockPos(x,base-1,z)).func_185904_a().func_76220_a()
                    &&column.func_180495_p(new BlockPos(x,base-2,z)).func_185904_a().func_76220_a();
            }
            for(int drop=1;drop<=5;drop++)if(column.func_180495_p(new BlockPos(x,base-drop,z)).func_185904_a().func_76220_a()
                &&column.func_180495_p(new BlockPos(x,base-drop-1,z)).func_185904_a().func_76220_a())return true;
            return false;
        });
    }
}
