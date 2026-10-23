package local.pixelmon.backport;

import java.util.*;
import com.pixelmonmod.pixelmon.world.structure.type.GymConfig;
import net.minecraft.util.math.*;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.world.gen.*;
import net.minecraft.world.gen.feature.jigsaw.JigsawManager;
import net.minecraft.world.gen.feature.structure.*;
import net.minecraft.world.gen.feature.template.TemplateManager;

/** Select terrain for a full native city; never shorten or clip its branches. */
public final class EndGymPlacement {
    private EndGymPlacement(){}
    private static final class Anchor {
        final BlockPos position;final int score;
        Anchor(BlockPos position,int score){this.position=position;this.score=score;}
    }
    public static void assemble(DynamicRegistries registries,GymConfig config,JigsawManager.IPieceFactory factory,
            ChunkGenerator generator,TemplateManager templates,BlockPos origin,List<AbstractVillagePiece> pieces,
            Random random,boolean expand,boolean project){
        Map<Long,Integer> heights=new HashMap<>();List<Anchor> anchors=new ArrayList<>();
        // Keep relocation close to the real structure-start chunk. Vanilla
        // references only look eight chunks away; larger shifts can lose pieces.
        for(int dx:new int[]{0,-16,16,-32,32})for(int dz:new int[]{0,-16,16,-32,32}){
            int x=origin.func_177958_n()+dx,z=origin.func_177952_p()+dz;
            int minimum=256,maximum=0;boolean land=true;
            terrain: for(int ox:new int[]{-64,0,64})for(int oz:new int[]{-64,0,64}){
                long key=new BlockPos(x+ox,0,z+oz).func_218275_a();
                int height=heights.computeIfAbsent(key,k->generator.func_222532_b(x+ox,z+oz,Heightmap.Type.WORLD_SURFACE_WG));
                if(height<40||height>160){land=false;break terrain;}
                minimum=Math.min(minimum,height);maximum=Math.max(maximum,height);
            }
            if(land)anchors.add(new Anchor(origin.func_177982_a(dx,0,dz),minimum*4-(maximum-minimum)*8-Math.abs(dx)-Math.abs(dz)));
        }
        anchors.sort(Comparator.comparingInt((Anchor a)->a.score).reversed());
        long seed=random.nextLong();int ordinal=0;
        for(int index=0;index<Math.min(4,anchors.size());index++)for(int layout=0;layout<3;layout++){
            List<AbstractVillagePiece> candidate=new ArrayList<>();
            JigsawManager.func_242837_a(registries,config,factory,generator,templates,anchors.get(index).position,
                candidate,new Random(seed+0x9e3779b97f4a7c15L*++ordinal),expand,project);
            if(!EndGymCities.district(candidate)||!reachable(origin,candidate))continue;
            if(EndGymCities.complete(generator,new ArrayList<StructurePiece>(candidate))){pieces.addAll(candidate);return;}
        }
    }
    static boolean reachable(BlockPos origin,List<? extends StructurePiece> pieces){
        int cx=origin.func_177958_n()>>4,cz=origin.func_177952_p()>>4;
        for(var piece:pieces){var b=piece.func_74874_b();
            if((b.field_78897_a>>4)<cx-8||(b.field_78893_d>>4)>cx+8||(b.field_78896_c>>4)<cz-8||(b.field_78892_f>>4)>cz+8)return false;
        }
        return true;
    }
}
