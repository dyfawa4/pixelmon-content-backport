package local.pixelmon.backport.client;

import mezz.jei.config.ServerInfo;
import mezz.jei.network.PacketHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.NetworkHooks;

/** Integrated MIT Pixelmon JEI connection patch; use negotiated channels after login. */
@Mod.EventBusSubscriber(modid="pixelmon_backport",value=Dist.CLIENT)
public final class JeiConnectionRepair {
    private static boolean enabled(){return ModList.get().isLoaded("jei") && !ModList.get().isLoaded("pixelmon_jei_fix");}
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void login(ClientPlayerNetworkEvent.LoggedInEvent event){
        if(!enabled() || event.getNetworkManager()==null)return;
        var connection=NetworkHooks.getConnectionData(event.getNetworkManager());
        if(connection==null)return;
        boolean present=connection.getChannels().containsKey(PacketHandler.CHANNEL_ID);
        ServerInfo.onConnectedToServer(present);
        org.apache.logging.log4j.LogManager.getLogger("pixelmon_backport").info("JEI server detection restored from negotiated channels: {}",present);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void logout(ClientPlayerNetworkEvent.LoggedOutEvent event){if(enabled())ServerInfo.onConnectedToServer(false);}
}
