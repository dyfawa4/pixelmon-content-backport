package local.pixelmon.backport;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraftforge.client.event.GuiScreenEvent;
/** Called only by a successfully decoded, native server-to-client TrashSlot packet. */
public final class TrashSlotCompat {
    private TrashSlotCompat(){}
    public static void received(){
        try{
            Class<?> trash=Class.forName("net.blay09.mods.trashslot.TrashSlot");
            trash.getField("isServerSideInstalled").setBoolean(null,true);
            var handler=((Optional<?>)trash.getField("trashSlotGui").get(null)).orElse(null);
            if(handler==null)return;
            var screen=Minecraft.func_71410_x().field_71462_r;
            if(screen instanceof ContainerScreen && handler.getClass().getMethod("getGuiTrashSlot").invoke(handler)==null){
                var init=new GuiScreenEvent.InitGuiEvent.Post(screen,Collections.emptyList(),widget->{},widget->{});
                handler.getClass().getMethod("onInitGui",GuiScreenEvent.InitGuiEvent.Post.class).invoke(handler,init);
            }
        }catch(ReflectiveOperationException e){org.apache.logging.log4j.LogManager.getLogger(Content.NS).warn("Cannot initialize optional TrashSlot compatibility",e);}
    }
}
