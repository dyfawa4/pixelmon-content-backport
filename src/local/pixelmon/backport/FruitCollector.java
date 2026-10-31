package local.pixelmon.backport;

import java.util.*;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.events.ApricornEvent;
import com.pixelmonmod.pixelmon.blocks.*;
import com.pixelmonmod.pixelmon.api.registries.PixelmonItems;
import local.pixelmon.backport.mixin.*;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.*;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;

/** Harvest fruit, never break leaves. Traversal is bounded and never loads chunks. */
public final class FruitCollector extends Item {
 public static final int LIMIT=20;
 public FruitCollector(){super(new Item.Properties().func_200918_c(10).func_208103_a(Rarity.RARE).func_200916_a(Backport.CREATIVE_TAB));}
 public static boolean is(ItemStack stack){return stack.func_77973_b() instanceof FruitCollector;}
 @Override public int func_77619_b(){return 0;}
 @Override public boolean func_77616_k(ItemStack stack){return false;}
 @Override public boolean isBookEnchantable(ItemStack stack,ItemStack book){return EnchantmentHelper.func_82781_a(book).keySet().stream().noneMatch(FruitCollector::forbidden);}
 @Override public boolean canApplyAtEnchantingTable(ItemStack stack,Enchantment enchantment){return false;}
 @Override public boolean func_82789_a(ItemStack stack,ItemStack material){return material.func_77973_b()==Items.field_151045_i;}
 @Override public float getXpRepairRatio(ItemStack stack){return 0;}
 public static boolean forbidden(Enchantment enchantment){var id=enchantment.getRegistryName();return new ResourceLocation("minecraft:mending").equals(id)||new ResourceLocation("minecraft:vanishing_curse").equals(id);}
 public static boolean leaves(BlockState s){return s.func_177230_c() instanceof BerryLeavesBlock||s.func_177230_c() instanceof ApricornLeavesBlock;}
 @Override public ActionResultType func_195939_a(ItemUseContext context){
  World world=context.func_195991_k();BlockPos start=context.func_195995_a();
  if(!leaves(world.func_180495_p(start)))return ActionResultType.PASS;
  if(world.field_72995_K)return ActionResultType.SUCCESS;
  if(!(context.func_195999_j() instanceof ServerPlayerEntity))return ActionResultType.PASS;
  var player=(ServerPlayerEntity)context.func_195999_j();var stack=context.func_195996_i();
  int harvested=harvest(world,player,stack,start,context.func_221531_n(),context.func_196000_l());
  if(harvested>0){stack.func_222118_a(1,player,p->p.func_213334_d(context.func_221531_n()));world.func_184133_a(null,player.func_233580_cy_(),net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("minecraft:entity.item.pickup")),SoundCategory.PLAYERS,.2f,1f+world.field_73012_v.nextFloat()*.4f);}
  return harvested>0?ActionResultType.SUCCESS:ActionResultType.CONSUME;
 }
 public static int harvest(World world,ServerPlayerEntity player,ItemStack tool,BlockPos start,Hand hand,Direction face){
  var queue=new ArrayDeque<BlockPos>();var seen=new HashSet<BlockPos>();queue.add(start);seen.add(start);int harvested=0,visited=0;
  int fortune=EnchantmentHelper.func_77506_a(Enchantments.field_185308_t,tool);
  while(!queue.isEmpty()&&harvested<LIMIT&&visited++<512){
   BlockPos pos=queue.removeFirst();if(!world.func_175667_e(pos))continue;var state=world.func_180495_p(pos);if(!leaves(state))continue;
   if(!world.func_175660_a(player,pos)||!player.func_175151_a(pos,face,tool))continue;
   // Check both root and neighboring harvest permissions, including DENY results.
   {var e=new PlayerInteractEvent.RightClickBlock(player,hand,pos,new BlockRayTraceResult(Vector3d.func_237489_a_(pos),face,pos,false));MinecraftForge.EVENT_BUS.post(e);if(e.isCanceled()||e.getUseBlock()==Event.Result.DENY||e.getUseItem()==Event.Result.DENY)continue;}
   for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++)for(int dz=-1;dz<=1;dz++)if(dx!=0||dy!=0||dz!=0){var n=pos.func_177982_a(dx,dy,dz);if(Math.abs(n.func_177958_n()-start.func_177958_n())<=8&&Math.abs(n.func_177956_o()-start.func_177956_o())<=8&&Math.abs(n.func_177952_p()-start.func_177952_p())<=8&&seen.add(n))queue.addLast(n);}
   boolean berry=state.func_177230_c() instanceof BerryLeavesBlock;var age=berry?BerryLeavesBlock.AGE:ApricornLeavesBlock.AGE;if(state.func_177229_b(age)<2)continue;
   ItemStack fruit=berry?new ItemStack(((BerryLeavesAccess)state.func_177230_c()).backport$berry().getBerryItem()):new ItemStack(PixelmonItems.getApricorn(((ApricornLeavesAccess)state.func_177230_c()).backport$apricorn()));
   fruit=fruit.func_77946_l();if(fortune>0)fruit.func_190920_e(1+Math.max(0,player.func_70681_au().nextInt(Math.min(3,fortune)+2)-1));
   if(!berry){var e=new ApricornEvent.Pick(world,state,((ApricornLeavesAccess)state.func_177230_c()).backport$apricorn(),pos,player,fruit);if(Pixelmon.EVENT_BUS.post(e))continue;fruit=e.getPickedStack();}
   if(fruit.func_190926_b()||world.func_180495_p(pos)!=state)continue;
   if(!world.func_180501_a(pos,state.func_206870_a(age,0),3))continue;
   harvested++;var remainder=fruit.func_77946_l();player.field_71071_by.func_70441_a(remainder);
   if(!remainder.func_190926_b()){var drop=new ItemEntity(world,player.func_226277_ct_(),player.func_226278_cu_(),player.func_226281_cx_(),remainder);drop.func_174869_p();world.func_217376_c(drop);}
  }
  return harvested;
 }
}
