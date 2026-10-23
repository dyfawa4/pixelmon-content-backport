package local.pixelmon.backport;

import java.util.*;
import com.google.gson.*;
import com.pixelmonmod.pixelmon.api.pokemon.Element;
import com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.PokeBallRegistry;
import com.pixelmonmod.pixelmon.battles.raids.WeightedItemStacks;
import com.pixelmonmod.pixelmon.entities.npcs.registry.DropItemRegistry;
import net.minecraft.item.*;
import net.minecraft.loot.LootTableManager;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

/** Read-only catalogue. Never rolls loot, explores chunks, or consumes weighted pools. */
public final class LootSourceCatalog {
 public static final class Entry {
  public final ItemStack stack;public final String note;
  public Entry(ItemStack stack,String note){this.stack=stack.func_77946_l();this.note=note;}
 }
 public static final class Source {
  public final String group,id,title,detail;public final List<Entry> entries;
  public Source(String group,String id,String title,String detail,List<Entry> entries){this.group=group;this.id=id;this.title=title;this.detail=detail;this.entries=List.copyOf(entries);}
 }
 private static final JsonObject NAMES=Content.read("backport/loot_source_names.json");
 private LootSourceCatalog(){}
 private static String type(Element type){String name=type==null?"Any":type.toString();var value=NAMES.getAsJsonObject("types").get(name.toLowerCase(Locale.ROOT));return value==null?name:value.getAsString();}
 private static String title(ResourceLocation id){var name=NAMES.getAsJsonObject("tables").get(id.toString());return name==null?id.func_110624_b()+" · "+id.func_110623_a().replace("chests/","").replace('_',' '):name.getAsString();}
 private static ItemStack item(String id){var key=ResourceLocation.func_208304_a(id);var item=key==null?null:ForgeRegistries.ITEMS.getValue(key);return item==null||item==Items.field_190931_a?ItemStack.field_190927_a:new ItemStack(item);}
 private static void add(List<Entry> output,ItemStack stack,String note){if(stack!=null&&!stack.func_190926_b())output.add(new Entry(stack,note));}
 private static List<Entry> distinct(List<Entry> entries){var seen=new LinkedHashMap<String,Entry>();for(var e:entries)seen.putIfAbsent(e.stack.func_77973_b().getRegistryName()+":"+e.stack.func_77978_p()+":"+e.note,e);return new ArrayList<>(seen.values());}
 private static String conditions(JsonObject node,String inherited){String result=inherited;if(node.has("conditions"))for(var value:node.getAsJsonArray("conditions")){var condition=value.getAsJsonObject();String name=condition.has("condition")?condition.get("condition").getAsString():"";if(name.endsWith(":random_chance")&&condition.has("chance"))result+="；额外概率门槛"+Math.round(condition.get("chance").getAsDouble()*10000)/100.0+"%";else result+="；按战利品表条件生效";}return result;}
 private static String count(JsonElement value){if(value.isJsonPrimitive())return value.getAsString();if(value.isJsonObject()){var o=value.getAsJsonObject();if(o.has("min")&&o.has("max"))return o.get("min").getAsString()+"～"+o.get("max").getAsString();}return "由战利品表决定";}
 private static void functions(JsonObject entry,List<JsonObject> functions){if(entry.has("functions"))for(var value:entry.getAsJsonArray("functions"))functions.add(value.getAsJsonObject());}
 private static void table(LootTableManager manager,ResourceLocation id,List<Entry> output,Set<ResourceLocation> path,String inherited){
  if(path.size()>=24||!path.add(id))return;
  try{var json=LootTableManager.func_215301_a(manager.func_186521_a(id)).getAsJsonObject();if(!json.has("pools"))return;List<JsonObject> common=new ArrayList<>();functions(json,common);
   for(var value:json.getAsJsonArray("pools")){var pool=value.getAsJsonObject();String note=conditions(pool,inherited);List<JsonObject> f=new ArrayList<>(common);functions(pool,f);if(pool.has("entries"))for(var entry:pool.getAsJsonArray("entries"))entry(manager,entry.getAsJsonObject(),output,path,note,f);}
  }finally{path.remove(id);}
 }
 private static void entry(LootTableManager manager,JsonObject entry,List<Entry> output,Set<ResourceLocation> path,String inherited,List<JsonObject> parentFunctions){
  String kind=entry.has("type")?entry.get("type").getAsString():"",note=conditions(entry,inherited);if(entry.has("weight"))note+="；池内权重"+entry.get("weight").getAsInt();
  List<JsonObject> functions=new ArrayList<>(parentFunctions);functions(entry,functions);
  if(entry.has("children"))for(var child:entry.getAsJsonArray("children"))entry(manager,child.getAsJsonObject(),output,path,note,functions);
  if(kind.endsWith("loot_table")&&entry.has("name")){table(manager,new ResourceLocation(entry.get("name").getAsString()),output,path,note);return;}
  if((kind.equals("tag")||kind.endsWith(":tag"))&&entry.has("name")){var tag=net.minecraft.tags.ItemTags.func_199903_a().func_199910_a(new ResourceLocation(entry.get("name").getAsString()));if(tag!=null)for(var value:tag.func_230236_b_())add(output,new ItemStack(value),note+"；标签候选物品");return;}
  if(!(kind.equals("item")||kind.endsWith(":item"))||!entry.has("name"))return;
  ItemStack stack=item(entry.get("name").getAsString());if(stack.func_190926_b())return;
  for(var f:functions){String name=f.has("function")?f.get("function").getAsString():"";try{
   if(name.endsWith("set_nbt")&&f.has("tag"))stack.func_196082_o().func_197643_a(JsonToNBT.func_180713_a(f.get("tag").getAsString()));
   if(name.endsWith("set_count")&&f.has("count"))note+="；数量"+count(f.get("count"));
   if(name.endsWith("enchant_randomly")||name.endsWith("enchant_with_levels")){if(stack.func_77973_b()==item("minecraft:book").func_77973_b())stack=item("minecraft:enchanted_book");note+="；随机附魔";}
   if(name.endsWith("exploration_map")){stack=item("minecraft:filled_map");note+="；探索地图";}
   if(name.endsWith("set_potion")&&f.has("id"))stack.func_196082_o().func_74778_a("Potion",f.get("id").getAsString());
  }catch(com.mojang.brigadier.exceptions.CommandSyntaxException bad){note+="；含自定义数据";}}
  add(output,stack,note);
 }
 @SuppressWarnings("unchecked") private static List<net.minecraft.util.Tuple<Integer,ItemStack>> weighted(WeightedItemStacks value){
  try{var field=WeightedItemStacks.class.getDeclaredField("stacks");field.setAccessible(true);return (List<net.minecraft.util.Tuple<Integer,ItemStack>>)field.get(value);}catch(ReflectiveOperationException e){throw new IllegalStateException("Native raid rewards unavailable",e);}
 }
 private static void raidEntry(List<Entry> entries,ItemStack stack,Element type,String note){
  if(stack.func_77973_b() instanceof com.pixelmonmod.pixelmon.items.TechnicalMoveItem){
   var template=com.pixelmonmod.pixelmon.items.TechnicalMoveItem.getInstanceOf(stack);if(template==null)return;
   for(Element candidate:Element.values())if(candidate!=null&&(type==null||candidate==type))for(var move:com.pixelmonmod.pixelmon.enums.technicalmoves.ITechnicalMove.getForType(template,candidate)){
    var copy=stack.func_77946_l();copy.func_196082_o().func_74777_a("tm",(short)move.getId());add(entries,copy,note+"；招式属性须匹配首领属性");
   }
  }else add(entries,stack,note);
 }
 public static List<Source> build(MinecraftServer server){
  List<Source> sources=new ArrayList<>();var manager=server.func_200249_aQ();var masks=Content.read("backport/structure_mask_loot.json");
  for(var id:new TreeSet<>(manager.func_215304_a()))if(id.func_110623_a().startsWith("chests/")){
   List<Entry> entries=new ArrayList<>();table(manager,id,entries,new HashSet<>(),"候选奖励");var mask=masks.getAsJsonObject("tables").get(id.toString());
   if(mask!=null){String item=mask.getAsString();double chance=masks.getAsJsonObject("chances").has(item)?masks.getAsJsonObject("chances").get(item).getAsDouble():masks.get("chance").getAsDouble();add(entries,item(item),"每箱额外独立"+Math.round(chance*100)+"%概率，1个");}
   if(!entries.isEmpty())sources.add(new Source("structures",id.toString(),title(id),"箱内可能获得；数量、条件见物品提示",distinct(entries)));
  }
  for(var star:DropItemRegistry.raidDrops.entrySet())for(var typed:star.getValue().entrySet()){
   List<Entry> entries=new ArrayList<>();for(var pair:weighted(typed.getValue()))raidEntry(entries,pair.func_76340_b(),typed.getKey(),"候选奖励，权重"+pair.func_76341_a()+"；抽取次数按巢穴配置");
   if(!entries.isEmpty())sources.add(new Source("max_raids","max/"+star.getKey()+"/"+typed.getKey(),star.getKey()+"星极巨巢穴 · "+type(typed.getKey()),"击败首领；通用奖池与首领各属性奖池合并抽取",distinct(entries)));
  }
  for(int stars=1;stars<=6;stars++)for(int type=0;type<TeraItems.NAMES.length;type++){
   List<Entry> entries=new ArrayList<>();ItemStack shard=new ItemStack(Backport.item(TeraItems.NAMES[type]+"_tera_shard"),2+stars*2);add(entries,shard,"固定获得"+(2+stars*2)+"枚，对应首领太晶属性");add(entries,new ItemStack(Backport.item("tera_blast_disc_single_use")),"固定获得1个");
   if(stars>=5&&type!=18)add(entries,new ItemStack(Backport.item("stellar_tera_shard"),stars-3),"额外固定获得"+(stars-3)+"枚");
   for(var value:TeraRaids.DATA.getAsJsonArray("rewards")){var row=value.getAsJsonObject();if(stars>=row.get("min").getAsInt()&&stars<=row.get("max").getAsInt())add(entries,item(row.get("item").getAsString()),"随机奖励，权重"+row.get("weight").getAsString());}
   var n=NAMES.getAsJsonObject("types").get(TeraItems.NAMES[type]);sources.add(new Source("tera_raids","tera/"+stars+"/"+type,stars+"星太晶巢穴 · "+(n==null?TeraItems.NAMES[type]:n.getAsString()),"固定奖励＋随机抽取"+(stars<4?"3～5":stars<6?"4～5":"5")+"次",distinct(entries)));
  }
  sources.addAll(BossLootCatalog.sources());
  return List.copyOf(sources);
 }
}
