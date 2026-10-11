package local.pixelmon.backport.client;
import local.pixelmon.backport.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;
@Mod.EventBusSubscriber(modid=Content.NS,value=Dist.CLIENT)
public final class ItemHelpTooltip {
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event){
        append(event.getItemStack(),event.getToolTip(),Screen.func_231173_s_());
    }
    public static void append(net.minecraft.item.ItemStack stack,List<ITextComponent> lines,boolean shift){
        List<ITextComponent> details=ItemHelp.details(stack);if(details.isEmpty())return;
        if(shift)for(ITextComponent detail:details)lines.add(detail.func_230532_e_().func_240699_a_(TextFormatting.GRAY));
        else lines.add(new TranslationTextComponent("pixelmon.backport.item.shift_help").func_240699_a_(TextFormatting.GRAY));
    }
}
