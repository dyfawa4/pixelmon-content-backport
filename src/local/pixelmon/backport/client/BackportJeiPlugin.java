package local.pixelmon.backport.client;
import local.pixelmon.backport.*;
import mezz.jei.api.*;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
/** Loaded by JEI only, never referenced by the common mod entry point. */
@JeiPlugin
public final class BackportJeiPlugin implements IModPlugin {
    @Override public ResourceLocation getPluginUid(){return new ResourceLocation(Content.NS,"item_sources");}
    @Override public void registerRecipes(IRecipeRegistration registry){
        Backport.REGISTERED.forEach((name,item)->{
            ItemStack stack=new ItemStack(item.get());
            java.util.List<ITextComponent> details=ItemHelp.details(stack);
            if(!details.isEmpty())registry.addIngredientInfo(stack,VanillaTypes.ITEM,details.toArray(new ITextComponent[0]));
        });
        for(int number:new int[]{171,207,210,225,226,227,228,229}){
            ItemStack stack=com.pixelmonmod.pixelmon.items.TechnicalMoveItem.of(com.pixelmonmod.pixelmon.enums.TMType.TM9,BackportTechnicalMoves.resolve(com.pixelmonmod.pixelmon.enums.TMType.TM9,number));
            java.util.List<ITextComponent> details=ItemHelp.details(stack);
            registry.addIngredientInfo(stack,VanillaTypes.ITEM,details.toArray(new ITextComponent[0]));
        }
    }
}
