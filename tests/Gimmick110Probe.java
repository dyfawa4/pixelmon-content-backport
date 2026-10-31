package qa;
import java.nio.file.*;import java.util.*;
import net.minecraftforge.fml.common.Mod;import net.minecraftforge.eventbus.api.SubscribeEvent;import net.minecraftforge.event.TickEvent;import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.minecraft.client.Minecraft;import local.pixelmon.backport.*;
import com.pixelmonmod.pixelmon.api.pokemon.*;import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import com.pixelmonmod.pixelmon.client.gui.battles.*;
@Mod("backport_gimmick110_probe") @Mod.EventBusSubscriber(modid="backport_gimmick110_probe")
public class Gimmick110Probe {
 static boolean opened,hidden,done,ready;static int ticks,checks;static Pokemon mon;
 static void check(boolean b,String message){checks++;if(!b)throw new AssertionError(message);}
 static void fail(Throwable t){t.printStackTrace();try{Files.writeString(Path.of("gimmick110-fail.txt"),t.toString());}catch(Exception ignored){}done=true;}
 @SubscribeEvent public static void server(TickEvent.ServerTickEvent e){if(done||ready||e.phase!=TickEvent.Phase.END)return;var server=ServerLifecycleHooks.getCurrentServer();if(server==null||server.func_184103_al().func_181057_v().isEmpty()||++ticks<100)return;try{
  var player=server.func_184103_al().func_181057_v().get(0);mon=PokemonFactory.create(PixelmonSpecies.fromNationalDex(6));mon.initialize();
  var npc=new com.pixelmonmod.pixelmon.entities.npcs.NPCTrainer(player.field_70170_p);npc.getPokemonStorage().set(0,mon);var team=new TrainerParticipant(npc,1);var w=team.allPokemon[0];
  var field=Tera.class.getDeclaredField("STATES");field.setAccessible(true);@SuppressWarnings("unchecked") var states=(Map<PixelmonWrapper,Tera.State>)field.get(null);var state=new Tera.State(w);state.active=true;states.put(w,state);
  check(!w.canMegaEvolve(),"server Mega eligibility");check(!w.canDynamax(),"server Max eligibility");check(!w.canUltraBurst(),"server Ultra eligibility");check(!w.megaEvolve(),"direct Mega execution rejected");check(!w.dynamax(false,1),"direct Max execution rejected");check(!w.registerZMove(),"direct Z execution rejected");
  var copy=new PixelmonWrapper(w);check(Tera.active(copy)&&!copy.canDynamax(),"simulation copy retains restriction");states.remove(copy);states.remove(w);npc.func_70106_y();ready=true;
 }catch(Throwable t){fail(t);}}
 static void clientCases(){var oldSnapshot=TeraClient.snapshot;var oldReply=TeraClient.battle;var oldRaid=TeraRaidClient.status;try{
  var b=new ClientBattleManager();b.battleControllerIndex=771;b.battleEnded=false;var data=new PixelmonClientData(mon);var other=new PixelmonClientData(mon);other.pokemonUUID=UUID.randomUUID();b.displayedOurPokemon=new PixelmonClientData[]{data};b.fullOurPokemon=List.of(data);b.currentPokemon=0;
  var snapshot=new TeraNetwork.Snapshot(771);snapshot.active.put(data.pokemonUUID,2);TeraClient.snapshot=snapshot;TeraClient.battle=null;TeraRaidClient.status=null;
  check(TeraRaidGimmicks.blocked(b,data),"active snapshot recognized");check(!b.canMegaEvolve(data)&&!b.canDynamax(data)&&!b.canUltraBurst(data)&&!b.canUseZMove(data),"all data overloads disabled");
  check(!b.canMegaEvolve()&&!b.canDynamax()&&!b.canUltraBurst()&&!b.canUseZMove(),"current Pokemon overloads disabled");check(!TeraRaidGimmicks.blocked(b,other),"other member available");
  b.megaEvolving=b.dynamaxing=b.showZMoves=true;TeraRaidGimmicks.clear(b);check(!b.megaEvolving&&!b.dynamaxing&&!b.showZMoves,"stale native selections cleared");
  TeraClient.snapshot=new TeraNetwork.Snapshot(770);check(!TeraRaidGimmicks.blocked(b,data),"stale battle isolated");TeraClient.snapshot=snapshot;snapshot.active.clear();check(!TeraRaidGimmicks.blocked(b,data),"ordinary battle preserved");snapshot.teraRaid=true;check(TeraRaidGimmicks.blocked(b,other),"Tera raid restriction preserved");snapshot.teraRaid=false;
  var reply=new TeraNetwork.Reply();reply.battle=771;reply.pokemon=data.pokemonUUID;reply.active=true;TeraClient.battle=reply;check(TeraRaidGimmicks.blocked(b,data),"reply fallback");check(!TeraRaidGimmicks.blocked(b,other),"reply Pokemon identity");reply.active=false;reply.selected=true;check(!TeraRaidGimmicks.blocked(b,data),"queued Tera may be replaced");reply.active=true;b.battleEnded=true;check(!TeraRaidGimmicks.blocked(b,data),"ended battle isolated");
 }finally{TeraClient.snapshot=oldSnapshot;TeraClient.battle=oldReply;TeraRaidClient.status=oldRaid;}}
 @SubscribeEvent public static void client(TickEvent.ClientTickEvent e){if(e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.func_71410_x();if(done){mc.func_71400_g();return;}mc.field_71474_y.field_82881_y=false;if(!hidden){org.lwjgl.glfw.GLFW.glfwHideWindow(mc.func_228018_at_().func_198092_i());hidden=true;}if(!opened&&mc.field_71462_r instanceof net.minecraft.client.gui.screen.MainMenuScreen){opened=true;mc.func_238191_a_("backport-qa-single");}if(mc.field_71439_g!=null){com.pixelmonmod.pixelmon.client.ServerStorageDisplay.showStarterScreen=false;mc.func_147108_a(null);}if(ready)try{clientCases();Files.writeString(Path.of("gimmick110-pass.txt"),"PASS native server/client checks="+checks);done=true;}catch(Throwable t){fail(t);}}
}
