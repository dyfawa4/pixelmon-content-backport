package local.pixelmon.backport;

import java.util.List;
import com.google.gson.JsonObject;
import net.minecraft.item.*;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.conditions.ILootCondition;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.loot.*;
import net.minecraftforge.registries.*;

/** Append one independent exploration roll, including data-pack-overridden chest tables. */
public final class StructureMaskLoot extends LootModifier {
 public static final DeferredRegister<GlobalLootModifierSerializer<?>> SERIALIZERS=DeferredRegister.create(ForgeRegistries.LOOT_MODIFIER_SERIALIZERS,Content.NS);
 static {SERIALIZERS.register("exploration_masks",Serializer::new);}
 private final float chance;
 private final JsonObject chances;
 private final JsonObject tables;
 private StructureMaskLoot(ILootCondition[] conditions,float chance,JsonObject chances,JsonObject tables){super(conditions);this.chance=chance;this.chances=new com.google.gson.JsonParser().parse(chances.toString()).getAsJsonObject();this.tables=new com.google.gson.JsonParser().parse(tables.toString()).getAsJsonObject();}
 @Override protected List<ItemStack> doApply(List<ItemStack> generated,LootContext context){
  var value=tables.get(context.getQueriedLootTableId().toString());
  if(value!=null){
   String id=value.getAsString();float rate=chances.has(id)?chances.get(id).getAsFloat():chance;
   if(context.func_216032_b().nextFloat()>=rate)return generated;
   var item=ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
   if(item==null||item==Items.field_190931_a)throw new IllegalStateException("Missing exploration mask "+id);
   generated.add(new ItemStack(item));
  }
  return generated;
 }
 public static final class Serializer extends GlobalLootModifierSerializer<StructureMaskLoot> {
  private static float valid(float chance){if(!Float.isFinite(chance)||chance<0||chance>1)throw new IllegalArgumentException("Invalid exploration loot chance "+chance);return chance;}
  @Override public StructureMaskLoot read(ResourceLocation id,JsonObject data,ILootCondition[] conditions){float chance=valid(data.get("chance").getAsFloat());var chances=data.has("chances")?data.getAsJsonObject("chances"):new JsonObject();for(var entry:chances.entrySet())valid(entry.getValue().getAsFloat());return new StructureMaskLoot(conditions,chance,chances,data.getAsJsonObject("tables"));}
  @Override public JsonObject write(StructureMaskLoot modifier){var json=makeConditions(modifier.conditions);json.addProperty("chance",modifier.chance);json.add("chances",new com.google.gson.JsonParser().parse(modifier.chances.toString()));json.add("tables",new com.google.gson.JsonParser().parse(modifier.tables.toString()));return json;}
 }
}
