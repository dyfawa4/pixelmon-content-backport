import java.nio.file.*;
import java.util.*;
import com.pixelmonmod.pixelmon.api.dialogue.*;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.client.gui.custom.dialogue.DialogueScreen;
import com.pixelmonmod.pixelmon.comm.packetHandlers.dialogue.DialogueNextActionPacket.DialogueGuiAction;
import com.pixelmonmod.pixelmon.quests.actions.actions.EndDialogueAction;
import com.pixelmonmod.pixelmon.quests.quest.Arguments;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

/** One opt-in client run: quest reply unlock and native END_DIALOGUE close. */
@Mod.EventBusSubscriber(modid="backport_tera_visual_test")
public class DialogueRecoveryGameTest {
    static volatile int clicks;static boolean opened;
    static boolean enabled(){return Files.isRegularFile(Path.of("dialogue-recovery-private-test"));}
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||opened||!enabled())return;
        var server=ServerLifecycleHooks.getCurrentServer();if(server==null||server.func_184103_al().func_181057_v().isEmpty())return;
        var p=server.func_184103_al().func_181057_v().get(0);if(p.field_70173_aa<160)return;
        opened=true;Dialogue.builder().setName("对话修复测试").setText("原生任务回复与再见关闭测试").addChoice(new Choice("再见",event->{
            if(++clicks==1)event.setAction(DialogueGuiAction.CONTINUE);
            else new EndDialogueAction().execute(null,null,StorageProxy.getParty(p).getQuestData(true),null,Arguments.empty());
        })).open(p);
    }
    @Mod.EventBusSubscriber(modid="backport_tera_visual_test",value=net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static class Client {
        static int phase,ticks,wait,closing;static boolean finished;
        static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
        @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
            if(e.phase!=TickEvent.Phase.END||!enabled())return;var mc=net.minecraft.client.Minecraft.func_71410_x();
            if(finished){if(++closing==30)mc.func_71400_g();return;}
            if(++ticks%10!=0)return;
            try{
                if(phase==0){if(!(mc.field_71462_r instanceof DialogueScreen))return;
                    var screen=(DialogueScreen)mc.field_71462_r;
                    var quest=screen.getClass().getDeclaredField("quest");quest.setAccessible(true);quest.set(null,UUID.randomUUID());
                    screen.func_231044_a_(screen.field_230708_k_/2d-3,(screen.field_230709_l_-166)/2d+75,0);phase=1;return;
                }
                if(phase==1){if(clicks<1){check(++wait<10,"Choice not delivered");return;}
                    var screen=(DialogueScreen)mc.field_71462_r;
                    var pause=screen.getClass().getDeclaredField("pause");pause.setAccessible(true);check(!pause.getBoolean(screen),"Quest reply left choice locked");
                    screen.func_231044_a_(screen.field_230708_k_/2d-3,(screen.field_230709_l_-166)/2d+75,0);phase=2;wait=0;return;
                }
                if(phase==2){if(clicks<2){check(++wait<10,"Retry remained locked");return;}
                    check(!(mc.field_71462_r instanceof DialogueScreen),"END_DIALOGUE did not close client screen");
                    Files.writeString(Path.of("dialogue-recovery-result.json"),"{\"passed\":true,\"questReplyUnlocked\":true,\"nativeEndDialogueClosed\":true,\"submittedChoices\":2}");finished=true;
                }
            }catch(Throwable failure){failure.printStackTrace();try{Files.writeString(Path.of("dialogue-recovery-result.json"),"{\"passed\":false,\"error\":\""+failure.toString()+"\"}");}catch(Exception ignored){}finished=true;}
        }
    }
}
