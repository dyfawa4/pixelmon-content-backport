package local.pixelmon.backport.client;
import local.pixelmon.backport.*;
import mezz.jei.api.*;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
/** Loaded by JEI only, never referenced by the common mod entry point. */
@JeiPlugin
public final class BackportJeiPlugin implements IModPlugin {
    public static java.util.List<ITextComponent> information(ItemStack stack){
        java.util.List<ITextComponent> result=new java.util.ArrayList<>();
        for(ITextComponent line:ItemHelp.details(stack))result.add(line.func_230532_e_().func_240699_a_(TextFormatting.BLACK));
        return result;
    }
    @Override public ResourceLocation getPluginUid(){return new ResourceLocation(Content.NS,"item_sources");}
    @Override public void registerRecipes(IRecipeRegistration registry){
        java.util.Set<net.minecraft.item.Item> seen=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        Backport.REGISTERED.forEach((name,item)->{
            ItemStack stack=new ItemStack(Backport.item(name));
            if(!seen.add(stack.func_77973_b()))return;
            java.util.List<ITextComponent> details=information(stack);
            if(!details.isEmpty())registry.addIngredientInfo(stack,VanillaTypes.ITEM,details.toArray(new ITextComponent[0]));
        });
        for(int number:new int[]{171,207,210,225,226,227,228,229}){
            ItemStack stack=com.pixelmonmod.pixelmon.items.TechnicalMoveItem.of(com.pixelmonmod.pixelmon.enums.TMType.TM9,BackportTechnicalMoves.resolve(com.pixelmonmod.pixelmon.enums.TMType.TM9,number));
            java.util.List<ITextComponent> details=information(stack);
            registry.addIngredientInfo(stack,VanillaTypes.ITEM,details.toArray(new ITextComponent[0]));
        }
        for(java.util.Map.Entry<String,com.google.gson.JsonElement> entry:ItemHelp.DATA.entrySet()){
            if(!entry.getKey().startsWith("pixelmon:"))continue;
            net.minecraft.item.Item item=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new ResourceLocation(entry.getKey()));
            if(item==null || item==net.minecraft.item.Items.field_190931_a || !seen.add(item))continue;
            ItemStack stack=new ItemStack(item);java.util.List<ITextComponent> details=information(stack);
            if(!details.isEmpty())registry.addIngredientInfo(stack,VanillaTypes.ITEM,details.toArray(new ITextComponent[0]));
        }
    }
    @Override public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime runtime){
        if(Backport.item("dragoninite")!=Backport.REGISTERED.get("dragoninite").get())runtime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM,java.util.Collections.singletonList(new ItemStack(Backport.REGISTERED.get("dragoninite").get())));
    }
}
