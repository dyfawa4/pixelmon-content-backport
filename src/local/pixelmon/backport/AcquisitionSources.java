package local.pixelmon.backport;
import java.util.*;
import net.minecraft.item.*;
import com.pixelmonmod.pixelmon.entities.npcs.registry.*;
import com.pixelmonmod.pixelmon.blocks.machines.VendingMachineShop;
import local.pixelmon.backport.mixin.*;
/** Read the loaded server catalogues. Do not roll stock, drops or prices. */
public final class AcquisitionSources {
 private AcquisitionSources(){}
 private static final com.google.gson.JsonObject NAMES=Content.read("assets/pixelmon/lang/zh_cn.json");
 private static String name(String key){var text=NAMES.get(key);return text==null?key:text.getAsString();}
 private static String shopType(String type){return type.equals("PokemartMain")?"宝可梦商店主店员":type.equals("PokemartSecond")?"宝可梦商店副店员":type.equals("Spawn")?"野外店员":type;}
 public static boolean relevant(ItemStack s){
  if(s.func_190926_b()||s.func_77973_b().getRegistryName()==null)return false;
  String id=s.func_77973_b().getRegistryName().toString();
  if(!id.startsWith("pixelmon:"))return false;
  for(var v:TARGETS)if(v.getAsString().equals(id))return true;
  return s.func_77973_b().getClass().getName().matches(".*(Potion|Vitamin|Curry|Juice|Candy|Ether|Elixir|Revive|Medicine|Feather).*Item");
 }
 private static final com.google.gson.JsonArray TARGETS=Content.read("backport/chapter_two_source_items.json").getAsJsonArray("items");
 public static List<LootSourceCatalog.Source> build(){
  var out=new ArrayList<LootSourceCatalog.Source>();
  for(var shop:ServerNPCRegistry.getEnglishShopkeepers()){
   var entries=new ArrayList<LootSourceCatalog.Entry>();
   for(var row:((ShopkeeperSourcesAccess)(Object)shop).backport$items()){
    if(row.getRarity()<=0||((ShopPriceSourcesAccess)(Object)row.getBaseItem()).backport$buy()<=0)continue;
    ItemStack stack=row.getItemStack();if(stack.func_190926_b()||TcgRemoval.item(stack))continue;
    entries.add(new LootSourceCatalog.Entry(stack,"店员候选商品；基础售价："+((ShopPriceSourcesAccess)(Object)row.getBaseItem()).backport$buy()+"宝可币／个；实际库存及售价以交互界面为准；商品配置："+row.getBaseItem().id));
   }
   if(!entries.isEmpty())out.add(new LootSourceCatalog.Source("shops","shop/"+shop.id,shopType(String.valueOf(shop.type))+" · "+shop.id,"店员类型："+shopType(String.valueOf(shop.type))+"；仅此类店员的候选商品，并非所有店员必定出售",entries));
  }
  var vending=new ArrayList<LootSourceCatalog.Entry>();
  for(var row:new VendingMachineShop().getItems())if(!row.getItemStack().func_190926_b()&&!TcgRemoval.item(row.getItemStack())&&row.getBuyCost()>0)vending.add(new LootSourceCatalog.Entry(row.getItemStack(),"售货机购买；基础售价："+row.getBuyCost()+"宝可币／个"));
  if(!vending.isEmpty())out.add(new LootSourceCatalog.Source("shops","vending","售货机购买","原生售货机商品；额外加入的商品或改价以实际机器界面为准",vending));
  for(var key:com.pixelmonmod.pixelmon.enums.EnumCurryKey.values()){
   Item dish=key.getDishItem();if(dish==null||dish==Items.field_190931_a)continue;
   var ingredients=new ArrayList<String>();for(Item item:net.minecraftforge.registries.ForgeRegistries.ITEMS)if(item instanceof com.pixelmonmod.pixelmon.items.CurryKeyItem&&((com.pixelmonmod.pixelmon.items.CurryKeyItem)item).getKey()==key)ingredients.add(name(item.func_77658_a())+"（"+item.getRegistryName()+"）");
   String material=ingredients.isEmpty()?"不加主食材，仅加入树果":"主食材："+String.join("／",ingredients)+"；另加入树果";
   out.add(new LootSourceCatalog.Source("cooking","curry/"+key.name(),"料理锅制作 · "+name(dish.func_77658_a()),"放入材料并完成料理锅烹饪；风味和评级取决于树果及操作",List.of(new LootSourceCatalog.Entry(new ItemStack(dish),material+"；使用料理锅，不是普通工作台合成"))));
  }
  for(var species:DropItemRegistry.pokemonDrops.entrySet())for(var info:species.getValue()){
   var entries=new ArrayList<LootSourceCatalog.Entry>();
   for(var row:info.getDrops())if(relevant(row.getItemStack())&&row.getChance()>0&&row.getMax()>0)entries.add(new LootSourceCatalog.Entry(row.getItemStack(),"击败后候选掉落；数量："+row.getMin()+"～"+row.getMax()+"；概率："+String.format(java.util.Locale.ROOT,"%.2f%%",row.getChance()*100)+"；个体条件："+info.getPokemonSpec()));
   if(!entries.isEmpty())out.add(new LootSourceCatalog.Source("pokemon_drops","pokemon/"+species.getKey().getName()+"/"+info.getPokemonSpec(),"宝可梦掉落 · "+name("pixelmon."+species.getKey().getName().toLowerCase(java.util.Locale.ROOT)),"按服务器已加载掉落配置；需满足该个体条件，事件可能修改实际掉落",entries));
  }
  int tier=0;for(var pool:List.of(DropItemRegistry.tier1,DropItemRegistry.tier2,DropItemRegistry.tier3,DropItemRegistry.ultraSpace)){
   tier++;var entries=new ArrayList<LootSourceCatalog.Entry>();for(var s:pool)if(relevant(s))entries.add(new LootSourceCatalog.Entry(s,"奖励箱候选物品；随机获得，并非必得"));
   if(!entries.isEmpty())out.add(new LootSourceCatalog.Source("structures","pokeloot/"+tier,"宝可梦奖励箱 · "+new String[]{"精灵球档","超级球档","大师球档","究极空间"}[tier-1],"服务器已加载的原生奖励箱池",entries));
  }
  return out;
 }
}
