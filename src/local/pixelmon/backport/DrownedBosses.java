package local.pixelmon.backport;
import java.util.*;
import com.google.gson.*;
import com.pixelmonmod.pixelmon.api.pokemon.boss.DropSet;
import com.pixelmonmod.pixelmon.entities.pixelmon.AbstractHoldsItemsEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.*;
import net.minecraft.util.ResourceLocation;
/** Native species drops remain in dropBossItems; this is only the requested extra prize pool. */
public final class DrownedBosses {
 private DrownedBosses(){}
 public static boolean is(AbstractHoldsItemsEntity entity){return entity!=null&&"drowned".equals(entity.getBossTier().getID());}
 public static void configure(Map<ResourceLocation,JsonElement> data){
  JsonObject legendary=null,ultimate=null;
  for(var row:data.entrySet())if(row.getValue().isJsonObject()){
   var tier=row.getValue().getAsJsonObject();String id=tier.has("id")?tier.get("id").getAsString():row.getKey().func_110623_a();
   if(id.equals("legendary"))legendary=tier;
   if(id.equals("ultimate"))ultimate=tier;
   if(id.equals("drowned")){tier.addProperty("canRandomlySpawn",true);tier.addProperty("weight",1);tier.addProperty("levelMultiplier",1);tier.addProperty("extraLevels",60);}
  }
  if(ultimate!=null){ultimate.addProperty("levelMultiplier",1);ultimate.addProperty("extraLevels",50);
   // Same item pools and odds as Legendary, with twice the rolls in each group.
   // Native getDrops still selects the species' corresponding Mega stone.
   if(legendary!=null&&legendary.has("dropSets")){var sets=new JsonParser().parse(legendary.get("dropSets").toString()).getAsJsonArray();for(var value:sets){var set=value.getAsJsonObject();set.addProperty("quantity",2*set.get("quantity").getAsInt());}ultimate.add("dropSets",sets);}
  }
 }
 public static boolean exclusive(Item item){return item!=null&&new ResourceLocation(Content.NS,"ash_greninjite").equals(item.getRegistryName());}
 public static List<Item> stones(){var seen=Collections.newSetFromMap(new IdentityHashMap<Item,Boolean>());List<Item> result=new ArrayList<>();for(var stone:com.pixelmonmod.pixelmon.api.registries.PixelmonItems.megaStones)if(stone!=Items.field_190931_a&&!exclusive(stone)&&seen.add(stone))result.add(stone);return result;}
 public static Set<DropSet.Drop> rewards(ServerPlayerEntity player,AbstractHoldsItemsEntity entity){
  var random=player==null?entity.func_70681_au():player.func_70681_au();Set<DropSet.Drop> result=new LinkedHashSet<>();var stones=stones();
  if(!stones.isEmpty())result.add(new DropSet.Drop(1,"",new ItemStack(stones.get(random.nextInt(stones.size()))),false));
  var pool=AuthorBossRewards.available();for(int index:AuthorBossPolicy.draw(pool.size(),3,random))result.add(new DropSet.Drop(1,"",AuthorBossRewards.stack(pool.get(index)),false));
  return result;
 }
 public static void catalogue(List<LootSourceCatalog.Entry> entries){
  for(var stone:stones())entries.add(new LootSourceCatalog.Entry(new ItemStack(stone),"额外随机获得1颗Mega石；全部已注册石头等权重"));
  for(var entry:AuthorBossRewards.available())entries.add(new LootSourceCatalog.Entry(AuthorBossRewards.stack(entry),"溺水额外从作者物品池抽3份、不重复；不附送作者的固定大师球/金钱"));
 }
}
