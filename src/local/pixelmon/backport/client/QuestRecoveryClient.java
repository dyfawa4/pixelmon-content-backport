package local.pixelmon.backport.client;
import local.pixelmon.backport.*;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=Content.NS,value=Dist.CLIENT)
public final class QuestRecoveryClient {
    private static boolean pending;
    private static long requested;
    private QuestRecoveryClient(){}
    private static boolean exists()throws ReflectiveOperationException{return (Boolean)Class.forName("dev.ftb.mods.ftbquests.client.ClientQuestFile").getMethod("exists").invoke(null);}
    public static boolean request(){
        Minecraft mc=Minecraft.func_71410_x();if(mc.field_71441_e==null||mc.field_71439_g==null||mc.func_147114_u()==null)return false;
        try{
            if(exists())return false;
            // A server genuinely missing this extension cannot service a resync.
            if(!QuestSyncRecovery.CHANNEL.isRemotePresent(mc.func_147114_u().func_147298_b()))return false;
            if(!pending){pending=true;requested=System.currentTimeMillis();QuestSyncRecovery.CHANNEL.sendToServer(new QuestSyncRecovery.Request());
                mc.field_71439_g.func_146105_b(new TranslationTextComponent("pixelmon_backport.quests.syncing"),true);}
            return true;
        }catch(ReflectiveOperationException failure){return false;}
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!pending)return;
        Minecraft mc=Minecraft.func_71410_x();if(mc.field_71441_e==null||mc.field_71439_g==null){pending=false;return;}
        try{
            if(exists()){
                Object file=Class.forName("dev.ftb.mods.ftbquests.client.ClientQuestFile").getField("INSTANCE").get(null);
                Object team=file.getClass().getField("self").get(file);
                if(team!=null&&!"Loading...".equals(team.getClass().getField("name").get(team))){
                    pending=false;Object proxy=Class.forName("dev.ftb.mods.ftbquests.FTBQuests").getField("PROXY").get(null);
                    proxy.getClass().getMethod("openGui").invoke(proxy);return;
                }
            }
        }catch(ReflectiveOperationException failure){pending=false;}
        if(System.currentTimeMillis()-requested>10000){pending=false;mc.field_71439_g.func_146105_b(new TranslationTextComponent("pixelmon_backport.quests.retry"),true);}
    }
}
