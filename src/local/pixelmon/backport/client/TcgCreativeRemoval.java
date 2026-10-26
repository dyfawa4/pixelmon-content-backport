package local.pixelmon.backport.client;
import java.util.*;
import net.minecraft.item.ItemGroup;
import com.pixelmonmod.pixelmon.items.group.PixelmonItemGroups;
import local.pixelmon.backport.Content;
import local.pixelmon.backport.mixin.CreativeGroupsAccess;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
/** Compact exactly the five TCG tabs, preserving all other groups and their order. */
@Mod.EventBusSubscriber(modid=Content.NS,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class TcgCreativeRemoval {
    @SubscribeEvent public static void complete(FMLLoadCompleteEvent event){event.enqueueWork(()->{
        var removed=Set.of(PixelmonItemGroups.TAB_TCG,PixelmonItemGroups.TAB_TCG_CARDS,PixelmonItemGroups.TAB_TCG_DECKS,PixelmonItemGroups.TAB_TCG_PACKS,PixelmonItemGroups.TAB_TCG_COSMETICS);
        var keep=Arrays.stream(ItemGroup.field_78032_a).filter(g->g!=null&&!removed.contains(g)).toArray(ItemGroup[]::new);
        for(int i=0;i<keep.length;i++)((CreativeGroupsAccess)keep[i]).backport$index(i);
        CreativeGroupsAccess.backport$groups(keep);
    });}
}
