package local.pixelmon.backport;
import com.google.gson.*;
import java.util.*;
import java.util.stream.Stream;
import com.pixelmonmod.pixelmon.items.*;
import com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.PokeBallRegistry;
import net.minecraft.item.*;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.IIngredientSerializer;
import net.minecraftforge.registries.ForgeRegistries;
/** All real native ball-lid variants, including their shared-item NBT and legacy lids. */
public final class BallLidIngredient extends Ingredient {
    public static final ResourceLocation ID=new ResourceLocation(Content.NS,"ball_lid");
    public static final IIngredientSerializer<BallLidIngredient> SERIALIZER=new IIngredientSerializer<BallLidIngredient>(){
        public BallLidIngredient parse(JsonObject json){return new BallLidIngredient();}public BallLidIngredient parse(PacketBuffer buffer){return new BallLidIngredient();}public void write(PacketBuffer buffer,BallLidIngredient value){}
    };
    private static Stream<Ingredient.IItemList> samples(){List<Ingredient.IItemList> result=new ArrayList<>();for(var ball:PokeBallRegistry.getAll())result.add(new Ingredient.SingleItemList(PokeBallLidItem.of(ball)));for(var item:ForgeRegistries.ITEMS)if(item instanceof OldPokeBallLidItem)result.add(new Ingredient.SingleItemList(new ItemStack(item)));return result.stream();}
    public BallLidIngredient(){super(samples());}
    @Override public boolean test(ItemStack stack){
        if(stack==null||stack.func_190926_b())return false;
        if(stack.func_77973_b() instanceof OldPokeBallLidItem)return true;
        if(!(stack.func_77973_b() instanceof PokeBallLidItem)||!stack.func_77942_o()||!stack.func_77978_p().func_150297_b("PokeBallID",8))return false;
        String id=stack.func_77978_p().func_74779_i("PokeBallID");return PokeBallRegistry.getAll().stream().anyMatch(ball->ball.getName().equals(id));
    }
    @Override public boolean isSimple(){return false;}
    @Override public IIngredientSerializer<? extends Ingredient> getSerializer(){return SERIALIZER;}
    @Override public JsonElement func_200304_c(){JsonObject json=new JsonObject();json.addProperty("type",ID.toString());return json;}
}
