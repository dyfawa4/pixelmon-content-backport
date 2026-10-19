package local.pixelmon.backport;

import com.google.gson.*;
import java.util.stream.Stream;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.*;
import net.minecraftforge.registries.ForgeRegistries;

/** Only the ordinary Poke Ball variant of the shared native item is accepted. */
public final class PokeBallIngredient extends Ingredient {
    public static final ResourceLocation ID=new ResourceLocation(Content.NS,"poke_ball");
    public static final IIngredientSerializer<PokeBallIngredient> SERIALIZER=new IIngredientSerializer<PokeBallIngredient>(){
        public PokeBallIngredient parse(JsonObject json){return new PokeBallIngredient();}
        public PokeBallIngredient parse(PacketBuffer buffer){return new PokeBallIngredient();}
        public void write(PacketBuffer buffer,PokeBallIngredient ingredient){}
    };
    public PokeBallIngredient(){super(Stream.of(new Ingredient.SingleItemList(modern())));}
    public static ItemStack modern(){
        ItemStack stack=new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("pixelmon","poke_ball")));
        stack.func_196082_o().func_74778_a("PokeBallID","poke_ball");return stack;
    }
    @Override public boolean test(ItemStack stack){
        return stack!=null&&!stack.func_190926_b()
            &&new ResourceLocation("pixelmon","poke_ball").equals(stack.func_77973_b().getRegistryName())
            &&stack.func_77942_o()&&"poke_ball".equals(stack.func_77978_p().func_74779_i("PokeBallID"));
    }
    @Override public boolean isSimple(){return false;}
    @Override public IIngredientSerializer<? extends Ingredient> getSerializer(){return SERIALIZER;}
    @Override public JsonElement func_200304_c(){JsonObject json=new JsonObject();json.addProperty("type",ID.toString());return json;}
}
