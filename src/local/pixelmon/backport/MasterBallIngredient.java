package local.pixelmon.backport;

import com.google.gson.*;
import java.util.stream.Stream;
import net.minecraft.item.*;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.*;
import net.minecraftforge.registries.ForgeRegistries;

/** Pixelmon stores modern balls in one item with a PokeBallID tag. */
public final class MasterBallIngredient extends Ingredient {
    public static final ResourceLocation ID=new ResourceLocation(Content.NS,"master_ball");
    public static final IIngredientSerializer<MasterBallIngredient> SERIALIZER=new IIngredientSerializer<MasterBallIngredient>(){
        public MasterBallIngredient parse(JsonObject json){return new MasterBallIngredient();}
        public MasterBallIngredient parse(PacketBuffer buffer){return new MasterBallIngredient();}
        public void write(PacketBuffer buffer,MasterBallIngredient ingredient){}
    };
    public MasterBallIngredient(){super(Stream.of(new Ingredient.SingleItemList(modern())));}
    public static ItemStack modern(){
        ItemStack stack=new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("pixelmon","poke_ball")));
        stack.func_196082_o().func_74778_a("PokeBallID","master_ball");return stack;
    }
    @Override public boolean test(ItemStack stack){
        if(stack==null || stack.func_190926_b())return false;
        ResourceLocation item=stack.func_77973_b().getRegistryName();
        return new ResourceLocation("pixelmon","master_ball").equals(item)
            || (new ResourceLocation("pixelmon","poke_ball").equals(item) && stack.func_77942_o()
                && "master_ball".equals(stack.func_77978_p().func_74779_i("PokeBallID")));
    }
    @Override public boolean isSimple(){return false;}
    @Override public IIngredientSerializer<? extends Ingredient> getSerializer(){return SERIALIZER;}
    @Override public JsonElement func_200304_c(){JsonObject json=new JsonObject();json.addProperty("type",ID.toString());return json;}
}
