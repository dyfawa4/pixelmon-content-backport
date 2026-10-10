import java.util.*;
import java.lang.reflect.*;
import java.nio.file.*;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.api.storage.PlayerPartyStorage;
import com.pixelmonmod.pixelmon.client.storage.ClientStorageManager;
import local.pixelmon.backport.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.inventory.CreativeScreen;
import net.minecraft.world.GameType;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.util.ScreenShotHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Native GUI input and screenshot check in the private client; restores the menu. */
@Mod.EventBusSubscriber(modid="backport_client_test",value=Dist.CLIENT)
public final class TeraScreenVerification {
    static boolean done,loading;static int frames,preview,readyTicks;
    static Screen previous;static PlayerPartyStorage previousParty;static TeraSettingsScreen screen;static TeraNetwork.Reply data;
    static CreativeScreen creative;static GameType previousMode;
    static void require(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static void click(TeraSettingsScreen s,int width,int height,float x,float y){float scale=TeraSettingsScreen.fit(width,height);s.func_231044_a_((width-480*scale)/2+x*scale,(height-330*scale)/2+y*scale,0);}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END || done || !Boolean.getBoolean("backport.ui.preview"))return;Minecraft mc=Minecraft.func_71410_x();
        if(!loading && mc.field_71462_r!=null && mc.field_71462_r.getClass().getSimpleName().equals("MainMenuScreen")){loading=true;mc.func_238191_a_("backport-qa-eternatus");return;}
        if(screen!=null && preview==2 && creative==null && mc.field_71442_b.func_178889_l()==GameType.CREATIVE)try{
            creative=new CreativeScreen(mc.field_71439_g);mc.func_147108_a(creative);
            Field page=CreativeScreen.class.getDeclaredField("tabPage");page.setAccessible(true);page.setInt(null,Backport.CREATIVE_TAB.getTabPage());
            Method select=CreativeScreen.class.getDeclaredMethod("func_147050_b",net.minecraft.item.ItemGroup.class);select.setAccessible(true);select.invoke(creative,Backport.CREATIVE_TAB);
            require(creative.func_212873_a_().field_148330_a.size()==Backport.REGISTERED.size()+8,"Native creative container incomplete");
        }catch(Throwable failure){done=true;System.err.println("BACKPORT_TERA_SCREEN_FAIL");failure.printStackTrace();}
        if(screen==null && mc.field_71441_e!=null && mc.field_71439_g!=null && mc.field_71462_r==null && ++readyTicks>=60)try{
            previous=mc.field_71462_r;previousParty=ClientStorageManager.party;PlayerPartyStorage party=new PlayerPartyStorage(new UUID(0,991),false);ClientStorageManager.party=party;
            data=new TeraNetwork.Reply();data.anyOrb=data.chargedOrb=true;data.shards[1]=49;data.shards[2]=64;data.shards[18]=128;
            int[] dex={143,6,9,1018,1017,1024};for(int i=0;i<6;i++){Pokemon p=PokemonFactory.create(PixelmonSpecies.fromNationalDex(dex[i]));p.setLevelNum(70);p.initialize();party.set(i,p);data.ids.add(p.getUUID());data.names.add(p.getFormattedDisplayName());data.types.add(i==5?18:2);data.locked.add(i==4?1:i==5?2:0);}
            screen=new TeraSettingsScreen(data);Field chosen=TeraSettingsScreen.class.getDeclaredField("chosen");chosen.setAccessible(true);Field candidate=TeraSettingsScreen.class.getDeclaredField("candidate");candidate.setAccessible(true);
            for(int[] size:new int[][]{{320,240},{427,267},{640,400},{1280,800}}){
                screen.func_231158_b_(mc,size[0],size[1]);click(screen,size[0],size[1],20,70);require(chosen.getInt(screen)==0,"Party scaled hitbox");
                click(screen,size[0],size[1],270,70);require(candidate.getInt(screen)==1 && !screen.canChange(),"49 shards must not enable change");
                data.shards[1]=50;require(screen.canChange(),"50 shards rejected");data.shards[1]=49;
                click(screen,size[0],size[1],20,61+4*34+10);require(chosen.getInt(screen)==4 && !screen.canChange(),"Ogerpon lock");
                click(screen,size[0],size[1],20,61+5*34+10);require(chosen.getInt(screen)==5 && !screen.canChange(),"Terapagos lock");
            }
            System.out.println("BACKPORT_TERA_SCREEN_INPUT_PASS four GUI sizes, matching transformed hitboxes, 49/50 shards, party selection and both fixed-type locks");
            mc.func_147108_a(screen);click(screen,mc.func_228018_at_().func_198107_o(),mc.func_228018_at_().func_198087_p(),20,70);
        }catch(Throwable failure){done=true;ClientStorageManager.party=previousParty;System.err.println("BACKPORT_TERA_SCREEN_FAIL");failure.printStackTrace();if(previous!=null)mc.func_147108_a(previous);}
    }
    @SubscribeEvent public static void render(TickEvent.RenderTickEvent e){
        if(done || screen==null || e.phase!=TickEvent.Phase.END || (Minecraft.func_71410_x().field_71462_r!=screen && Minecraft.func_71410_x().field_71462_r!=creative))return;
        if(++frames<12)return;frames=0;Minecraft mc=Minecraft.func_71410_x();
        try{
            Path folder=Paths.get(System.getProperty("backport.ui.output","../tera-ui"));Files.createDirectories(folder);
            try(NativeImage image=ScreenShotHelper.func_198052_a(mc.func_228018_at_().func_198105_m(),mc.func_228018_at_().func_198083_n(),mc.func_147110_a())){image.func_209270_a(folder.resolve("tera-settings-"+preview+".png"));}
            if(preview++==0){click(screen,mc.func_228018_at_().func_198107_o(),mc.func_228018_at_().func_198087_p(),20,61+4*34+10);}
            else if(preview==2){
                ClientStorageManager.party=previousParty;previousMode=mc.field_71442_b.func_178889_l();
                mc.field_71439_g.func_71165_d("/gamemode creative");mc.func_147108_a(previous);
            }
            else if(preview==3){creative.func_212873_a_().func_148329_a(1f);}
            else {done=true;mc.func_147108_a(previous);mc.field_71439_g.func_71165_d("/gamemode "+previousMode.func_77149_b());System.out.println("BACKPORT_TERA_SCREEN_VISUAL_READY "+folder.toAbsolutePath()+" creative tab and crystal-base icon rendered");}
        }catch(Throwable failure){done=true;ClientStorageManager.party=previousParty;mc.func_147108_a(previous);if(previousMode!=null)mc.field_71442_b.func_78746_a(previousMode);System.err.println("BACKPORT_TERA_SCREEN_FAIL");failure.printStackTrace();}
    }
}

