package qa;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.GuiScreenEvent;
import com.pixelmonmod.pixelmon.client.gui.inventory.InventoryPixelmonExtendedScreen;
import java.util.Optional;
public final class TrashSlotCases {
 public static void run()throws Exception{
  var mc=Minecraft.func_71410_x();mc.field_71439_g.field_71075_bZ.field_75098_d=false;
  var screen=new InventoryPixelmonExtendedScreen(mc.field_71439_g);mc.func_147108_a(screen);
  Class<?> trash=Class.forName("net.blay09.mods.trashslot.TrashSlot");
  if(!trash.getField("isServerSideInstalled").getBoolean(null))throw new AssertionError("Native TrashSlot server handshake");
  var handler=((Optional<?>)trash.getField("trashSlotGui").get(null)).orElseThrow();var gui=handler.getClass().getMethod("getGuiTrashSlot").invoke(handler);
  if(gui==null)throw new AssertionError("Pixelmon GUI initialized trash slot");
  var visible=gui.getClass().getMethod("isVisible");var keyboard=handler.getClass().getMethod("onGuiKeyboard",GuiScreenEvent.KeyboardKeyPressedEvent.Post.class);
  boolean before=(Boolean)visible.invoke(gui);
  keyboard.invoke(handler,new GuiScreenEvent.KeyboardKeyPressedEvent.Post(screen,84,0,0));
  if((Boolean)visible.invoke(gui)==before)throw new AssertionError("T toggles Pixelmon trash slot");
  if(!(Boolean)visible.invoke(gui))keyboard.invoke(handler,new GuiScreenEvent.KeyboardKeyPressedEvent.Post(screen,84,0,0));
  System.out.println("TRASHSLOT_PASS native 12.2.1 server handshake, Pixelmon screen init, T toggle, visible slot");
 }
}
