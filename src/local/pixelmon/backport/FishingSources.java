package local.pixelmon.backport;
import java.util.*;
import com.google.gson.*;
import com.pixelmonmod.pixelmon.api.spawning.*;
import com.pixelmonmod.pixelmon.api.spawning.archetypes.entities.collection.SpawnInfoCollection;
import com.pixelmonmod.pixelmon.api.spawning.archetypes.entities.items.SpawnInfoItem;
import com.pixelmonmod.pixelmon.api.spawning.archetypes.entities.pokemon.SpawnInfoPokemon;
import com.pixelmonmod.pixelmon.spawning.PixelmonSpawning;
import net.minecraft.item.*;import net.minecraft.util.ResourceLocation;
/** Read the loaded fishing pool without selecting a catch or creating a Pokemon. */
public final class FishingSources {
 private static final Gson JSON=new Gson();
 private static final JsonObject NAMES=Content.read("assets/pixelmon/lang/zh_cn.json");
 private static final JsonObject BIOMES=Content.read("backport/compass_names.json").getAsJsonObject("biome");
 private FishingSources(){}
 static String name(String key){return NAMES.has(key)?NAMES.get(key).getAsString():key;}
 static String biome(String value){if(BIOMES!=null&&BIOMES.has(value))return BIOMES.get(value).getAsString();String k="biome."+value.replace(':','.');if(NAMES.has(k))return name(k);switch(value){case "arid":return "干旱类群系";case "deserts":return "沙漠类群系";case "oceanic":return "海洋类群系";case "beaches":return "沙滩类群系";case "lakes":return "湖泊类群系";case "roofed":return "黑森林类群系";case "mesas":return "恶地类群系";case "hellish":return "下界类群系";case "freezing":return "冰冻类群系";case "taigas":return "针叶林类群系";case "mountainous":return "山地类群系";default:return value;}}
 static String label(String key){switch(key){case "stringBiomes":return "群系（所列满足其一）";case "times":return "时段";case "weathers":return "天气";case "dimensions":return "维度";case "seesSky":return "可见天空";case "minY":return "最低高度";case "maxY":return "最高高度";case "minLightLevel":return "最低亮度";case "maxLightLevel":return "最高亮度";case "baseBlocks":return "水底／基底方块";case "neededNearbyBlocks":return "附近需要方块";case "structures":return "建筑限制";case "moonPhase":return "月相";case "minX":return "最小X";case "maxX":return "最大X";case "minZ":return "最小Z";case "maxZ":return "最大Z";case "temperature":return "温度";case "partyHeadSpecies":return "队首宝可梦";case "realWorldSpawnTimes":return "现实时间";default:return key;}}
 static String value(String key,JsonElement value){if(value.isJsonArray()){var list=new ArrayList<String>();for(var v:value.getAsJsonArray())list.add(value(key,v));return String.join("、",list);}if(value.isJsonObject())return value.toString();String text=value.getAsString();if(key.equals("stringBiomes"))return biome(text);if(text.equals("true"))return "是";if(text.equals("false"))return "否";switch(text){case "DAY":return "白天";case "NIGHT":return "夜晚";case "DAWN":return "黎明";case "DUSK":return "黄昏";case "MORNING":return "早晨";case "AFTERNOON":return "下午";case "RAIN":return "雨天";case "CLEAR":return "晴天";case "STORM":return "雷雨";default:return text;}}
 static String condition(Object condition){if(condition==null)return "";var out=new ArrayList<String>();var data=JSON.toJsonTree(condition).getAsJsonObject();for(var pair:data.entrySet()){if(pair.getValue().isJsonArray()&&pair.getValue().getAsJsonArray().size()==0||pair.getValue().isJsonPrimitive()&&pair.getValue().getAsString().isEmpty()||pair.getValue().isJsonNull()||pair.getKey().equals("disabled")&&pair.getValue().getAsString().equals("false"))continue;out.add(label(pair.getKey())+"："+value(pair.getKey(),pair.getValue()));}return String.join("；",out);}
 static String rods(SpawnInfo info){var out=new ArrayList<String>();if(info.stringLocationTypes!=null)for(String value:info.stringLocationTypes)out.add(value.replace("Super Rod","厉害钓竿").replace("Good Rod","好钓竿").replace("Old Rod","宝可梦钓竿").replace("Oas Rod","绿洲钓竿").replace(" Lava","／岩浆").replace(" Rod", "钓竿")+(value.contains("Lava")?"":"／水域"));return out.isEmpty()?"":"钓竿／介质："+String.join("、",out);}
 static String selection(SpawnInfo info){return info.percentage!=null?"配置百分比："+info.percentage+"%（只对应当前选择层）":info.chance!=null?"配置概率参数："+info.chance:"候选权重："+info.rarity+"（非固定百分比）";}
 static String constraints(SpawnInfo info){var out=new ArrayList<String>();String r=rods(info);if(!r.isEmpty())out.add(r);String c=condition(info.condition);if(!c.isEmpty())out.add("需要："+c);c=condition(info.anticondition);if(!c.isEmpty())out.add("排除条件："+c);if(info.compositeCondition!=null){if(info.compositeCondition.conditions!=null)for(var v:info.compositeCondition.conditions)out.add("复合可选组（各组满足其一）："+condition(v));if(info.compositeCondition.anticonditions!=null)for(var v:info.compositeCondition.anticonditions)out.add("复合排除组（任一组满足即排除）："+condition(v));}if(info.rarityMultipliers!=null&&!info.rarityMultipliers.isEmpty())out.add("另含条件权重倍率："+JSON.toJson(info.rarityMultipliers));return String.join("；",out);}
 static void add(List<LootSourceCatalog.Source> out,SpawnInfo info,String id,String inherited){
  String own=constraints(info);String conditions=inherited+(inherited.isEmpty()||own.isEmpty()?"":"；")+own;
  if(info instanceof SpawnInfoCollection){int n=0;for(var child:((SpawnInfoCollection)info).collection)add(out,child,id+"/"+n++,conditions+"；外层奖池"+selection(info));return;}
  ItemStack stack;String title;
  if(info instanceof SpawnInfoItem){var item=(SpawnInfoItem)info;stack=item.itemStack==null?item.item.getItemStack():item.itemStack.func_77946_l();if(stack.func_190926_b())return;title=name(stack.func_77973_b().func_77658_a());conditions+="；数量："+item.minQuantity+"～"+item.maxQuantity;}
  else if(info instanceof SpawnInfoPokemon){var pokemon=(SpawnInfoPokemon)info;var species=pokemon.getSpecies();if(species==null||species.getDex()<1)return;var form=pokemon.getForm();stack=new ItemStack(com.pixelmonmod.pixelmon.api.registries.PixelmonItems.pixelmon_sprite);var tag=stack.func_196082_o();tag.func_74777_a("ndex",(short)species.getDex());tag.func_74778_a("form",form.getName());tag.func_74774_a("gender",(byte)0);tag.func_74778_a("palette","none");title=name("pixelmon."+species.getName().toLowerCase(Locale.ROOT));conditions+="；等级："+pokemon.minLevel+"～"+pokemon.maxLevel+"；个体条件："+pokemon.toString();}
  else return;
  conditions+="；"+selection(info);
  out.add(new LootSourceCatalog.Source("fishing",id,"钓鱼 · "+title,"服务器当前已加载钓鱼候选；所有内外层条件同时检查，附魔不会绕过地区限制",List.of(new LootSourceCatalog.Entry(stack,conditions))));
 }
 public static List<LootSourceCatalog.Source> build(){var out=new ArrayList<LootSourceCatalog.Source>();var spawner=PixelmonSpawning.fishingSpawner;var sets=spawner==null?PixelmonSpawning.fishing:spawner.spawnSets;if(sets!=null)for(var set:sets){int n=0;for(var info:set.spawnInfos)add(out,info,"fishing/"+set.id+"/"+n++,"");}return out;}
}
