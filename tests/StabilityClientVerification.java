import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import com.pixelmonmod.pixelmon.client.gui.npc.BackportDialogueLayoutProbe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.*;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.*;
import net.minecraft.util.text.*;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.api.distmarker.Dist;

/** Private QA mod only: translated real Configured screen and repeated isolated world lifecycle. */
@Mod("backport_stability_probe")
public final class StabilityClientVerification {
    @Mod.EventBusSubscriber(modid="backport_stability_probe",value=Dist.CLIENT)
    public static final class Events {
        static int stage,ticks,cycles,scans,labels;
        static boolean done;static Object drawFolder;
        static long deadline=System.nanoTime()+240_000_000_000L;
        static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
        static void result(String text)throws Exception{
            Files.writeString(Paths.get("stability-result.txt"),text+"\n");
            System.out.println("BACKPORT_STABILITY_"+text);
        }
        static void entries(Object entry)throws Exception{
            Class<?> api=Class.forName("com.mrcrayfish.configured.api.IConfigEntry");
            Object value=api.getMethod("getValue").invoke(entry);
            if(value!=null){
                Class<?> values=Class.forName("com.mrcrayfish.configured.api.IConfigValue");
                String key=(String)values.getMethod("getTranslationKey").invoke(value);
                Class<?> screenType=Class.forName("com.mrcrayfish.configured.client.screen.ConfigScreen");Method label=screenType.getDeclaredMethod("createLabelFromHolder",values);label.setAccessible(true);
                require(key!=null,"Missing detector label key");
                require(((String)label.invoke(null,value)).matches(".*[\\u4e00-\\u9fff].*"),"English detector label: "+key);
                String comment=(String)values.getMethod("getComment").invoke(value);
                require(comment!=null && comment.matches("(?s).*[\\u4e00-\\u9fff].*"),"English detector tooltip: "+key);
                labels++;return;
            }
            String name=(String)api.getMethod("getEntryName").invoke(entry);
            require(name.matches(".*[\\u4e00-\\u9fff].*"),"English detector folder: "+name);
            for(Object child:(List<?>)api.getMethod("getChildren").invoke(entry))entries(child);
        }
        static void config(Minecraft mc)throws Exception{
            ForgeConfigSpec spec=(ForgeConfigSpec)Class.forName("pixelmondetector.config.PDConfig").getField("COMMON_SPEC").get(null);
            ModConfig nativeConfig=(ModConfig)Class.forName("com.mrcrayfish.configured.util.ConfigHelper").getMethod("getModConfig",String.class).invoke(null,"pixelmondetector-common.toml");
            require(nativeConfig!=null,"Missing detector native config");
            Class<?> adapter=Class.forName("com.mrcrayfish.configured.impl.ForgeConfig");
            Object cfg=adapter.getConstructor(ModConfig.class,ForgeConfigSpec.class).newInstance(nativeConfig,spec);
            Method fileLabel=Class.forName("com.mrcrayfish.configured.client.screen.ModConfigSelectionScreen").getDeclaredMethod("createLabelFromModConfig",Class.forName("com.mrcrayfish.configured.api.IModConfig"));fileLabel.setAccessible(true);require("通用配置".equals(fileLabel.invoke(null,cfg)),"Common file label");
            Object root=adapter.getMethod("getRoot").invoke(cfg);System.out.println("BACKPORT_CONFIG_DIAGNOSTIC locale="+mc.field_71474_y.field_74363_ab+" spec="+spec.getRaw(java.util.Arrays.asList("general","allowDetector"))+" catalog="+local.pixelmon.backport.client.DetectorConfigText.text("pixelmondetector.config.category.root"));entries(root);
            Class<?> entryApi=Class.forName("com.mrcrayfish.configured.api.IConfigEntry");for(Object child:(List<?>)entryApi.getMethod("getChildren").invoke(root))if("探测显示".equals(entryApi.getMethod("getEntryName").invoke(child)))drawFolder=child;require(drawFolder!=null,"Missing draws folder");
            require(labels==25,"Detector coverage: "+labels);
            Class<?> configApi=Class.forName("com.mrcrayfish.configured.api.IModConfig");
            Screen screen=(Screen)Class.forName("com.mrcrayfish.configured.client.screen.ConfigScreen")
                .getConstructor(Screen.class,ITextComponent.class,configApi,ResourceLocation.class)
                .newInstance(mc.field_71462_r,new StringTextComponent("PixelmonDetector"),cfg,new ResourceLocation("minecraft","textures/gui/options_background.png"));
            require(screen.func_231171_q_().getString().equals("宝可梦探测器"),"Detector screen title: "+screen.func_231171_q_().getString());
            mc.func_147108_a(screen);
        }
        @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
            if(event.phase!=TickEvent.Phase.END || done)return;
            Minecraft mc=Minecraft.func_71410_x();
            try{
                if(System.nanoTime()>deadline)throw new AssertionError("Lifecycle timed out at stage "+stage+", cycles="+cycles);
                if(stage==0){
                    if(!(mc.field_71462_r instanceof MainMenuScreen))return;
                    config(mc);stage=1;ticks=0;
                }else if(stage==1 && ++ticks>=20){
                    try(NativeImage img=ScreenShotHelper.func_198052_a(mc.func_228018_at_().func_198105_m(),mc.func_228018_at_().func_198083_n(),mc.func_147110_a())){img.func_209270_a(Paths.get("detector-config-root.png"));}
                    if(!Boolean.getBoolean("backport.stability.worldCycles")){Class<?> entryApi=Class.forName("com.mrcrayfish.configured.api.IConfigEntry");Screen child=(Screen)Class.forName("com.mrcrayfish.configured.client.screen.ConfigScreen").getConstructor(Screen.class,ITextComponent.class,ResourceLocation.class,entryApi).newInstance(mc.field_71462_r,new StringTextComponent("PixelmonDetector > Draws"),new ResourceLocation("minecraft","textures/gui/options_background.png"),drawFolder);mc.func_147108_a(child);require(child.func_231171_q_().getString().equals("宝可梦探测器 > 探测显示"),"Draws title");stage=4;ticks=0;return;}mc.func_238191_a_("backport-qa-single");stage=2;ticks=0;
                }else if(stage==4 && ++ticks>=20){
                    try(NativeImage img=ScreenShotHelper.func_198052_a(mc.func_228018_at_().func_198105_m(),mc.func_228018_at_().func_198083_n(),mc.func_147110_a())){img.func_209270_a(Paths.get("detector-config-draws.png"));}
                    String sample=BackportDialogueLayoutProbe.TEXT;
                    for(String text:new String[]{sample,sample.repeat(10),"中英文 Mixed dialogue 50%\n"+sample}){
                        local.pixelmon.backport.client.NpcDialogueLayout.Layout layout=local.pixelmon.backport.client.NpcDialogueLayout.layout(mc.field_71466_p,text,200);
                        require(layout.height()<=46.01f,"Dialogue height overflow");
                        for(net.minecraft.util.IReorderingProcessor line:layout.lines)require(mc.field_71466_p.func_243245_a(line)*layout.scale<=200.01f,"Dialogue width overflow");
                    }
                    mc.func_147108_a(new BackportDialogueLayoutProbe());stage=5;ticks=0;
                }else if(stage==5 && ++ticks>=20){
                    try(NativeImage img=ScreenShotHelper.func_198052_a(mc.func_228018_at_().func_198105_m(),mc.func_228018_at_().func_198083_n(),mc.func_147110_a())){img.func_209270_a(Paths.get("npc-dialogue-wrapped.png"));}
                    result("PASS detector labels="+labels+" tooltips/categories/Common/root/draws; dialogue width/height and native helper rendering; world cycles skipped");done=true;
                }else if(stage==2){
                    if(mc.field_71441_e==null || mc.field_71439_g==null)return;
                    if(++ticks%10==0){Class.forName("pixelmondetector.entities.EntityFinder").getMethod("getAndDraw",Minecraft.class).invoke(null,mc);scans++;}
                    if(ticks<120)return;
                    if(++cycles==3){result("PASS cycles="+cycles+" scans="+scans+" labels="+labels);done=true;return;}
                    mc.func_71403_a(null);mc.func_147108_a(new MainMenuScreen());stage=3;ticks=0;
                }else if(stage==3){
                    if(mc.field_71441_e!=null || mc.func_71401_C()!=null)return;
                    if(++ticks<40)return;
                    mc.func_238191_a_("backport-qa-single");stage=2;ticks=0;
                }
            }catch(Throwable failure){
                done=true;failure.printStackTrace();
                try{result("FAIL stage="+stage+" "+failure);}catch(Exception io){io.printStackTrace();}
            }
        }
    }
}
