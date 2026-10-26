package local.pixelmon.backport;
import com.pixelmonmod.pixelmon.blocks.daycare.DayCareBlock;
import net.minecraft.block.*;
import net.minecraft.block.material.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.*;
import net.minecraftforge.fml.RegistryObject;

/** The existing native lower block keeps the renderer and all daycare functionality. */
public final class DayCareFootprint {
    public static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,Content.NS);
    public static final RegistryObject<Block> UPPER=BLOCKS.register("day_care_upper",Upper::new);
    private static final ThreadLocal<Boolean> REMOVING=ThreadLocal.withInitial(()->false);
    public static boolean lower(BlockState state){return state.func_177230_c() instanceof DayCareBlock;}
    public static boolean upper(BlockState state){return state.func_177230_c() instanceof Upper;}
    public static boolean reserve(World world,BlockPos base){
        if(world.field_72995_K||!lower(world.func_180495_p(base))||World.func_189509_E(base.func_177984_a()))return false;
        BlockPos top=base.func_177984_a();BlockState old=world.func_180495_p(top);if(upper(old))return true;
        if(!old.func_185904_a().func_76222_j())return false;
        return world.func_180501_a(top,UPPER.get().func_176223_P(),3);
    }
    public static void removedLower(World world,BlockPos base){if(!REMOVING.get()&&upper(world.func_180495_p(base.func_177984_a()))){REMOVING.set(true);try{world.func_180501_a(base.func_177984_a(),Blocks.field_150350_a.func_176223_P(),3);}finally{REMOVING.remove();}}}
    private static void removedUpper(World world,BlockPos top,boolean drops){if(!REMOVING.get()&&lower(world.func_180495_p(top.func_177977_b()))){REMOVING.set(true);try{
        BlockPos base=top.func_177977_b();BlockState bottom=world.func_180495_p(base);
        // Companion removal is silent: the initiating break already owns event2001.
        if(drops)Block.func_220054_a(bottom,world,base,bottom.hasTileEntity()?world.func_175625_s(base):null,null,ItemStack.field_190927_a);
        world.func_180501_a(base,world.func_204610_c(base).func_206883_i(),3);
    }finally{REMOVING.remove();}}}
    public static final class Upper extends Block {
        Upper(){super(AbstractBlock.Properties.func_200945_a(Material.field_151573_f).func_200943_b(2f).func_200947_a(SoundType.field_185851_d));}
        @Override public BlockRenderType func_149645_b(BlockState state){return BlockRenderType.INVISIBLE;}
        @Override public PushReaction func_149656_h(BlockState state){return PushReaction.BLOCK;}
        @Override public ActionResultType func_225533_a_(BlockState state,World world,BlockPos pos,PlayerEntity player,Hand hand,BlockRayTraceResult hit){BlockPos base=pos.func_177977_b();BlockState bottom=world.func_180495_p(base);return lower(bottom)?bottom.func_177230_c().func_225533_a_(bottom,world,base,player,hand,new BlockRayTraceResult(hit.func_216347_e(),hit.func_216354_b(),base,false)):ActionResultType.PASS;}
        @Override public ItemStack getPickBlock(BlockState state,RayTraceResult target,IBlockReader world,BlockPos pos,PlayerEntity player){BlockState base=world.func_180495_p(pos.func_177977_b());return lower(base)?new ItemStack(base.func_177230_c()):ItemStack.field_190927_a;}
        @Override public void func_176208_a(World world,BlockPos pos,BlockState state,PlayerEntity player){
            BlockPos base=pos.func_177977_b();BlockState bottom=world.func_180495_p(base);
            if(lower(bottom))world.func_217378_a(player,2001,base,Block.func_196246_j(bottom));
            removedUpper(world,pos,!player.field_71075_bZ.field_75098_d);
        }
        @Override public boolean addDestroyEffects(BlockState state,World world,BlockPos pos,net.minecraft.client.particle.ParticleManager particles){
            BlockPos base=pos.func_177977_b();BlockState bottom=world.func_180495_p(base);
            if(lower(bottom))particles.func_180533_a(base,bottom);
            return true;
        }
        @Override public void func_196243_a(BlockState state,World world,BlockPos pos,BlockState next,boolean moving){if(next.func_177230_c()!=this)removedUpper(world,pos,true);super.func_196243_a(state,world,pos,next,moving);}
        @Override public BlockState func_196271_a(BlockState state,Direction side,BlockState neighbour,IWorld world,BlockPos pos,BlockPos other){return !REMOVING.get()&&side==Direction.DOWN&&!lower(neighbour)?Blocks.field_150350_a.func_176223_P():state;}
    }
    public static final class Events {
        @SubscribeEvent public void interact(PlayerInteractEvent.RightClickBlock event){World world=event.getWorld();if(world.field_72995_K)return;BlockPos base=event.getPos();if(lower(world.func_180495_p(base)))reserve(world,base);}
    }
}
