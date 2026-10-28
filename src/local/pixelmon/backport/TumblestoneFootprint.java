package local.pixelmon.backport;
import com.pixelmonmod.pixelmon.blocks.TumblestoneBlock;
import net.minecraft.block.*;
import net.minecraft.block.material.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.eventbus.api.SubscribeEvent;
/** Reserve only the tall growth stages. Harvesting stays entirely in the native ore. */
public final class TumblestoneFootprint {
 public static final net.minecraftforge.fml.RegistryObject<Block> UPPER=DayCareFootprint.BLOCKS.register("tumblestone_upper",Upper::new);
 private static final ThreadLocal<Boolean> BUSY=ThreadLocal.withInitial(()->false);
 public static boolean ore(BlockState s){return s.func_177230_c() instanceof TumblestoneBlock;}
 public static boolean tall(BlockState s){return ore(s)&&s.func_177229_b(TumblestoneBlock.AGE)>3&&s.func_177229_b(TumblestoneBlock.DESTROYED)<3;}
 public static void changed(World w,BlockPos base){
  if(w.field_72995_K||BUSY.get()||World.func_189509_E(base.func_177984_a()))return;
  BlockPos top=base.func_177984_a();BlockState s=w.func_180495_p(base),old=w.func_180495_p(top);
  if(!ore(s)&&!(old.func_177230_c() instanceof Upper))return;
  BUSY.set(true);try{
   if(tall(s)&&old.func_185904_a().func_76222_j()&&!(old.func_177230_c() instanceof Upper))w.func_180501_a(top,UPPER.get().func_176223_P().func_206870_a(net.minecraft.state.properties.BlockStateProperties.field_208198_y,w.func_204610_c(top).func_206886_c()==net.minecraft.fluid.Fluids.field_204546_a),3);
   else if(!tall(s)&&old.func_177230_c() instanceof Upper)w.func_180501_a(top,w.func_204610_c(top).func_206883_i(),3);
  }finally{BUSY.remove();}
 }
 public static final class Upper extends Block implements IWaterLoggable {
  Upper(){super(AbstractBlock.Properties.func_200945_a(Material.field_151576_e).func_226896_b_().func_200943_b(5).func_200947_a(SoundType.field_185853_f));func_180632_j(func_176223_P().func_206870_a(net.minecraft.state.properties.BlockStateProperties.field_208198_y,false));}
  @Override protected void func_206840_a(net.minecraft.state.StateContainer.Builder<Block,BlockState> b){b.func_206894_a(net.minecraft.state.properties.BlockStateProperties.field_208198_y);}
  @Override public net.minecraft.fluid.FluidState func_204507_t(BlockState s){return s.func_177229_b(net.minecraft.state.properties.BlockStateProperties.field_208198_y)?net.minecraft.fluid.Fluids.field_204546_a.func_207204_a(false):super.func_204507_t(s);}
  @Override public BlockRenderType func_149645_b(BlockState s){return BlockRenderType.INVISIBLE;}
  @Override public PushReaction func_149656_h(BlockState s){return PushReaction.BLOCK;}
  @Override public ItemStack getPickBlock(BlockState s,RayTraceResult hit,IBlockReader w,BlockPos pos,PlayerEntity p){BlockPos base=pos.func_177977_b();BlockState bottom=w.func_180495_p(base);return ore(bottom)?bottom.func_177230_c().getPickBlock(bottom,hit,w,base,p):ItemStack.field_190927_a;}
  @Override public float func_180647_a(BlockState s,PlayerEntity p,IBlockReader w,BlockPos pos){BlockPos base=pos.func_177977_b();BlockState bottom=w.func_180495_p(base);return ore(bottom)?bottom.func_177230_c().func_180647_a(bottom,p,w,base):0;}
  @Override public void func_176208_a(World w,BlockPos pos,BlockState s,PlayerEntity p){BlockState bottom=w.func_180495_p(pos.func_177977_b());if(ore(bottom))w.func_217378_a(p,2001,pos.func_177977_b(),Block.func_196246_j(bottom));}
  @Override public boolean removedByPlayer(BlockState s,World w,BlockPos pos,PlayerEntity p,boolean harvest,net.minecraft.fluid.FluidState fluid){
   if(w.field_72995_K)return false;
   BlockPos base=pos.func_177977_b();BlockState bottom=w.func_180495_p(base);
   if(tall(bottom)&&!p.field_71075_bZ.field_75098_d&&bottom.func_177229_b(TumblestoneBlock.AGE)>=5&&p.func_184614_ca().func_77973_b() instanceof com.pixelmonmod.pixelmon.items.HammerItem)w.func_217378_a(p,2001,base,Block.func_196246_j(bottom));
   BUSY.set(true);try{if(ore(bottom))bottom.func_177230_c().removedByPlayer(bottom,w,base,p,harvest,w.func_204610_c(base));}finally{BUSY.remove();}changed(w,base);return false;
  }
  @Override public boolean addDestroyEffects(BlockState s,World w,BlockPos pos,net.minecraft.client.particle.ParticleManager particles){BlockState bottom=w.func_180495_p(pos.func_177977_b());if(ore(bottom))particles.func_180533_a(pos.func_177977_b(),bottom);return true;}
  // Removal is owned by changed(), without a second automatic destroy event.
  @Override public BlockState func_196271_a(BlockState s,Direction side,BlockState neighbour,IWorld w,BlockPos pos,BlockPos other){return s;}
 }
 public static final class Events {
  private final java.util.Queue<Chunk> pending=new java.util.concurrent.ConcurrentLinkedQueue<>();
  @SubscribeEvent public void load(net.minecraftforge.event.world.ChunkEvent.Load e){if(e.getWorld() instanceof net.minecraft.world.server.ServerWorld&&e.getChunk() instanceof Chunk)pending.add((Chunk)e.getChunk());}
  @SubscribeEvent public void unload(net.minecraftforge.event.world.WorldEvent.Unload e){pending.removeIf(c->c.func_177412_p()==e.getWorld());}
  @SubscribeEvent public void tick(net.minecraftforge.event.TickEvent.ServerTickEvent e){if(e.phase!=net.minecraftforge.event.TickEvent.Phase.END)return;for(int n=0;n<4&&!pending.isEmpty();n++){
   Chunk c=pending.poll();World w=c.func_177412_p();if(!w.func_175667_e(new BlockPos(c.func_76632_l().field_77276_a<<4,64,c.func_76632_l().field_77275_b<<4)))continue;
   for(var section:c.func_76587_i())if(section!=null&&section.func_235962_a_(TumblestoneFootprint::ore))for(int y=0;y<16;y++)for(int x=0;x<16;x++)for(int z=0;z<16;z++)if(ore(section.func_177485_a(x,y,z)))changed(w,new BlockPos((c.func_76632_l().field_77276_a<<4)+x,section.func_222632_g()+y,(c.func_76632_l().field_77275_b<<4)+z));
  }}
 }
}
