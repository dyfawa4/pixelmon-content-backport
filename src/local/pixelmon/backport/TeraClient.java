package local.pixelmon.backport;
import java.util.*;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.*;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid=Content.NS,value=net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class TeraClient {
    public static TeraNetwork.Reply battle;
    public static TeraNetwork.Snapshot snapshot;
    public static TeraNetwork.RaidBurst raidBurst;
    private TeraClient(){}
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggedOutEvent event){battle=null;snapshot=null;raidBurst=null;}
    public static void accept(TeraNetwork.Reply r){if(r.battle>=0)battle=r;else {Minecraft mc=Minecraft.func_71410_x();if(mc.field_71462_r instanceof TeraSettingsScreen)((TeraSettingsScreen)mc.field_71462_r).accept(r);else if(r.feedback==0)mc.func_147108_a(new TeraSettingsScreen(r));}}
}
