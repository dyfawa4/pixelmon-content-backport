package local.pixelmon.backport;
import java.util.*;
import com.google.gson.*;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
/** Keep registry IDs for old saves, remove TCG acquisition and interactions only. */
@Mod.EventBusSubscriber(modid=Content.NS)
public final class TcgRemoval {
    private TcgRemoval(){}
    public static boolean id(ResourceLocation id){return id!=null&&id.func_110624_b().equals("tcg");}
    public static boolean item(ItemStack stack){return stack!=null&&id(stack.func_77973_b().getRegistryName());}
    public static void recipes(Map<ResourceLocation,JsonElement> recipes){recipes.entrySet().removeIf(e->id(e.getKey())||tcgResult(e.getValue()));}
    private static boolean tcgResult(JsonElement json){
        if(!json.isJsonObject())return false;var result=json.getAsJsonObject().get("result");if(result==null)return false;
        if(result.isJsonPrimitive())return result.getAsString().startsWith("tcg:");
        return result.isJsonObject()&&result.getAsJsonObject().has("item")&&result.getAsJsonObject().get("item").getAsString().startsWith("tcg:");
    }
    public static void loot(Map<ResourceLocation,JsonElement> tables){
        tables.forEach((id,table)->{if(table.isJsonObject()){if(id(id))table.getAsJsonObject().add("pools",new JsonArray());else clean(table);}});
    }
    private static void clean(JsonElement value){
        if(value.isJsonArray()){
            var array=value.getAsJsonArray();for(int i=array.size()-1;i>=0;i--){var element=array.get(i);
                if(element.isJsonObject()&&element.getAsJsonObject().has("name")&&element.getAsJsonObject().get("name").isJsonPrimitive()
                    &&element.getAsJsonObject().get("name").getAsString().startsWith("tcg:"))array.remove(i);else clean(element);}
        }else if(value.isJsonObject())for(var member:value.getAsJsonObject().entrySet())clean(member.getValue());
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void rightItem(PlayerInteractEvent.RightClickItem event){if(item(event.getItemStack()))event.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void rightBlock(PlayerInteractEvent.RightClickBlock event){if(item(event.getItemStack())||id(event.getWorld().func_180495_p(event.getPos()).func_177230_c().getRegistryName()))event.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void join(EntityJoinWorldEvent event){if(id(event.getEntity().func_200600_R().getRegistryName()))event.setCanceled(true);}
}
