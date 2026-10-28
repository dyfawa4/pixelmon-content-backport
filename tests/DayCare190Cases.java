package qa;
import java.util.*;
import java.nio.file.*;
import com.google.gson.*;
import local.pixelmon.backport.*;
import com.pixelmonmod.pixelmon.api.daycare.*;
import com.pixelmonmod.pixelmon.api.daycare.impl.requirement.ItemStackRequirement;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
public final class DayCare190Cases {
 static int assertions,mismatches;
 static void check(boolean yes,String message){assertions++;if(!yes)throw new AssertionError(message);}
 static String expected(ItemStackRequirement r){var parts=new ArrayList<String>();for(var stack:r.getRequiredItems())parts.add(stack.func_190916_E()+"x "+stack.func_200301_q().getString());return String.join(", ",parts);}
 static ItemStack stack(String id,int count){return new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new ResourceLocation(id)),count);}
 public static void run(ServerPlayerEntity player)throws Exception{
  boolean baseline=Files.exists(Path.of("daycare-baseline"));var rows=new JsonArray();int conditions=0,requirements=0;ItemStackRequirement grass=null,clay=null;
  for(var condition:DayCareConditionRegistry.getAllConditions()){
   conditions++;for(var requirement:condition.getRequirements())if(requirement instanceof ItemStackRequirement){var r=(ItemStackRequirement)requirement;requirements++;String want=expected(r),got=r.getError().getString();if(!got.equals(want)){mismatches++;if(!baseline)throw new AssertionError("Runtime material description mismatch "+got+" vs "+want);}var row=new JsonObject();row.addProperty("shown",got);row.addProperty("actual",want);rows.add(row);
    if(r.getRequiredItems().size()==1){var id=r.getRequiredItems().get(0).func_77973_b().getRegistryName().toString();if(id.equals("minecraft:grass_block"))grass=r;if(id.equals("minecraft:clay"))clay=r;}
   }
  }
  check(conditions==18&&requirements==54,"Full native18-type54-item requirement audit");check(grass!=null&&clay!=null,"Both original mismatches present as native requirements");
  var shroom=BossBalance190Probe.mon(285,50,"Tackle");var shroomCondition=DayCareConditionRegistry.getAllConditions().stream().filter(c->c.isConditionFor(shroom,shroom,shroom)).findFirst().orElseThrow();check(shroomCondition.getRequirements().contains(grass),"Actual Shroomish uses native grass condition");
  for(var r:List.of(grass,clay)){
   String id=r.getRequiredItems().get(0).func_77973_b().getRegistryName().toString();check(r.getRequiredItems().get(0).func_190916_E()==3,"Actual native amount3 "+id);
   for(int i=0;i<36;i++)player.field_71071_by.func_70299_a(i,ItemStack.field_190927_a);
   player.field_71071_by.func_70299_a(0,stack("pixelmon:grass_gem",64));check(!r.hasMetRequirement(player,null),"Wrong referenced gem cannot satisfy actual material "+id);
   player.field_71071_by.func_70299_a(1,stack(id,2));check(!r.hasMetRequirement(player,null),"Two materials insufficient "+id);
   player.field_71071_by.func_70299_a(1,stack(id,5));check(r.hasMetRequirement(player,null),"Five includes actual three "+id);r.takeFrom(player,null);check(player.field_71071_by.func_70301_a(1).func_190916_E()==2&&player.field_71071_by.func_70301_a(0).func_190916_E()==64,"Native takes exactly3 actual materials; gem retained "+id);
   var nbt=new CompoundNBT();r.write(nbt);var saved=new ItemStackRequirement();saved.read(nbt);saved.setError(new StringTextComponent(id.endsWith("grass_block")?"3x 草之宝石":"5x 黏土"));
   if(!baseline)check(saved.getError().getString().equals(expected(saved)),"Old saved/network-read wrong text repaired from actual stacks "+id);
  }
  if(!baseline){var nativeData=new LinkedHashMap<ResourceLocation,JsonElement>();for(String type:new String[]{"bug","dark","dragon","electric","fairy","fighting","fire","flying","ghost","grass","ground","ice","normal","poison","psychic","rock","steel","water"})try(var stream=Content.class.getClassLoader().getResourceAsStream("data/pixelmon/daycare/"+type+".json")){nativeData.put(new ResourceLocation("pixelmon",type),new JsonParser().parse(new java.io.InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8)));}
   String original=new Gson().toJson(nativeData);Content.apply("pixelmon","daycare",nativeData);var before=new JsonParser().parse(original).getAsJsonObject();for(var entry:nativeData.entrySet()){var a=before.getAsJsonObject(entry.getKey().toString());var b=entry.getValue().getAsJsonObject();for(var req:a.getAsJsonArray("requirements"))req.getAsJsonObject().remove("error_message");var copy=new JsonParser().parse(b.toString()).getAsJsonObject();for(var req:copy.getAsJsonArray("requirements"))req.getAsJsonObject().remove("error_message");check(a.equals(copy),"All native mechanics/counts/durations unchanged "+entry.getKey());}
   var root=nativeData.get(new ResourceLocation("pixelmon:grass")).getAsJsonObject();var reference=Content.DATA.getAsJsonObject("referenceExtras").getAsJsonObject("daycare/grass");var reversed=new JsonParser().parse(reference.toString()).getAsJsonObject();var array=new JsonArray();for(int i=2;i>=0;i--)array.add(reversed.getAsJsonArray("requirements").get(i));reversed.add("requirements",array);DayCareRequirementText.localize(root,reversed);check(root.getAsJsonArray("requirements").get(0).getAsJsonObject().get("error_message").getAsString().equals("5x 草")&&root.getAsJsonArray("requirements").get(2).getAsJsonObject().get("error_message").getAsString().equals("3x 草方块"),"Reordered reference cannot relabel other rows");
  }
  var result=new JsonObject();result.addProperty("pass",true);result.addProperty("assertions",assertions);result.addProperty("mismatches",mismatches);result.addProperty("baseline",baseline);result.addProperty("conditions",conditions);result.addProperty("requirements",requirements);result.add("nativeRows",rows);Files.writeString(Path.of("daycare190-results.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result));BossBalance190Probe.log("DAYCARE190_PASS conditions="+conditions+" requirements="+requirements+" mismatches="+mismatches+" assertions="+assertions);
 }
}
