package local.pixelmon.backport;
import com.pixelmonmod.pixelmon.client.storage.ClientStorageManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;
@Mod.EventBusSubscriber(modid=Content.NS,value=Dist.CLIENT)
public final class FieldMegaClient {
    public static final KeyBinding KEY=new KeyBinding("key.pixelmon_backport.field_mega",KeyConflictContext.IN_GAME,InputMappings.Type.KEYSYM,GLFW.GLFW_KEY_J,"key.categories.pixelmon_backport");
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){if(event.phase!=TickEvent.Phase.END)return;
        Minecraft mc=Minecraft.func_71410_x();while(KEY.func_151468_f()){
            if(mc.field_71439_g==null || mc.field_71462_r!=null || ClientStorageManager.party==null)continue;
            mc.func_147108_a(new FieldMegaScreen(ClientStorageManager.party.getSelectedSlot()));
        }
    }
    @SubscribeEvent public static void wheel(net.minecraftforge.client.event.GuiOpenEvent event){
        if(event.getGui() instanceof com.pixelmonmod.pixelmon.client.gui.moveskills.MoveSkillSelectScreen
            && !(event.getGui() instanceof FieldMegaScreen.Skills) && ClientStorageManager.party!=null)
            event.setGui(new FieldMegaScreen(ClientStorageManager.party.getSelectedSlot()));
    }
    @Mod.EventBusSubscriber(modid=Content.NS,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Setup {@SubscribeEvent public static void setup(FMLClientSetupEvent event){ClientRegistry.registerKeyBinding(KEY);}}
}
