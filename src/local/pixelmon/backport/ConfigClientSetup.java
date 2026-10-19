package local.pixelmon.backport;
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid=Content.NS,value=net.minecraftforge.api.distmarker.Dist.CLIENT,bus=net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD)
public final class ConfigClientSetup {
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent e){
        net.minecraftforge.fml.ModLoadingContext.get().registerExtensionPoint(net.minecraftforge.fml.ExtensionPoint.CONFIGGUIFACTORY,()->(mc,parent)->new BackportConfigScreen(parent));
    }
}
