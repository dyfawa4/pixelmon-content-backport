import java.util.*;
import java.nio.file.*;
import local.pixelmon.backport.*;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.api.pokemon.PokemonFactory;
import com.pixelmonmod.pixelmon.api.registries.*;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.client.storage.ClientStorageManager;
import com.pixelmonmod.pixelmon.client.gui.PixelmonOverlayScreen;
import com.pixelmonmod.pixelmon.enums.EnumMegaItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
/** Automatic screenshots only in a separately named private developer instance. */
@Mod("backport_field_ui_test")
public final class FieldWheelClientVerification {
 @Mod.EventBusSubscriber(modid="backport_field_ui_test",value=Dist.CLIENT)
 public static class Events {
  static boolean loading,prepared,done;static int ticks,frames,phase;static FieldMegaScreen screen;
  static void require(boolean b,String s){if(!b)throw new AssertionError(s);}
  @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
   if(e.phase!=TickEvent.Phase.END || done)return;Minecraft mc=Minecraft.func_71410_x();
   try{
    if(!loading && mc.field_71462_r!=null && mc.field_71462_r.getClass().getSimpleName().equals("MainMenuScreen")){loading=true;mc.func_238191_a_("backport-qa-single");return;}
    if(mc.field_71439_g==null || mc.field_71441_e==null || mc.func_71401_C()==null)return;
    if(!prepared){
     prepared=true;UUID player=mc.field_71439_g.func_110124_au();var server=mc.func_71401_C();
     server.execute(()->{
      var owner=server.func_184103_al().func_177451_a(player);var party=StorageProxy.getParty(owner);party.starterPicked=true;party.unlockMega(true);party.setMegaItem(EnumMegaItem.BraceletORAS,true);
      for(int i=0;i<6;i++)party.set(i,null);
      for(int i=0;i<3;i++){
       var p=PokemonFactory.create(PixelmonSpecies.fromNationalDex(i==1?149:6));p.setLevelNum(70);p.initialize();
       p.getMoveset().clear();p.getMoveset().add(new Attack("Fly"));p.getMoveset().add(new Attack("Flamethrower"));
       p.setHeldItem(new ItemStack(i==1?Backport.item("dragoninite"):i==0?PixelmonItems.charizardite_x:PixelmonItems.charizardite_y));party.set(i,p);p.getOrSpawnPixelmon(owner);
      }
     });return;
    }
    if(screen==null && ++ticks>220 && ClientStorageManager.party!=null && ClientStorageManager.party.get(1)!=null){
     screen=new FieldMegaScreen(0);mc.func_147108_a(screen);
     var released=FieldMegaScreen.class.getDeclaredMethod("released");released.setAccessible(true);require(((List<?>)released.invoke(screen)).size()==3,"Three released targets not shown");
     for(int[] size:new int[][]{{320,240},{427,267},{640,400},{1280,800}})screen.func_231158_b_(mc,size[0],size[1]);
     screen.func_231158_b_(mc,mc.func_228018_at_().func_198107_o(),mc.func_228018_at_().func_198087_p());
    }
   }catch(Throwable failure){done=true;System.err.println("BACKPORT_FIELD_WHEEL_FAIL");failure.printStackTrace();mc.func_71400_g();}
  }
  @SubscribeEvent public static void render(TickEvent.RenderTickEvent e){
   if(done || screen==null || e.phase!=TickEvent.Phase.END || ++frames<20)return;frames=0;Minecraft mc=Minecraft.func_71410_x();
   try{
    Path out=Paths.get("../field-wheel-ui");Files.createDirectories(out);
    try(NativeImage img=ScreenShotHelper.func_198052_a(mc.func_228018_at_().func_198105_m(),mc.func_228018_at_().func_198083_n(),mc.func_147110_a())){img.func_209270_a(out.resolve("wheel-"+phase+".png"));}
    if(phase++==0){
     PixelmonOverlayScreen.selectedMoveSkill="test-old-target";
     var cycle=FieldMegaScreen.class.getDeclaredMethod("cycle",int.class);cycle.setAccessible(true);cycle.invoke(screen,1);
     var id=FieldMegaScreen.class.getDeclaredField("identity");id.setAccessible(true);
     require(id.get(screen).equals(ClientStorageManager.party.get(1).getUUID()) && ClientStorageManager.party.getSelectedSlot()==1,"Wheel switched to wrong target");
     require(PixelmonOverlayScreen.selectedMoveSkill==null,"Old skill followed another Pokemon");
    }else if(phase==2){mc.func_147108_a(new FieldMegaScreen.Skills(1));require(mc.field_71462_r instanceof FieldMegaScreen.Skills,"Native selector intercepted recursively");}
    else {done=true;System.out.println("BACKPORT_FIELD_WHEEL_PASS native wheel textures/render, three released targets, UUID selection, cleared old skill, native selector, four sizes");mc.func_71400_g();}
   }catch(Throwable failure){done=true;System.err.println("BACKPORT_FIELD_WHEEL_FAIL");failure.printStackTrace();mc.func_71400_g();}
  }
 }
}
