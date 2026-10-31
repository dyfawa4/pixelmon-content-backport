package local.pixelmon.backport.client;
import local.pixelmon.backport.*;
import net.minecraft.util.text.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;
@Mod.EventBusSubscriber(modid=Content.NS,value=Dist.CLIENT)
public final class ItemHelpTooltip {
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event){
        append(event.getItemStack(),event.getToolTip(),true);
    }
    public static void append(net.minecraft.item.ItemStack stack,List<ITextComponent> lines,boolean shift){
        if(ItemDescriptionReview.replaces(stack))return;
        List<ITextComponent> details=ItemHelp.details(stack);if(details.isEmpty())return;
        for(ITextComponent detail:details)lines.add(detail.func_230532_e_().func_240699_a_(TextFormatting.GRAY));
    }
}
