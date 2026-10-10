package local.pixelmon.backport;
import com.pixelmonmod.pixelmon.items.PixelmonItem;
import net.minecraft.item.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import net.minecraft.util.*;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.RayTraceContext;
/** Creative placement tool, not a required survival item. Natural dens are renewable. */
public final class TeraDenItem extends PixelmonItem {
    public TeraDenItem(){super(new Item.Properties().func_200916_a(ItemGroup.field_78026_f).func_200917_a(1));}
    @Override public ActionResult<ItemStack> func_77659_a(World world,PlayerEntity player,Hand hand){
        ItemStack stack=player.func_184586_b(hand);if(!player.field_71075_bZ.field_75098_d)return ActionResult.func_226251_d_(stack);
        BlockRayTraceResult hit=func_219968_a(world,player,RayTraceContext.FluidMode.NONE);
        if(hit.func_216346_c()!=RayTraceResult.Type.BLOCK)return ActionResult.func_226250_c_(stack);
        if(!world.field_72995_K){BlockPos pos=hit.func_216350_a().func_177972_a(hit.func_216354_b());TeraDenEntity den=new TeraDenEntity(world);den.func_226286_f_(pos.func_177958_n()+.5,pos.func_177956_o(),pos.func_177952_p()+.5);den.roll((int)(world.func_72820_D()/24000));world.func_217376_c(den);}
        return ActionResult.func_226249_b_(stack);
    }
}
