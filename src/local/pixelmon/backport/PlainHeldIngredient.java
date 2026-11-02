package local.pixelmon.backport;
import com.google.gson.*;
import java.util.stream.Stream;
import net.minecraft.item.*;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.*;
import net.minecraftforge.registries.ForgeRegistries;
/** Prevent an endless item from being fed back into its nine-to-one recipe. */
public final class PlainHeldIngredient extends Ingredient {
 public static final ResourceLocation ID=new ResourceLocation(Content.NS,"plain_held_item");
 public final ResourceLocation item;
 public PlainHeldIngredient(ResourceLocation id){super(Stream.of(new Ingredient.SingleItemList(new ItemStack(ForgeRegistries.ITEMS.getValue(id)))));item=id;}
 @Override public boolean test(ItemStack s){return s!=null&&!s.func_190926_b()&&item.equals(s.func_77973_b().getRegistryName())&&!EndlessHeldItems.is(s);}
 @Override public boolean isSimple(){return false;}
 @Override public IIngredientSerializer<? extends Ingredient> getSerializer(){return SERIALIZER;}
 @Override public JsonElement func_200304_c(){var j=new JsonObject();j.addProperty("type",ID.toString());j.addProperty("item",item.toString());return j;}
 public static final IIngredientSerializer<PlainHeldIngredient> SERIALIZER=new IIngredientSerializer<PlainHeldIngredient>(){
  public PlainHeldIngredient parse(JsonObject j){return new PlainHeldIngredient(new ResourceLocation(j.get("item").getAsString()));}
  public PlainHeldIngredient parse(PacketBuffer b){return new PlainHeldIngredient(b.func_192575_l());}
  public void write(PacketBuffer b,PlainHeldIngredient i){b.func_192572_a(i.item);}
 };
}
