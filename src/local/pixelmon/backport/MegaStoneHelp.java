package local.pixelmon.backport;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.*;
import java.util.*;

/** One description for native and backported stones, derived from actual routes. */
public final class MegaStoneHelp {
    private MegaStoneHelp(){}
    public static List<ITextComponent> details(ItemStack stack){
        if(DrownedBosses.exclusive(stack.func_77973_b()))return Arrays.asList(new TranslationTextComponent("pixelmon.backport.ash_mega.use"),new TranslationTextComponent("pixelmon.backport.ash_mega.ability"),new TranslationTextComponent("pixelmon.backport.ash_mega.source"));
        if(stack.func_77973_b()==Backport.item("greninjite"))return Arrays.asList(new TranslationTextComponent("pixelmon.backport.normal_greninja_mega.use"),new TranslationTextComponent("pixelmon.backport.mega.help.activate"),new TranslationTextComponent("pixelmon.backport.mega.help.source"));
        Set<String> compatible=new LinkedHashSet<>();
        for(var species:PixelmonSpecies.getAll())for(var form:species.getForms()){
            if(form.getName().startsWith("mega"))continue;
            var target=form.getMegaForm(stack.func_77973_b());
            if(target.isPresent())compatible.add(species.getLocalizedName());
        }
        ITextComponent use=compatible.isEmpty()?new TranslationTextComponent("pixelmon.backport.mega.help.fallback"):
            new TranslationTextComponent("pixelmon.backport.mega.help.use",String.join("、",compatible));
        return Arrays.asList(use,new TranslationTextComponent("pixelmon.backport.mega.help.activate"),new TranslationTextComponent("pixelmon.backport.mega.help.source"));
    }
}
