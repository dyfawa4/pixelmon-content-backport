package qa;
import java.util.*;
import local.pixelmon.backport.*;
import local.pixelmon.backport.client.LootSourceJei;
import net.minecraft.util.ResourceLocation;
import net.minecraft.network.PacketBuffer;
import io.netty.buffer.Unpooled;
import net.minecraft.item.ItemStack;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.category.IRecipeCategory;

/** Native loaded catalogue, actual packet codec and real JEI runtime; no loot rolls. */
public final class Update180LootNameCases {
 static List<LootSourceCatalog.Source> sources;static int wrappers;
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 static boolean shop(String id){return id.startsWith("pixelmon_backport:chests/pokemart/");}
 public static void server(net.minecraft.entity.player.ServerPlayerEntity player)throws Exception{
  sources=LootSourceCatalog.build(player.func_184102_h());var names=Content.read("backport/loot_source_names.json").getAsJsonObject("tables");int named=0,entries=0;
  for(var source:sources){if(names.has(source.id)){check(source.title.equals(names.get(source.id).getAsString()),"Exact Chinese title "+source.id);named++;}
   if(shop(source.id)){wrappers++;check(source.title.startsWith("村庄道具商店·")&&!source.title.contains("pixelmon_backport"),"Shop never exposes path title");for(String id:UtilityItemLoot.ITEMS){var e=source.entries.stream().filter(v->v.stack.func_77973_b().getRegistryName().toString().equals(id)).findFirst().orElseThrow();check(e.note.contains("5.0%"),"Existing5percent utility chance preserved");}}
  }
  check(wrappers==10,"All10 live shop wrappers translated");check(named>=58,"Original source title coverage retained");
  for(int start=0;start<sources.size();start+=4){var rows=sources.subList(start,Math.min(sources.size(),start+4));var batch=new LootSourceNetwork.Batch(UUID.randomUUID(),start/4,(sources.size()+3)/4,rows);var buffer=new PacketBuffer(Unpooled.buffer());try{LootSourceNetwork.Batch.encode(batch,buffer);var decoded=LootSourceNetwork.Batch.decode(buffer);for(int i=0;i<rows.size();i++){var a=rows.get(i);var b=decoded.sources.get(i);check(a.title.equals(b.title)&&a.id.equals(b.id)&&a.detail.equals(b.detail)&&a.entries.size()==b.entries.size(),"Title/id/detail batch roundtrip");for(int j=0;j<a.entries.size();j++){check(ItemStack.func_77989_b(a.entries.get(j).stack,b.entries.get(j).stack)&&a.entries.get(j).note.equals(b.entries.get(j).note),"NBT/quantity/conditions unchanged");entries++;}}check(buffer.readableBytes()==0,"No trailing packet bytes");}finally{buffer.release();}}
  // Simulate an older server's unlocalized title directly on the native wire.
  var sample=sources.stream().filter(v->shop(v.id)).findFirst().orElseThrow();var buffer=new PacketBuffer(Unpooled.buffer());try{buffer.func_179252_a(UUID.randomUUID());buffer.writeInt(0);buffer.writeInt(1);buffer.writeInt(1);buffer.func_180714_a(sample.group);buffer.func_180714_a(sample.id);buffer.func_180714_a("pixelmon_backport · pokemart/minecraft/chests/village/village_tannery");buffer.func_180714_a(sample.detail);buffer.writeInt(sample.entries.size());for(var e:sample.entries){buffer.func_150788_a(e.stack);buffer.func_180714_a(e.note);}var decoded=LootSourceNetwork.Batch.decode(buffer);check(decoded.sources.get(0).title.equals(sample.title),"Client decode repairs raw title from older server");}finally{buffer.release();}
  var future=new LootSourceCatalog.Source("structures","pixelmon_backport:chests/pokemart/minecraft/chests/shipwreck_treasure","raw","",List.of());check(future.title.equals("村庄道具商店·沉船·宝箱"),"Unlisted wrapper inherits known base Chinese title");
  var foreign=new LootSourceCatalog.Source("structures","othermod:chests/example","External custom title","",List.of());check(foreign.title.equals("External custom title"),"Unknown external source title retained");
  System.out.println("R13_LOOT_NAMES_SERVER_PASS "+sources.size()+" sources; "+named+" known Chinese titles including10shops; "+entries+" item/NBT/condition roundtrips; old-server title decode and future wrapper fallback");
 }
 public static void client(int age)throws Exception{
  var mc=net.minecraft.client.Minecraft.func_71410_x();
  if(age==20)mc.func_147108_a(null);
  if(age==65){var f=LootSourceJei.class.getDeclaredField("runtime");f.setAccessible(true);var runtime=(IJeiRuntime)f.get(null);check(runtime!=null,"Actual JEI runtime");var manager=runtime.getRecipeManager();var category=(IRecipeCategory<LootSourceJei.Page>)manager.getRecipeCategory(LootSourceJei.uid("structures"),false);check(category!=null,"Native structures category");int count=0;for(var page:manager.getRecipes(category)){if(shop(page.source.id)){count++;check(page.source.title.startsWith("村庄道具商店·"),"Actual JEI title translated");var layout=manager.createRecipeLayoutDrawable(category,page,manager.createFocus(IFocus.Mode.INPUT,LootSourceJei.icon("structures")));check(layout!=null,"Actual JEI shop layout");var tip=category.getTooltipStrings(page,25,5);check(tip.get(0).getString().equals(page.source.title),"Hover title Chinese");check(tip.get(tip.size()-1).getString().equals(page.source.id),"Raw diagnostic ID retained in tooltip only");}}
   check(count>=10,"Every shop page installed in actual JEI");for(String item:UtilityItemLoot.ITEMS){var focus=manager.createFocus(IFocus.Mode.OUTPUT,Update180R11Cases.item(item));var pages=manager.getRecipes(category,focus,false);check(pages.stream().filter(v->shop(v.source.id)).map(v->v.source.id).distinct().count()==10,"R lookup10 translated shops "+item);}
   runtime.getRecipesGui().show(manager.createFocus(IFocus.Mode.OUTPUT,Update180R11Cases.item("pixelmon_backport:moving_agreement")));System.out.println("R13_LOOT_NAMES_CLIENT_PASS real JEI pages and hover titles Chinese; both utility R-lookups10shops, raw tooltip IDs retained");
  }
  if(age==90)Update180R9Cases.shot("qa-r13-loot-names.png");
  if(age>105)mc.func_71400_g();
 }
}
