package qa;
import java.nio.file.*;import java.util.*;import java.lang.reflect.*;
import net.minecraftforge.fml.common.Mod;import net.minecraftforge.eventbus.api.SubscribeEvent;import net.minecraftforge.event.TickEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.item.*;import net.minecraft.enchantment.*;import net.minecraft.util.*;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.*;
import com.pixelmonmod.pixelmon.entities.projectiles.HookEntity;
import com.pixelmonmod.pixelmon.enums.items.EnumRodType;
import com.pixelmonmod.pixelmon.api.spawning.*;
import com.pixelmonmod.pixelmon.api.spawning.archetypes.spawners.TriggerSpawner;
import com.pixelmonmod.pixelmon.api.spawning.archetypes.entities.items.SpawnInfoItem;
import com.pixelmonmod.pixelmon.api.spawning.archetypes.entities.pokemon.SpawnInfoPokemon;
import com.pixelmonmod.pixelmon.api.spawning.conditions.SpawnCondition;
import com.pixelmonmod.pixelmon.api.world.MutableLocation;
import com.pixelmonmod.pixelmon.spawning.PixelmonSpawning;
import local.pixelmon.backport.FishingEnchantments;
@Mod("backport_fishing_probe") @Mod.EventBusSubscriber(modid="backport_fishing_probe")
public class Fishing110Probe {
 static boolean opened,hidden,done,serverReady;static int ticks,checks;static boolean cancelSpawn;
 static List<SpawnInfo> pool=List.of();
 static void check(boolean yes,String why){checks++;if(!yes)throw new AssertionError(why);}
 static ItemStack stack(String id){return new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(id)));}
 static Enchantment ench(String id){return ForgeRegistries.ENCHANTMENTS.getValue(new ResourceLocation("minecraft",id));}
 static ItemStack rod(String id,int total){var r=stack("pixelmon:"+id);for(String name:List.of("luck_of_the_sea","fortune","looting")){int n=Math.min(3,total);if(n>0)r.func_77966_a(ench(name),n);total-=n;}return r;}
 static void field(Object obj,String name,Object value)throws Exception{var f=HookEntity.class.getDeclaredField(name);f.setAccessible(true);f.set(obj,value);}
 static HookEntity hook(ServerPlayerEntity p,ItemStack r){var h=new HookEntity(p.field_70170_p,p,((com.pixelmonmod.pixelmon.items.FishingRodItem)r.func_77973_b()).getRodType(),-1);h.func_70012_b(p.func_226277_ct_()+1,p.func_226278_cu_(),p.func_226281_cx_(),0,0);p.field_71104_cf=h;return h;}
 static SpawnLocation location(ServerPlayerEntity p){return new SpawnLocation(p,new MutableLocation(p.field_70170_p,p.func_233580_cy_()),new HashSet<>(),net.minecraft.block.Blocks.field_150355_j,new HashSet<>(),p.field_70170_p.func_226691_t_(p.func_233580_cy_()),true,15,12);}
 static SpawnInfoItem item(float weight){var v=new SpawnInfoItem();v.rarity=weight;v.condition=new SpawnCondition();v.item=new com.pixelmonmod.pixelmon.api.item.JsonItemStack(Items.field_151045_i);v.item.quantity=1;v.itemStack=stack("minecraft:brick");return v;}
 static ItemStack book(ItemStack rod,String id,int level,ServerPlayerEntity p){var c=new RepairContainer(31,p.field_71071_by);var b=new ItemStack(Items.field_151134_bR);EnchantedBookItem.func_92115_a(b,new EnchantmentData(ench(id),level));c.func_75139_a(0).func_75215_d(rod);c.func_75139_a(1).func_75215_d(b);c.func_82848_d();return c.func_75139_a(2).func_75211_c().func_77946_l();}
 static void run(ServerPlayerEntity p)throws Exception{
  boolean creative=p.field_71075_bZ.field_75098_d;p.field_71075_bZ.field_75098_d=false;
  var original=PixelmonSpawning.fishingSpawner;
  var test=new TriggerSpawner("qa-fishing"){public List<SpawnInfo> getSuitableSpawns(SpawnLocation l){return pool;}};
  try{
   for(String id:List.of("old_rod","good_rod","super_rod")){
    var r=rod(id,0);check(r.func_77958_k()==(id.equals("old_rod")?32:id.equals("good_rod")?64:128),"Native max durability unchanged "+id);
    for(String name:List.of("fortune","looting")){check(ench(name).func_92089_a(r),"Anvil applicability "+id+name);check(!ench(name).canApplyAtEnchantingTable(r),"Not enchanting-table eligible "+id+name);}
    for(int seed=0;seed<150;seed++)for(var d:EnchantmentHelper.func_77513_b(new Random(seed),r,30,false))check(!FishingEnchantments.bookOnly(d.field_76302_b),"Actual table roll excludes fortune/looting "+id);
    for(var order:List.of(List.of("fortune","looting","luck_of_the_sea"),List.of("luck_of_the_sea","looting","fortune"),List.of("looting","fortune","luck_of_the_sea"))){var combined=r.func_77946_l();for(String name:order){combined=book(combined,name,3,p);check(!combined.func_190926_b(),"Actual survival anvil allows "+name+" on "+id);}check(FishingEnchantments.total(combined)==9,"Native anvil coexists III+III+III "+id);}
    var tableLuck=false;for(int seed=0;seed<500;seed++)for(var d:EnchantmentHelper.func_77513_b(new Random(seed),r,30,false))if(d.field_76302_b==ench("luck_of_the_sea"))tableLuck=true;check(tableLuck,"Sea luck remains obtainable in enchanting table "+id);
    var nine=rod(id,9);nine.func_196082_o().func_74768_a("rodQuality",5);nine.func_77966_a(ench("mending"),1);nine.func_77966_a(ench("vanishing_curse"),1);var grind=new GrindstoneContainer(32,p.field_71071_by);grind.func_75139_a(0).func_75215_d(nine);var clean=grind.func_75139_a(2).func_75211_c();check(FishingEnchantments.total(clean)==0&&EnchantmentHelper.func_77506_a(ench("mending"),clean)==0&&EnchantmentHelper.func_77506_a(ench("vanishing_curse"),clean)==1&&clean.func_77978_p().func_74762_e("rodQuality")==5,"Grindstone/quality/curse unchanged "+id);
   }
   var rare=item(4);var common=item(100);var disabled=item(0);var loc=location(p);pool=List.of(rare,common,disabled);PixelmonSpawning.fishingSpawner=test;
   for(int total=0;total<=9;total++){
    var r=rod("super_rod",total);p.func_184611_a(Hand.MAIN_HAND,r);p.func_184611_a(Hand.OFF_HAND,ItemStack.field_190927_a);var h=hook(p,r);check(rare.getAdjustedRarity(test,loc)==4,"Old Luck rarity multiplier not stacked "+total);
    int count=0,expected=0;long seed=20991030L+total;p.func_70681_au().setSeed(seed);var rng=new Random(seed);
    for(int draw=0;draw<3000;draw++){var got=test.getWeightedSpawnInfo(loc);check(got==rare||got==common,"Real spawner chooses only eligible positive-weight entries");if(got==rare)count++;if(rng.nextInt(10)<total)expected++;}
    if(total>0)check(count==expected,"Real spawner exact deterministic "+total+"/10 probability actual="+count+" expected="+expected);else check(count>0&&count<300,"Zero preserves native rare encounters, no bonus count="+count);
    check(FishingEnchantments.catchDamage(r)==(total==0?0:total+1),"Catch cost "+total);h.func_70106_y();p.field_71104_cf=null;
   }
   // All three enchantments independently add the same probability/cost, not just Sea Luck.
   for(String name:List.of("fortune","looting","luck_of_the_sea")){var r=rod("super_rod",0);r.func_77966_a(ench(name),3);p.func_184611_a(Hand.MAIN_HAND,r);var h=hook(p,r);check(FishingEnchantments.total(r)==3&&FishingEnchantments.catchDamage(r)==4,"Independent enchant "+name);h.func_70106_y();}
   // Native bobber tick selects the action and renders marks from that action's real rarity.
   pool=List.of(rare,common);int triples=0;loc.diameter=12;var waterPos=p.func_233580_cy_().func_177982_a(1,0,0);var previousWater=p.field_70170_p.func_180495_p(waterPos);p.field_70170_p.func_180501_a(waterPos,net.minecraft.block.Blocks.field_150355_j.func_176223_P(),2);
   for(int i=0;i<200;i++){
    var markedRod=rod("super_rod",9);p.func_184611_a(Hand.MAIN_HAND,markedRod);var marked=new HookEntity(p.field_70170_p,p,EnumRodType.SuperRod,-1){public SpawnLocation createSpawnLocation(){return loc;}};p.field_71104_cf=marked;marked.func_70012_b(p.func_226277_ct_()+1,p.func_226278_cu_(),p.func_226281_cx_(),0,0);
    field(marked,"state",HookEntity.HookState.BOBBING);field(marked,"ticksTillCatch",1);marked.func_70071_h_();var caught=HookEntity.class.getDeclaredField("caught");caught.setAccessible(true);var action=(SpawnAction<?>)caught.get(marked);check(action!=null,"Native tick caught a real candidate");int marks=marked.func_184212_Q().func_187225_a(HookEntity.DATA_HOOK_STATE);boolean special=action.spawnInfo.calculateNominalRarity()<5;check((marks==3)==special,"Native !!! matches actual rare catch, never relabeled common");if(special)triples++;
    field(marked,"ticksTillEscape",1);field(marked,"ticksTillCatch",100);marked.func_70071_h_();check(caught.get(marked)==null,"Actual escape clears pending catch");check(marked.func_146034_e(markedRod)==0&&markedRod.func_77952_i()==0,"Actual escaped catch costs no durability");marked.func_70106_y();p.field_71104_cf=null;
   }p.field_70170_p.func_180501_a(waterPos,previousWater,2);check(triples>160&&triples<198,"Native marked special event frequency at combined nine: "+triples);
   var r=rod("super_rod",9);p.func_184611_a(Hand.MAIN_HAND,r);var h=hook(p,r);pool=List.of(common,disabled);for(int i=0;i<100;i++)check(test.getWeightedSpawnInfo(loc)==common,"Absent rare pool does not invent encounters");pool=List.of(disabled);check(test.getWeightedSpawnInfo(loc)==null,"Zero-weight-only pool stays empty");
   // Nonfishing spawners cannot acquire the fishing bonus.
   pool=List.of(rare,common);var non=new TriggerSpawner("qa-unrelated"){public List<SpawnInfo> getSuitableSpawns(SpawnLocation l){return pool;}};int unrelated=0;for(int i=0;i<3000;i++)if(non.getWeightedSpawnInfo(loc)==rare)unrelated++;check(unrelated<300,"Nonfishing spawns retain native weights");h.func_70106_y();p.field_71104_cf=null;
   // Actual HookEntity reeling and actual FishingRodItem use, no substitute damage call.
   for(String id:List.of("old_rod","good_rod","super_rod"))for(int total=0;total<=9;total++){
    r=rod(id,total);p.func_184611_a(Hand.MAIN_HAND,r);h=hook(p,r);field(h,"caught",common.construct(test,loc));int before=p.field_71071_by.field_70462_a.stream().filter(x->x.func_77973_b()==stack("minecraft:brick").func_77973_b()).mapToInt(ItemStack::func_190916_E).sum();r.func_77973_b().func_77659_a(p.field_70170_p,p,Hand.MAIN_HAND);int after=p.field_71071_by.field_70462_a.stream().filter(x->x.func_77973_b()==stack("minecraft:brick").func_77973_b()).mapToInt(ItemStack::func_190916_E).sum();check(after==before+1,"Native caught item delivered "+id+total);check(r.func_77952_i()==(total==0?0:total+1),"Actual successful use durability "+id+total);
    var empty=rod(id,total);p.func_184611_a(Hand.MAIN_HAND,empty);h=hook(p,empty);var ground=net.minecraft.entity.Entity.class.getDeclaredField("field_70122_E");ground.setAccessible(true);ground.setBoolean(h,true);empty.func_77973_b().func_77659_a(p.field_70170_p,p,Hand.MAIN_HAND);check(empty.func_77952_i()==0,"Empty/on-ground reeling costs zero "+id+total);
   }
   r=rod("super_rod",9);p.func_184611_a(Hand.MAIN_HAND,r);h=hook(p,r);cancelSpawn=true;field(h,"caught",common.construct(test,loc));r.func_77973_b().func_77659_a(p.field_70170_p,p,Hand.MAIN_HAND);check(r.func_77952_i()==0,"Canceled spawn costs zero");cancelSpawn=false;h.func_70106_y();p.field_71104_cf=null;
   var party=com.pixelmonmod.pixelmon.api.storage.StorageProxy.getParty(p);var saved=new com.pixelmonmod.pixelmon.api.pokemon.Pokemon[6];for(int i=0;i<6;i++){saved[i]=party.get(i);party.set(i,null);}
   try{var fish=new SpawnInfoPokemon();fish.set=new SpawnSet();fish.condition=new SpawnCondition();fish.setPokemon(com.pixelmonmod.api.pokemon.PokemonSpecificationProxy.create("Magikarp"));fish.minLevel=fish.maxLevel=5;fish.spawnSpecificShinyRate=0f;fish.spawnSpecificBossRate=0f;fish.spawnSpecificPokerusRate=0f;var action=fish.construct(test,loc);r=rod("super_rod",7);p.func_184611_a(Hand.MAIN_HAND,r);h=hook(p,r);field(h,"caught",action);r.func_77973_b().func_77659_a(p.field_70170_p,p,Hand.MAIN_HAND);check(action.getOrCreateEntity().func_70089_S()&&r.func_77952_i()==8,"Real native Pokemon catch also costs eight for seven levels");action.getOrCreateEntity().func_70106_y();
   var able=com.pixelmonmod.pixelmon.api.pokemon.PokemonFactory.create(com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies.fromNationalDex(25));able.setLevelNum(10);able.initialize();party.set(0,able);var main=rod("super_rod",0);var off=rod("super_rod",9);p.func_184611_a(Hand.MAIN_HAND,main);p.func_184611_a(Hand.OFF_HAND,off);p.field_71104_cf=null;off.func_77973_b().func_77659_a(p.field_70170_p,p,Hand.OFF_HAND);check(p.field_71104_cf instanceof HookEntity&&FishingEnchantments.rod(p,(HookEntity)p.field_71104_cf)==off,"Actual offhand cast does not borrow mainhand rod enchantments");field(p.field_71104_cf,"caught",common.construct(test,loc));main.func_77973_b().func_77659_a(p.field_70170_p,p,Hand.MAIN_HAND);check(main.func_77952_i()==0&&off.func_77952_i()==10,"Other-hand reel charges only the enchanted casting rod, no free catch or double charge");
   }finally{for(int i=0;i<6;i++)party.set(i,saved[i]);}
   // Offhand successful catch charges only the actual used rod.
   r=rod("super_rod",7);p.func_184611_a(Hand.MAIN_HAND,stack("minecraft:stick"));p.func_184611_a(Hand.OFF_HAND,r);h=hook(p,r);field(h,"caught",common.construct(test,loc));r.func_77973_b().func_77659_a(p.field_70170_p,p,Hand.OFF_HAND);check(r.func_77952_i()==8,"Offhand total seven costs eight");
   r=rod("super_rod",9);r.func_196085_b(119);p.func_184611_a(Hand.MAIN_HAND,r);h=hook(p,r);field(h,"caught",common.construct(test,loc));r.func_77973_b().func_77659_a(p.field_70170_p,p,Hand.MAIN_HAND);check(r.func_190926_b(),"Rod breaks normally when successful catch exceeds remaining durability");
   r=rod("super_rod",9);r.func_77966_a(ench("unbreaking"),3);int used=0;for(int i=0;i<10;i++){r.func_196085_b(0);p.func_184611_a(Hand.MAIN_HAND,r);h=hook(p,r);field(h,"caught",common.construct(test,loc));r.func_77973_b().func_77659_a(p.field_70170_p,p,Hand.MAIN_HAND);used+=r.func_77952_i();}check(used>0&&used<100,"Native Unbreaking reduces base cost used="+used);
   r=rod("super_rod",9);r.func_77966_a(ench("mending"),1);r.func_196085_b(30);p.func_184611_a(Hand.MAIN_HAND,r);p.func_184611_a(Hand.OFF_HAND,ItemStack.field_190927_a);p.field_71090_bL=0;var orb=new net.minecraft.entity.item.ExperienceOrbEntity(p.field_70170_p,p.func_226277_ct_(),p.func_226278_cu_(),p.func_226281_cx_(),5);orb.func_70100_b_(p);check(r.func_77952_i()<30,"Native XP Mending still repairs rod");
   // Existing Lure event remains effective.
   r=rod("super_rod",0);r.func_77966_a(ench("lure"),3);p.func_184611_a(Hand.MAIN_HAND,r);h=hook(p,r);h.setNewTimeToCatch();var wait=HookEntity.class.getDeclaredField("ticksTillCatch");wait.setAccessible(true);check(wait.getInt(h)>=25&&wait.getInt(h)<=75,"Native Lure cast still reduces wait");h.func_70106_y();p.field_71104_cf=null;
  }finally{cancelSpawn=false;p.field_71075_bZ.field_75098_d=creative;PixelmonSpawning.fishingSpawner=original;if(p.field_71104_cf!=null)p.field_71104_cf.func_70106_y();p.field_71104_cf=null;}
 }
 @SubscribeEvent public static void cancel(com.pixelmonmod.pixelmon.api.events.spawning.SpawnEvent e){if(cancelSpawn)e.setCanceled(true);}
 @SubscribeEvent public static void server(TickEvent.ServerTickEvent e){if(done||serverReady||e.phase!=TickEvent.Phase.END)return;var s=net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer();if(s==null||s.func_184103_al().func_181057_v().isEmpty()||++ticks<100)return;try{com.pixelmonmod.pixelmon.Pixelmon.EVENT_BUS.register(Fishing110Probe.class);run(s.func_184103_al().func_181057_v().get(0));serverReady=true;}catch(Throwable t){fail(t);}}
 static void fail(Throwable t){t.printStackTrace();try{Files.writeString(Path.of("fishing110-fail.txt"),t.toString());}catch(Exception ignored){}done=true;}
 @SubscribeEvent public static void client(TickEvent.ClientTickEvent e){if(e.phase!=TickEvent.Phase.END)return;var mc=net.minecraft.client.Minecraft.func_71410_x();if(done){mc.func_71400_g();return;}mc.field_71474_y.field_82881_y=false;if(!hidden){org.lwjgl.glfw.GLFW.glfwHideWindow(mc.func_228018_at_().func_198092_i());hidden=true;}if(!opened&&mc.field_71462_r instanceof net.minecraft.client.gui.screen.MainMenuScreen){opened=true;mc.func_238191_a_("backport-qa-single");}if(mc.field_71439_g!=null){com.pixelmonmod.pixelmon.client.ServerStorageDisplay.showStarterScreen=false;if(mc.field_71462_r!=null)mc.func_147108_a(null);}if(serverReady)try{
  for(int total:new int[]{0,7,9}){String text=rod("super_rod",total).func_82840_a(mc.field_71439_g,net.minecraft.client.util.ITooltipFlag.TooltipFlags.NORMAL).stream().map(t->t.getString()).collect(java.util.stream.Collectors.joining("\n"));check(text.contains("附魔书")&&text.contains(total==0?"不消耗耐久":total==7?"70":"90"),"Actual Chinese item tooltip "+total+" = "+text);if(total>0)check(text.contains("消耗"+(total+1)+"点耐久"),"Tooltip catch cost "+total);}
  Files.writeString(Path.of("fishing110-pass.txt"),"PASS "+checks+" native survival anvils, enchanting table/grindstone, exact 1-9 triple-mark pool selection, actual !!! matching caught action, zero/native odds, eligibility, successful Pokemon/item and empty/escaped/canceled reeling durability, offhand, breakage, Unbreaking, Mending, Lure and actual Chinese tooltips");done=true;
 }catch(Throwable t){fail(t);}}
}
