package local.pixelmon.backport;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
/** The obsolete registry entry is removed; existing world IDs remap to the retained stone. */
@Mod.EventBusSubscriber(modid=Content.NS,bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class DragonStoneMigration {
    @SubscribeEvent public static void missing(RegistryEvent.MissingMappings<Item> event){
        for(RegistryEvent.MissingMappings.Mapping<Item> mapping:event.getAllMappings())if(mapping.key.toString().equals("pixelmon_backport:dragonitite"))mapping.remap(Backport.item("dragoninite"));
    }
}
