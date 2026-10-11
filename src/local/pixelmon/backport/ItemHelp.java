package local.pixelmon.backport;
import com.google.gson.*;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
/** Shared tooltip and JEI information: one catalogue for uses and actual sources. */
public final class ItemHelp {
    public static final JsonObject DATA;
    static{try(InputStream in=ItemHelp.class.getResourceAsStream("/backport/item_help.json")){
        DATA=new JsonParser().parse(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
    }catch(IOException e){throw new ExceptionInInitializerError(e);}}
    private ItemHelp(){}
    public static List<ITextComponent> details(ItemStack stack){
        if(stack.func_77973_b().getRegistryName()==null)return Collections.emptyList();
        String id=stack.func_77973_b().getRegistryName().toString();JsonElement found=DATA.get(id);
        if(id.equals("pixelmon:dragoninite") || id.equals("pixelmon:dragonitite"))found=DATA.get("pixelmon_backport:dragoninite");
        if(found==null && stack.func_77973_b() instanceof com.pixelmonmod.pixelmon.items.TechnicalMoveItem){
            com.pixelmonmod.pixelmon.enums.technicalmoves.ITechnicalMove move=com.pixelmonmod.pixelmon.items.TechnicalMoveItem.getMove(stack);
            if(move!=null && move.getGeneration()==9)found=DATA.get("tm_gen9:"+move.getId());
        }
        if(found==null)return Collections.emptyList();
        List<ITextComponent> lines=new ArrayList<>();
        for(JsonElement value:found.getAsJsonArray())lines.add(new StringTextComponent(value.getAsString()).func_240699_a_(TextFormatting.GRAY));
        if(stack.func_77973_b() instanceof com.pixelmonmod.pixelmon.items.TechnicalMoveItem){
            com.pixelmonmod.pixelmon.enums.technicalmoves.ITechnicalMove move=com.pixelmonmod.pixelmon.items.TechnicalMoveItem.getMove(stack);
            if(move!=null && move.getAttack()!=null)lines.add(new TranslationTextComponent(move.getAttack().getDescriptionKey()).func_240699_a_(TextFormatting.GRAY));
        }
        return lines;
    }
}
