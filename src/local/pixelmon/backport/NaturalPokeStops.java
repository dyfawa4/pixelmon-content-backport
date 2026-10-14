package local.pixelmon.backport;
import java.awt.Color;
import java.util.*;
import com.pixelmonmod.pixelmon.api.registries.PixelmonBlocks;
import com.pixelmonmod.pixelmon.blocks.PokeStopBlock;
import com.pixelmonmod.pixelmon.blocks.tileentity.PokeStopTileEntity;
import net.minecraft.world.IWorld;
import net.minecraft.world.gen.WorldGenRegion;
import net.minecraft.world.gen.feature.template.Template;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.block.BlockState;

/** Upgrade newly generated native way-point stops, never loaded or player-placed blocks. */
public final class NaturalPokeStops {
    private NaturalPokeStops(){}
    /** Stable mutually exclusive tier: 0 common, 1 rare, 2 legendary. */
    public static int tier(long worldSeed,long position){
        int roll=new Random(worldSeed^position^0x506f6b6553746f70L).nextInt(100);
        return roll<30?1:roll<40?2:0;
    }
    public static List<Template.BlockInfo> convert(IWorld world,List<Template.BlockInfo> entries){
        if(!(world instanceof WorldGenRegion))return entries;
        List<Template.BlockInfo> result=null;
        for(int i=0;i<entries.size();i++){
            Template.BlockInfo old=entries.get(i);
            if(old.field_186243_b.func_177230_c()!=PixelmonBlocks.common_pokestop)continue;
            // Templates can be processed again for bordering chunks. A stable
            // seed per absolute position prevents rerolls and consumes no world RNG.
            int tier=tier(((WorldGenRegion)world).func_72905_C(),old.field_186242_a.func_218275_a());
            if(tier==0)continue;
            PokeStopBlock block=tier==1?PixelmonBlocks.rare_pokestop:PixelmonBlocks.legendary_pokestop;
            BlockState state=block.func_176223_P().func_206870_a(PokeStopBlock.FACING,old.field_186243_b.func_177229_b(PokeStopBlock.FACING));
            CompoundNBT tag=old.field_186244_c==null?null:old.field_186244_c.func_74737_b();
            if(tag!=null){
                PokeStopTileEntity defaults=(PokeStopTileEntity)block.func_196283_a_(world);
                tag.func_74778_a("PokeStopLootTable",defaults.getLootTable().toString());
                Color color=defaults.getColor();
                tag.func_74768_a("PokeStopColorRed",color.getRed());tag.func_74768_a("PokeStopColorGreen",color.getGreen());
                tag.func_74768_a("PokeStopColorBlue",color.getBlue());tag.func_74768_a("PokeStopColorAlpha",color.getAlpha());
            }
            if(result==null)result=new ArrayList<>(entries);
            result.set(i,new Template.BlockInfo(old.field_186242_a,state,tag));
        }
        return result==null?entries:result;
    }
}
