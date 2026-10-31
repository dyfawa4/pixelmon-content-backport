package qa;
import java.nio.file.*;import java.util.*;
import net.minecraftforge.fml.common.Mod;import net.minecraftforge.eventbus.api.SubscribeEvent;import net.minecraftforge.event.TickEvent;import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.minecraft.client.Minecraft;import net.minecraft.util.math.BlockPos;import net.minecraft.util.Direction;import net.minecraft.util.math.vector.Vector3d;import net.minecraft.util.ResourceLocation;
import local.pixelmon.backport.*;import com.pixelmonmod.pixelmon.api.pokemon.*;import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;import com.pixelmonmod.pixelmon.api.storage.StorageProxy;import com.pixelmonmod.pixelmon.entities.pokeballs.*;import com.pixelmonmod.pixelmon.blocks.*;
@Mod("backport_plate110_probe") @Mod.EventBusSubscriber(modid="backport_plate110_probe")
public class Plate110Probe {
 static boolean opened,hidden,done;static int ticks,checks;
 static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 @SubscribeEvent public static void server(TickEvent.ServerTickEvent e){if(done||e.phase!=TickEvent.Phase.END)return;var server=ServerLifecycleHooks.getCurrentServer();if(server==null||server.func_184103_al().func_181057_v().isEmpty()||++ticks<100)return;try{
  var player=server.func_184103_al().func_181057_v().get(0);var world=player.func_71121_q();var mon=PokemonFactory.create(PixelmonSpecies.fromNationalDex(25));mon.initialize();StorageProxy.getParty(player).set(0,mon);
  var plate=(MovementPlateBlock)net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(new ResourceLocation("pixelmon:movement_plate"));var pos=new BlockPos(4,90,4);var ball=com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.PokeBallRegistry.getPokeBall("poke_ball").getValueUnsafe();
  var occupied=new OccupiedPokeBallEntity(world,player,0,ball);var empty=new EmptyPokeBallEntity(world);var entity=mon.getOrCreatePixelmon(world,4,90,4);Vector3d initial=new Vector3d(.12,.17,.08);var report=new ArrayList<String>();boolean baseline=Files.exists(Path.of("plate110-baseline"));
  for(var dir:List.of(Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST)){
   var state=plate.func_176223_P().func_206870_a(GenericRotatableBlock.FACING,dir);world.func_180501_a(pos,state,3);
   occupied.func_226286_f_(4.5,90.2,4.5);occupied.func_213317_d(initial);plate.func_196262_a(state,world,pos,occupied);boolean unchanged=occupied.func_213322_ci().equals(initial);report.add(dir+" occupiedUnchanged="+unchanged);check(unchanged!=baseline,"occupied plate velocity "+dir);
   for(var other:List.of(empty,entity,player)){other.func_226286_f_(4.5,90.2,4.5);other.func_213317_d(initial);plate.func_196262_a(state,world,pos,other);check(!other.func_213322_ci().equals(initial),"native passenger preserved "+dir+"/"+other.getClass().getSimpleName());}
  }
  occupied.field_70173_aa=401;occupied.func_70071_h_();check(!occupied.func_70089_S(),"native occupied lifetime cleanup retained");check(mon.getUUID().equals(StorageProxy.getParty(player).get(0).getUUID()),"party individual retained");
  Files.writeString(Path.of("plate110-pass.txt"),"PASS checks="+checks+" baseline="+baseline+" "+report);empty.func_70106_y();entity.func_70106_y();
 }catch(Throwable t){t.printStackTrace();try{Files.writeString(Path.of("plate110-fail.txt"),t.toString());}catch(Exception ignored){}}finally{done=true;}}
 @SubscribeEvent public static void client(TickEvent.ClientTickEvent e){if(e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.func_71410_x();if(done){mc.func_71400_g();return;}mc.field_71474_y.field_82881_y=false;if(!hidden){org.lwjgl.glfw.GLFW.glfwHideWindow(mc.func_228018_at_().func_198092_i());hidden=true;}if(!opened&&mc.field_71462_r instanceof net.minecraft.client.gui.screen.MainMenuScreen){opened=true;mc.func_238191_a_("backport-qa-single");}if(mc.field_71439_g!=null){com.pixelmonmod.pixelmon.client.ServerStorageDisplay.showStarterScreen=false;mc.func_147108_a(null);}}
}
