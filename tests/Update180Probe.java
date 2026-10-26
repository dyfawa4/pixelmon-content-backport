package qa;
import java.nio.file.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.util.ScreenShotHelper;
import com.pixelmonmod.pixelmon.client.ServerStorageDisplay;
import com.pixelmonmod.pixelmon.client.gui.starter.ChooseStarterScreen;

/** Opt-in, disposable save only. Never runs against a normal player world. */
@Mod("backport_update180_probe") @Mod.EventBusSubscriber(modid="backport_update180_probe")
public final class Update180Probe {
    static final String WORLD="backport-update-180";
    static int r13Warmup;static boolean opened,hidden,ran;static volatile int done;static int age;
    static boolean enabled(){return WORLD.equals(System.getProperty("backport.manual.world"));}
    static void report(String message){try{Files.writeString(Path.of("update180-result.txt"),message+"\n");}catch(Exception e){throw new RuntimeException(e);}System.out.println(message);}
    @SubscribeEvent public static void fixtureScreen(net.minecraftforge.client.event.GuiOpenEvent event){if(enabled()&&(Files.exists(Path.of("r7-only"))||Files.exists(Path.of("r9-teams-only"))||Files.exists(Path.of("r12-status-only"))||Files.exists(Path.of("r12-repairs-only")))&&event.getGui() instanceof com.pixelmonmod.pixelmon.client.gui.battles.BattleScreen&&!Update180R12ExtraCases.allowBattleUI)event.setCanceled(true);}
    @SubscribeEvent public static void server(TickEvent.ServerTickEvent event){
        if(!enabled()||event.phase!=TickEvent.Phase.END||ran)return;
        var server=ServerLifecycleHooks.getCurrentServer();
        if(server==null||!server.func_240776_a_(net.minecraft.world.storage.FolderName.field_237253_i_).normalize().getFileName().toString().equals(WORLD)||server.func_184103_al().func_181057_v().isEmpty())return;
        if((Files.exists(Path.of("r15-only"))||Files.exists(Path.of("potion-only"))||Files.exists(Path.of("r13-append-only")))&&++r13Warmup<120)return;
        ran=true;try{var player=server.func_184103_al().func_181057_v().get(0);if(Files.exists(Path.of("r15-only")))Update180R15Cases.server(player);else if(Files.exists(Path.of("r13-append-only")))Update180R13AppendCases.server(player);else if(Files.exists(Path.of("potion-only")))Update180PotionCases.server(player);else if(Files.exists(Path.of("loot-names-only")))Update180LootNameCases.server(player);else if(Files.exists(Path.of("pc-sync-only")))Update180PCSyncCases.server(player);else if(Files.exists(Path.of("r12-repairs-only"))){Update180R12ExtraCases.server(player);Update180R11Cases.setup(player);}else if(Files.exists(Path.of("r12-status-only")))Update180R12StatusCases.run(player);else if(Files.exists(Path.of("r11-only")))Update180R11Cases.setup(player);else if(Files.exists(Path.of("trainer-display-only")))Update180TrainerDisplayCases.setup(player);else if(Files.exists(Path.of("r9-consumable-only"))){Update180R9TeamCases.control(player);Update180R9TeamCases.potion(player);}else if(Files.exists(Path.of("r9-teams-only")))Update180R9TeamCases.run(player);else if(Files.exists(Path.of("r9-repairs-only")))Update180R9RepairCases.setup(player);else if(Files.exists(Path.of("r9-only")))Update180R9Cases.setup(player);else if(Files.exists(Path.of("r8-only")))Update180R8Cases.run(player);else if(Files.exists(Path.of("r7-only"))){Update180R6Cases.run(player);Update180R7Cases.run(player);}else if(Files.exists(Path.of("r6-only")))Update180R6Cases.run(player);else if(Files.exists(Path.of("expansion-only")))Update180ExpansionCases.run(player);else if(Files.exists(Path.of("riding-audit-only"))){}else if(Files.exists(Path.of("fishing-only")))Update180FishingCases.run(player);else if(Files.exists(Path.of("ride-repair-only")))Update180RidingRepairCases.setup(player);else if(Files.exists(Path.of("repair-only")))Update180RepairCases.server(player);else if(Files.exists(Path.of("strategy-only")))Update180StrategyCases.run(player);else if(Files.exists(Path.of("final-only"))){Update180Cases.run(player);Update180ShieldCases.run(player);Update180MomentumCases.run(player);Update180AdviceCases.run(player);Update180VisualCases.run(player);Update180AppendCases.run(player);Update180StrategyCases.run(player);}else if(Files.exists(Path.of("append-only")))Update180AppendCases.run(player);else if(Files.exists(Path.of("visual-only")))Update180VisualCases.run(player);else if(Files.exists(Path.of("advice-only")))Update180AdviceCases.run(player);else if(Files.exists(Path.of("shield-only")))Update180ShieldCases.run(player);else if(Files.exists(Path.of("momentum-only")))Update180MomentumCases.run(player);else Update180Cases.run(player);report("UPDATE180_PASS targeted native checks");done=1;}
        catch(Throwable error){error.printStackTrace();report("UPDATE180_FAIL "+error);done=2;}
    }
    @SubscribeEvent public static void client(TickEvent.ClientTickEvent event){
        if(!enabled()||event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.func_71410_x();
        mc.field_71474_y.field_82881_y=false;
        if(!hidden){org.lwjgl.glfw.GLFW.glfwHideWindow(mc.func_228018_at_().func_198092_i());hidden=true;}
        if(!opened&&mc.field_71462_r instanceof MainMenuScreen){opened=true;mc.func_238191_a_(WORLD);}
        if(mc.field_71439_g!=null){ServerStorageDisplay.showStarterScreen=false;if(mc.field_71462_r instanceof ChooseStarterScreen)mc.func_147108_a(null);}
        if(done>0&&Files.exists(Path.of("r15-only"))){age++;try{if(done==1)Update180R15Cases.client(age);else if(age>25)mc.func_71400_g();}catch(Throwable e){e.printStackTrace();report("UPDATE180_FAIL r15 "+e);mc.func_71400_g();}return;}
        if(done>0&&Files.exists(Path.of("r13-append-only"))){age++;try{if(done==1)Update180R13AppendCases.client(age);else if(age>25)mc.func_71400_g();}catch(Throwable e){e.printStackTrace();report("UPDATE180_FAIL r13 append "+e);mc.func_71400_g();}return;}
        if(done>0&&Files.exists(Path.of("potion-only"))){age++;try{if(done==1)Update180PotionCases.client(age);else if(age>25)mc.func_71400_g();}catch(Throwable e){e.printStackTrace();report("UPDATE180_FAIL potion "+e);mc.func_71400_g();}return;}
        if(done>0&&Files.exists(Path.of("loot-names-only"))){age++;try{if(done==1)Update180LootNameCases.client(age);else if(age>25)mc.func_71400_g();}catch(Throwable e){e.printStackTrace();report("UPDATE180_FAIL loot names "+e);mc.func_71400_g();}return;}
        if(done>0&&Files.exists(Path.of("pc-sync-only"))){age++;try{if(done==1)Update180PCSyncCases.client(age);else if(age>25)mc.func_71400_g();}catch(Throwable e){e.printStackTrace();report("UPDATE180_FAIL PC sync "+e);mc.func_71400_g();}return;}
        if(done>0&&Files.exists(Path.of("r12-repairs-only"))){age++;try{if(age==20)mc.func_147108_a(null);if(done==1){Update180R11Cases.client(age);Update180R12ExtraCases.client(age);}else if(age>25)mc.func_71400_g();}catch(Throwable e){e.printStackTrace();report("UPDATE180_FAIL r12 repairs "+e);mc.func_71400_g();}return;}
        if(done>0&&Files.exists(Path.of("r12-status-only"))){age++;if(done==1)Update180R12StatusCases.client(age);else if(age>25)mc.func_71400_g();return;}
        if(done>0&&Files.exists(Path.of("r11-only"))){age++;try{if(age==20)mc.func_147108_a(null);if(done==1)Update180R11Cases.client(age);else if(age>25)mc.func_71400_g();}catch(Throwable e){e.printStackTrace();report("UPDATE180_FAIL r11 "+e);mc.func_71400_g();}return;}
        if(done>0&&Files.exists(Path.of("trainer-display-only"))){age++;try{if(age==20)mc.func_147108_a(null);if(done==1)Update180TrainerDisplayCases.client(age);else if(age>25)mc.func_71400_g();}catch(Throwable e){e.printStackTrace();report("UPDATE180_FAIL trainer display "+e);mc.func_71400_g();}return;}
        if(done>0&&(Files.exists(Path.of("r9-teams-only"))||Files.exists(Path.of("r9-consumable-only")))){age++;try{if(age==20)mc.func_147108_a(null);if(done==1)Update180R9TeamCases.client(age);else if(age>25)mc.func_71400_g();}catch(Throwable e){e.printStackTrace();report("UPDATE180_FAIL r9 teams "+e);mc.func_71400_g();}return;}
        if(done>0&&Files.exists(Path.of("r9-repairs-only"))){age++;try{if(age==20)mc.func_147108_a(null);if(done==1)Update180R9RepairCases.client(age);else if(age>25)mc.func_71400_g();}catch(Throwable e){e.printStackTrace();report("UPDATE180_FAIL r9 repairs "+e);mc.func_71400_g();}return;}
        if(done>0&&Files.exists(Path.of("r9-only"))){age++;try{if(age==20)mc.func_147108_a(null);if(done==1)Update180R9Cases.client(age);else if(age>25)mc.func_71400_g();}catch(Throwable e){e.printStackTrace();report("UPDATE180_FAIL r9 models "+e);mc.func_71400_g();}return;}
        if((Files.exists(Path.of("r8-only"))||Files.exists(Path.of("r6-only")))&&done>0){if(++age>25)mc.func_71400_g();return;}
        if(Files.exists(Path.of("riding-audit-only")))return;
        if(done>0&&Files.exists(Path.of("r7-only"))){age++;try{if(age==20)mc.func_147108_a(null);if(done==1)Update180R7Cases.client(age);else if(age>25)mc.func_71400_g();}catch(Throwable e){e.printStackTrace();report("UPDATE180_FAIL r7 client "+e);mc.func_71400_g();}return;}
        if(done>0&&Files.exists(Path.of("ride-repair-only"))){age++;try{Update180RidingRepairCases.client(mc,age);}catch(Throwable e){e.printStackTrace();report("UPDATE180_FAIL riding client "+e);mc.func_71400_g();}return;}
        if(done>0&&Files.exists(Path.of("repair-only"))){age++;if(done==2){if(age>20)mc.func_71400_g();return;}try{
            if(age==20)mc.func_147108_a(null);
            if(age==35)Update180RepairCases.client(mc);
            if(age==45)ScreenShotHelper.func_148259_a(mc.field_71412_D,"qa-180-repair-names.png",mc.func_228018_at_().func_198107_o(),mc.func_228018_at_().func_198087_p(),mc.func_147110_a(),message->{});
            if(age==50)Update180RepairCases.openEditor(mc);
            if(age==65)ScreenShotHelper.func_148259_a(mc.field_71412_D,"qa-180-repair-editor.png",mc.func_228018_at_().func_198107_o(),mc.func_228018_at_().func_198087_p(),mc.func_147110_a(),message->{});
            if(age==70)Update180RepairCases.editorSave(mc);
            if(age==90)Update180RepairCases.saved(mc);
            if(age==110){if(!(mc.field_71462_r instanceof com.pixelmonmod.pixelmon.client.gui.npcEditor.TrainerEditorScreen))throw new AssertionError("Ordinary texture editor did not remain open");System.out.println("REPAIR180_EDITOR_RENDER_PASS custom sentinel and normal model selection");mc.func_147108_a(new Update180ArtScreen());}
            if(age==130)ScreenShotHelper.func_148259_a(mc.field_71412_D,"qa-180-repair-items.png",mc.func_228018_at_().func_198107_o(),mc.func_228018_at_().func_198087_p(),mc.func_147110_a(),message->{});
            if(age==140)mc.func_147108_a(null);
            if(age==145)Update180RepairCases.accuracyBattle(mc);
            if(age==235)Update180RepairCases.accuracyClient();
            if(age==245)ScreenShotHelper.func_148259_a(mc.field_71412_D,"qa-180-repair-battle.png",mc.func_228018_at_().func_198107_o(),mc.func_228018_at_().func_198087_p(),mc.func_147110_a(),message->{});
            if(age>260)mc.func_71400_g();
        }catch(Throwable error){error.printStackTrace();report("UPDATE180_FAIL repair client "+error);mc.func_71400_g();}return;}
        if(done>0&&Files.exists(Path.of("expansion-only"))){age++;try{if(age==20)mc.func_147108_a(null);if(age==40&&done==1)Update180ExpansionCases.client();if(age==50&&done==1)mc.func_147108_a(new Update180ArtScreen());if(age==70&&done==1)ScreenShotHelper.func_148259_a(mc.field_71412_D,"qa-180-new-lamps.png",mc.func_228018_at_().func_198107_o(),mc.func_228018_at_().func_198087_p(),mc.func_147110_a(),message->{});if(age>80)mc.func_71400_g();}catch(Throwable e){e.printStackTrace();report("UPDATE180_FAIL expansion client "+e);mc.func_71400_g();}return;}
        if(done>0){age++;if(age==20)mc.func_147108_a(null);
            if(age==35&&Files.exists(Path.of("visual-only")))for(var entity:mc.field_71441_e.func_217416_b())if(entity instanceof com.pixelmonmod.pixelmon.entities.npcs.NPCEntity){var npc=(com.pixelmonmod.pixelmon.entities.npcs.NPCEntity)entity;System.out.println("NPC_VISUAL_CLIENT "+npc.getClass().getSimpleName()+" alive="+npc.func_70089_S()+" pos="+npc.func_233580_cy_()+" name="+local.pixelmon.backport.client.NpcNameplates.name(npc));}
            if(age==45)ScreenShotHelper.func_148259_a(mc.field_71412_D,"qa-180-furnishings.png",mc.func_228018_at_().func_198107_o(),mc.func_228018_at_().func_198087_p(),mc.func_147110_a(),message->{});
            if(Files.exists(Path.of("visual-only"))&&done==1){if(age==50)mc.func_71401_C().func_213167_f(()->{var player=mc.func_71401_C().func_184103_al().func_181057_v().get(0);player.func_200619_a(player.func_71121_q(),19,75,23,180,4);});if(age==75)ScreenShotHelper.func_148259_a(mc.field_71412_D,"qa-180-npc-names.png",mc.func_228018_at_().func_198107_o(),mc.func_228018_at_().func_198087_p(),mc.func_147110_a(),message->{});if(age==80)mc.func_147108_a(new Update180ArtScreen());if(age==100)ScreenShotHelper.func_148259_a(mc.field_71412_D,"qa-180-items.png",mc.func_228018_at_().func_198107_o(),mc.func_228018_at_().func_198087_p(),mc.func_147110_a(),message->{});if(age==105)creative(mc);if(age>125)mc.func_71400_g();}
            else if(age>60)mc.func_71400_g();}
    }
    static void creative(Minecraft mc){try{
        mc.func_147108_a(new net.minecraft.client.gui.screen.inventory.CreativeScreen(mc.field_71439_g));if(!(mc.field_71462_r instanceof net.minecraft.client.gui.screen.inventory.CreativeScreen))throw new AssertionError("Expected actual creative screen, got "+mc.field_71462_r);var screen=(net.minecraft.client.gui.screen.inventory.CreativeScreen)mc.field_71462_r;
        var tab=net.minecraft.client.gui.screen.inventory.CreativeScreen.class.getDeclaredMethod("func_147050_b",net.minecraft.item.ItemGroup.class);tab.setAccessible(true);tab.invoke(screen,net.minecraft.item.ItemGroup.field_78027_g);
        var search=net.minecraft.client.gui.screen.inventory.CreativeScreen.class.getDeclaredMethod("func_147053_i");search.setAccessible(true);search.invoke(screen);
        var field=net.minecraft.client.gui.screen.inventory.ContainerScreen.class.getDeclaredField("field_147002_h");field.setAccessible(true);var container=(net.minecraft.client.gui.screen.inventory.CreativeScreen.CreativeContainer)field.get(screen);
        if(container.field_148330_a.stream().anyMatch(local.pixelmon.backport.TcgRemoval::item))throw new AssertionError("TCG leaked into creative search");
        System.out.println("TCG_CREATIVE_SEARCH_PASS actual native search refresh retains "+container.field_148330_a.size()+" non-TCG entries");
    }catch(Throwable e){e.printStackTrace();report("UPDATE180_FAIL creative "+e);}}
}
