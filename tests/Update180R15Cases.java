package qa;
import java.util.*;
import local.pixelmon.backport.*;
import net.minecraft.entity.player.*;
import net.minecraft.inventory.*;
import net.minecraft.inventory.container.*;
import net.minecraft.item.*;
import net.minecraft.item.crafting.*;
import net.minecraft.tileentity.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.registry.Registry;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

public final class Update180R15Cases {
 static volatile int stage;static int clientStage,wait;static ServerPlayerEntity player;static ICurioStacksHandler head;static float armor;
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 static ItemStack item(String id){return Update180R11Cases.item(id);}
 public static void server(ServerPlayerEntity p)throws Exception{
  player=p;p.func_71033_a(net.minecraft.world.GameType.CREATIVE);recipes(p);furnaces(p);curios(p);
  System.out.println("R15_SERVER_PASS native material crafting, cooking recipes/results/timing, vanilla and Iron Furnaces ticks, actual Curios slot and zero armor");
 }
 static void recipes(ServerPlayerEntity p){
  var recipes=p.func_184102_h().func_199529_aN();var grid=new CraftingInventory(new Container(null,997){public boolean func_75145_c(PlayerEntity p){return true;}},3,3);
  var furnace=(ICraftingRecipe)recipes.func_215367_a(new ResourceLocation("minecraft:furnace")).orElseThrow();
  for(String stone:List.of("pixelmon:ultra_space_stone","minecraft:cobblestone","minecraft:blackstone")){for(int i=0;i<9;i++)grid.func_70299_a(i,i==4?ItemStack.field_190927_a:item(stone));check(furnace.func_77569_a(grid,p.field_70170_p),"Native furnace accepts old/new stone "+stone);}
  var lever=(ICraftingRecipe)recipes.func_215367_a(new ResourceLocation("minecraft:lever")).orElseThrow();grid.func_174888_l();grid.func_70299_a(0,item("minecraft:stick"));grid.func_70299_a(3,item("pixelmon:ultra_space_stone"));check(lever.func_77569_a(grid,p.field_70170_p),"Direct cobblestone ingredient normalized for native lever");
  var pick=(ICraftingRecipe)recipes.func_215367_a(new ResourceLocation("minecraft:stone_pickaxe")).orElseThrow();grid.func_174888_l();for(int i=0;i<3;i++)grid.func_70299_a(i,item("pixelmon:ultra_space_stone"));grid.func_70299_a(4,item("minecraft:stick"));grid.func_70299_a(7,item("minecraft:stick"));check(pick.func_77569_a(grid,p.field_70170_p),"Native stone tools accept Ultra Space stone");
  var gilded=(AbstractCookingRecipe)recipes.func_215367_a(new ResourceLocation(Content.NS,"crystal_from_ultra_desert_gilded_sand")).orElseThrow();var blast=(AbstractCookingRecipe)recipes.func_215367_a(new ResourceLocation(Content.NS,"crystal_from_ultra_desert_gilded_sand_blasting")).orElseThrow();var glass=(AbstractCookingRecipe)recipes.func_215367_a(new ResourceLocation(Content.NS,"glass_from_ultra_desert_sand")).orElseThrow();
  var iron=(AbstractCookingRecipe)recipes.func_215367_a(new ResourceLocation("minecraft:iron_ingot")).orElseThrow();var ironBlast=(AbstractCookingRecipe)recipes.func_215367_a(new ResourceLocation("minecraft:iron_ingot_from_blasting")).orElseThrow();check(gilded.func_222137_e()==iron.func_222137_e()&&gilded.func_222137_e()==200,"Native iron and gilded smelting time200");check(blast.func_222137_e()==ironBlast.func_222137_e()&&blast.func_222137_e()==100,"Native iron and gilded blast time100");check(glass.func_77571_b().func_190916_E()==2&&glass.func_222137_e()==200,"Two glass per input, normal sand timing");
  var inventory=new Inventory(item("pixelmon:ultra_desert_sand"));check(recipes.func_215371_a(IRecipeType.field_222151_c,inventory,p.field_70170_p).isEmpty(),"No Ultra Desert sand blasting recipe from any loaded mod");
  for(var r:List.of(gilded,blast,glass)){var buffer=new net.minecraft.network.PacketBuffer(io.netty.buffer.Unpooled.buffer());try{((IRecipeSerializer)r.func_199559_b()).func_199427_a_(buffer,r);var decoded=(AbstractCookingRecipe)r.func_199559_b().func_199426_a_(r.func_199560_c(),buffer);check(ItemStack.func_77989_b(r.func_77571_b(),decoded.func_77571_b())&&r.func_222137_e()==decoded.func_222137_e(),"Real recipe network count/time preserved");}finally{buffer.release();}}
  System.out.println("R15_RECIPE_PASS furnace/lever/stone tool native recipes, original stones retained, 200/100 iron timing, exact two-glass output, no blast recipe, network result count");
 }
 static int cook(ServerPlayerEntity p,String block,String input,int inputs,String output,int expected,int maxTicks,int offset){
  var pos=new BlockPos(60+offset*3,76,50);var world=p.func_71121_q();world.func_180501_a(pos,ForgeRegistries.BLOCKS.getValue(new ResourceLocation(block)).func_176223_P(),3);var tile=world.func_175625_s(pos);check(tile instanceof IInventory&&tile instanceof ITickableTileEntity,"Real furnace tile "+block);var inv=(IInventory)tile;var stack=item(input);stack.func_190920_e(inputs);inv.func_70299_a(0,stack);var coal=item("minecraft:coal");coal.func_190920_e(8);inv.func_70299_a(1,coal);int first=-1;
  for(int tick=1;tick<=maxTicks;tick++){((ITickableTileEntity)tile).func_73660_a();if(first<0&&!inv.func_70301_a(2).func_190926_b())first=tick;}
  var result=inv.func_70301_a(2);check(result.func_190916_E()==expected,"Actual "+block+" produces exact count="+expected+" got="+result.func_190916_E());if(expected>0)check(result.func_77973_b().getRegistryName().toString().equals(output)&&inv.func_70301_a(0).func_190926_b(),"Actual output/input consumption "+block);else check(inv.func_70301_a(0).func_190916_E()==inputs,"Rejected blast input not consumed");
  world.func_180501_a(pos,net.minecraft.block.Blocks.field_150350_a.func_176223_P(),3);return first;
 }
 static void furnaces(ServerPlayerEntity p){
  int iron=cook(p,"minecraft:furnace","minecraft:iron_ore",1,"minecraft:iron_ingot",1,205,0),gilded=cook(p,"minecraft:furnace","pixelmon:ultra_desert_gilded_sand",1,"pixelmon:crystal",1,205,1);check(iron==gilded,"Actual vanilla iron/gilded tick count identical");
  int ironBlast=cook(p,"minecraft:blast_furnace","minecraft:iron_ore",1,"minecraft:iron_ingot",1,105,2),gildedBlast=cook(p,"minecraft:blast_furnace","pixelmon:ultra_desert_gilded_sand",1,"pixelmon:crystal",1,105,3);check(ironBlast==gildedBlast,"Actual blast iron/gilded ticks identical");
  cook(p,"minecraft:furnace","pixelmon:ultra_desert_sand",2,"minecraft:glass",4,405,4);cook(p,"minecraft:blast_furnace","pixelmon:ultra_desert_sand",2,"minecraft:glass",0,405,5);
  cook(p,"ironfurnaces:iron_furnace","pixelmon:ultra_desert_sand",2,"minecraft:glass",4,405,6);cook(p,"ironfurnaces:iron_furnace","pixelmon:ultra_desert_gilded_sand",1,"pixelmon:crystal",1,205,7);
  System.out.println("R15_FURNACE_PASS vanilla iron/gilded ticks="+iron+" blast="+ironBlast+"; 2 inputs to4glass, no blasting, Iron Furnaces actual consecutive output4glass and crystal");
 }
 static void curios(ServerPlayerEntity p){
  head=CuriosApi.getCuriosHelper().getCuriosHandler(p).resolve().orElseThrow().getStacksHandler("head").orElseThrow();check(head.getStacks().getSlots()>0,"Actual head slot registered");var slot=new top.theillusivec4.curios.common.inventory.CurioSlot(p,head.getStacks(),0,"head",0,0,head.getRenders());check(slot.func_75214_a(item("pixelmon:safety_goggles")),"Native CurioSlot accepts safety goggles");check(!slot.func_75214_a(item("minecraft:cobblestone")),"Random item not accepted as head curio");
  head.getStacks().setStackInSlot(0,ItemStack.field_190927_a);armor=p.func_70658_aO();slot.func_75215_d(item("pixelmon:safety_goggles"));check(GogglesCurios.goggles(GogglesCurios.head(p))&&p.func_70658_aO()==armor,"Real Curios equipped head found without armor increase");
  var stored=CuriosApi.getCuriosHelper().getCuriosHandler(p).resolve().orElseThrow().writeNBT();check(stored.toString().contains("safety_goggles"),"Curios native save contains goggles");stage=1;System.out.println("R15_CURIOS_SERVER_PASS actual head slot insertion, unrelated item rejected, zero armor, persisted slot NBT");
 }
 static net.minecraftforge.client.event.EntityViewRenderEvent.FogDensity fog(){var mc=net.minecraft.client.Minecraft.func_71410_x();var info=new net.minecraft.client.renderer.ActiveRenderInfo();info.func_216772_a(mc.field_71441_e,mc.field_71439_g,false,false,0f);return new net.minecraftforge.client.event.EntityViewRenderEvent.FogDensity(net.minecraft.client.renderer.FogRenderer.FogType.FOG_TERRAIN,info,0f,.15f);}
 static boolean distance()throws Exception{return ((net.minecraft.client.world.DimensionRenderInfo)Class.forName("com.pixelmonmod.pixelmon.world.dimension.ultraspace.UltraSpace$UltraSpaceRenderInfo").getConstructor().newInstance()).func_230493_a_(0,0);}
 public static void client(int age)throws Exception{
  var mc=net.minecraft.client.Minecraft.func_71410_x();if(++wait>700)throw new AssertionError("Client Curios/fog stages timed out "+clientStage);
  if(clientStage==0&&stage==1&&GogglesCurios.goggles(GogglesCurios.head(mc.field_71439_g))){mc.func_147108_a(null);check(!distance(),"Actual native fog-distance hook recognizes synced head curio");clientStage=1;mc.func_71401_C().func_213167_f(()->{var ultra=player.func_184102_h().func_71218_a(net.minecraft.util.RegistryKey.func_240903_a_(Registry.field_239699_ae_,new ResourceLocation("pixelmon:ultra_space")));player.func_200619_a(ultra,16,90,16,180,0);});}
  if(clientStage==1&&mc.field_71441_e.func_234923_W_().func_240901_a_().toString().equals("pixelmon:ultra_space")&&GogglesCurios.goggles(GogglesCurios.head(mc.field_71439_g))){var event=fog();new com.pixelmonmod.pixelmon.world.listener.UltraSpaceClientListener().onRenderFog(event);check(event.isCanceled()&&event.getDensity()==-1f&&!distance(),"Both native Ultra Space fog paths match helmet effect from curio");clientStage=2;mc.func_71401_C().func_213167_f(()->head.getStacks().setStackInSlot(0,ItemStack.field_190927_a));}
  if(clientStage==2&&GogglesCurios.head(mc.field_71439_g).func_190926_b()){var event=fog();new com.pixelmonmod.pixelmon.world.listener.UltraSpaceClientListener().onRenderFog(event);check(event.getDensity()!=-1f&&distance(),"Removing synced head curio restores native fog");languageAndJei();clientStage=3;System.out.println("R15_CURIOS_CLIENT_PASS real server-to-client head equip/unequip, native Ultra Space fog density/distance enabled and restored");}
  if(clientStage>=3){if(clientStage==3&&age%10==0){Update180R9Cases.shot("qa-r15-materials.png");clientStage=4;}if(age>200)mc.func_71400_g();}
 }
 static void languageAndJei()throws Exception{
  for(String[] pair:new String[][]{{"blue","天蓝玉石原矿"},{"red","玉石原矿"},{"black","黑玉石原矿"}}){String key="block.pixelmon."+pair[0]+"_tumblestone";check(net.minecraft.client.resources.I18n.func_135052_a(key).equals(pair[1]),"Final language after external resource pack "+key);var block=ForgeRegistries.BLOCKS.getValue(new ResourceLocation("pixelmon:"+pair[0]+"_tumblestone"));check(new net.minecraft.util.text.TranslationTextComponent(block.func_149739_a()).getString().equals(pair[1]),"Actual placed block/Jade source name Chinese");}
  for(String[] pair:new String[][]{{"tumblestone","玉石"},{"sky_tumblestone","天蓝玉石"},{"black_tumblestone","黑玉石"},{"raw_tumblestone","未加工的玉石"},{"raw_sky_tumblestone","未加工的天蓝玉石"},{"raw_black_tumblestone","未加工的黑玉石"}})check(item("pixelmon:"+pair[0]).func_200301_q().getString().equals(pair[1]),"Actual material item/JEI Chinese stays distinct from ore block "+pair[0]);
  var f=local.pixelmon.backport.client.LootSourceJei.class.getDeclaredField("runtime");f.setAccessible(true);var runtime=(mezz.jei.api.runtime.IJeiRuntime)f.get(null);check(runtime!=null,"Real JEI runtime loaded");var manager=runtime.getRecipeManager();var normal=(mezz.jei.api.recipe.category.IRecipeCategory<AbstractCookingRecipe>)manager.getRecipeCategory(mezz.jei.api.constants.VanillaRecipeCategoryUid.FURNACE,false);var blast=(mezz.jei.api.recipe.category.IRecipeCategory<AbstractCookingRecipe>)manager.getRecipeCategory(mezz.jei.api.constants.VanillaRecipeCategoryUid.BLASTING,false);check(normal!=null&&blast!=null,"Actual JEI furnace/blasting categories");check(manager.getRecipes(normal).stream().anyMatch(r->r.func_199560_c().toString().equals(Content.NS+":glass_from_ultra_desert_sand")&&r.func_77571_b().func_190916_E()==2),"Actual JEI two-glass recipe/count");check(manager.getRecipes(normal).stream().anyMatch(r->r.func_199560_c().toString().equals(Content.NS+":crystal_from_ultra_desert_gilded_sand")),"Actual JEI crystal smelting recipe");check(manager.getRecipes(blast).stream().anyMatch(r->r.func_199560_c().toString().equals(Content.NS+":crystal_from_ultra_desert_gilded_sand_blasting")),"Actual JEI crystal blasting recipe");runtime.getRecipesGui().show(manager.createFocus(mezz.jei.api.recipe.IFocus.Mode.INPUT,item("pixelmon:ultra_desert_sand")));System.out.println("R15_LANGUAGE_JEI_PASS all3 native item/block titles translated after external language pack, JEI runtime loaded");
 }
}
