package local.pixelmon.backport;
import com.google.gson.*;
import java.util.stream.Stream;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.*;
/** Accept native shared-item/NBT Beast Balls and the legacy dedicated ID, without accepting other ball variants. */
public final class BeastBallIngredient extends Ingredient {
    public static final ResourceLocation ID=new ResourceLocation(Content.NS,"beast_ball");
    public static final IIngredientSerializer<BeastBallIngredient> SERIALIZER=new IIngredientSerializer<BeastBallIngredient>(){
        public BeastBallIngredient parse(JsonObject json){return new BeastBallIngredient();}
        public BeastBallIngredient parse(PacketBuffer buffer){return new BeastBallIngredient();}
        public void write(PacketBuffer buffer,BeastBallIngredient ingredient){}
    };
    public BeastBallIngredient(){super(Stream.of(new Ingredient.SingleItemList(modern())));}
    public static ItemStack modern(){
        // Client recipe reload happens before the native PokeBall registry has finished initializing.
        var stack=new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new ResourceLocation("pixelmon:poke_ball")));
        stack.func_196082_o().func_74778_a("PokeBallID","beast_ball");return stack;
    }
    @Override public boolean test(ItemStack stack){
        if(stack==null||stack.func_190926_b())return false;var id=stack.func_77973_b().getRegistryName();
        return new ResourceLocation("pixelmon:beast_ball").equals(id)||new ResourceLocation("pixelmon:poke_ball").equals(id)&&stack.func_77942_o()&&"beast_ball".equals(stack.func_77978_p().func_74779_i("PokeBallID"));
    }
    @Override public boolean isSimple(){return false;}
    @Override public IIngredientSerializer<? extends Ingredient> getSerializer(){return SERIALIZER;}
    @Override public JsonElement func_200304_c(){var json=new JsonObject();json.addProperty("type",ID.toString());return json;}
}
