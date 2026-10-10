import com.google.gson.*;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import com.pixelmonmod.pixelmon.client.models.smd.AnimationType;
import com.pixelmonmod.pixelmon.client.models.PixelmonModelSmd;
import local.pixelmon.backport.Content;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.*;

/** Isolated visual QA harness. Not part of the addon. Native renderers only. */
@Mod("backport_client_test")
public class ClientGallery {
    @Mod.EventBusSubscriber(modid="backport_client_test",value=Dist.CLIENT)
    public static class Events {
        static boolean opened;
        @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
            Minecraft mc=Minecraft.func_71410_x();
            if(event.phase==TickEvent.Phase.END && mc.field_71441_e!=null && mc.field_71439_g!=null && !opened) {
                opened=true;
                try{ClientCompatibilityVerification.run();}catch(Throwable failure){System.err.println("BACKPORT_PM_BATTLE_INFO_FAIL");failure.printStackTrace();}
                if(!"false".equals(System.getProperty("backport.gallery.enabled")))mc.func_147108_a(new Gallery());
            }
        }
    }
    static class Gallery extends Screen {
        final List<PixelmonEntity> entities=new ArrayList<>();
        final Set<String> rendered=new HashSet<>();
        int page,mode;boolean shiny;boolean auto=true;int ticks,readyTicks,waitingTicks;boolean pageReady;
        Gallery(){super(new StringTextComponent("Backport visual QA"));
            for(JsonElement patch:Content.DATA.getAsJsonArray("patches")) {
                JsonObject json=patch.getAsJsonObject().getAsJsonObject("species");
                for(JsonElement form:json.getAsJsonArray("forms")) {
                    Pokemon pokemon=PokemonFactory.create(PixelmonSpecies.fromNationalDex(json.get("dex").getAsInt()));
                    pokemon.setForm(form.getAsJsonObject().get("name").getAsString());
                    entities.add(new PixelmonEntity(Minecraft.func_71410_x().field_71441_e,pokemon));
                }
            }
        }
        @Override public boolean func_231177_au__(){return false;}
        @Override public void func_231023_e_(){
            ++ticks;if(!auto)return;
            if(!pageReady){readyTicks=0;if(++waitingTicks==600){auto=false;System.err.println("BACKPORT_CLIENT_GALLERY_FAIL model load timed out on page "+(page+1));}return;}
            waitingTicks=0;
            if(++readyTicks>=40){readyTicks=0;pageReady=false;page++;if(page>=(entities.size()+5)/6){page=0;mode++;if(mode>=3){mode=0;if(shiny){auto=false;int expected=entities.size()*6;if(rendered.size()==expected)System.out.println("BACKPORT_CLIENT_GALLERY_PASS loaded render cases="+rendered.size());else System.err.println("BACKPORT_CLIENT_GALLERY_FAIL loaded="+rendered.size()+" expected="+expected);}else shiny=true;}}}
        }
        @Override public boolean func_231046_a_(int key,int scan,int mods){
            if(key==82){auto=!auto;readyTicks=waitingTicks=0;return true;}
            if(key==262){page=(page+1)%((entities.size()+5)/6);return true;}
            if(key==263){page=(page+(entities.size()+5)/6-1)%((entities.size()+5)/6);return true;}
            if(key==83){shiny=!shiny;return true;}
            if(key==65){mode=(mode+1)%3;return true;}
            return super.func_231046_a_(key,scan,mods);
        }
        @Override public void func_230430_a_(MatrixStack matrix,int mx,int my,float partial){
            func_238467_a_(matrix,0,0,field_230708_k_,field_230709_l_,0xff151e29);
            AnimationType animation=new AnimationType[]{AnimationType.IDLE,AnimationType.WALK,AnimationType.FLY}[mode];
            func_238476_c_(matrix,field_230706_i_.field_71466_p,"Java 11 | 模型检查 / "+new String[]{"待机","行走","飞行"}[mode]+" / "+(shiny?"异色":"普通")+" | 第 "+(page+1)+" 页",10,9,0xffffffff);
            func_238476_c_(matrix,field_230706_i_.field_71466_p,"方向键翻页    A：动作    S：异色    R：自动（等待加载）    Esc：返回游戏",10,22,0xffb5c8db);
            int cw=(field_230708_k_-24)/3,ch=(field_230709_l_-54)/2;
            pageReady=true;
            for(int i=0;i<6 && page*6+i<entities.size();i++) {
                PixelmonEntity entity=entities.get(page*6+i);Pokemon pokemon=entity.getPokemon();
                pokemon.setPalette(shiny?"shiny":"none");entity.setAnimation(animation);entity.field_70173_aa=ticks;
                int x=8+(i%3)*(cw+4),y=40+(i/3)*(ch+4);
                func_238467_a_(matrix,x,y,x+cw,y+ch,0xff263445);
                String label=pokemon.getSpecies().getDex()+" "+pokemon.getSpecies().getName()+" / "+pokemon.getForm().getName();
                func_238476_c_(matrix,field_230706_i_.field_71466_p,pokemon.getSpecies().getDex()+" "+pokemon.getLocalizedName()+" / "+pokemon.getForm().getName(),x+5,y+5,0xffffffff);
                int scale=(int)Math.min(38,(ch-27)/Math.max(1.0,Math.max(entity.func_213302_cg(),entity.func_213311_cf())));
                InventoryScreen.func_228187_a_(x+cw/2,y+ch-8,scale,-30,0,entity);
                boolean loaded=entity.getModel() instanceof PixelmonModelSmd && ((PixelmonModelSmd<?>)entity.getModel()).theModel!=null && ((PixelmonModelSmd<?>)entity.getModel()).theModel.body!=null && !((PixelmonModelSmd<?>)entity.getModel()).theModel.body.faces.isEmpty();
                if(!loaded){pageReady=false;func_238476_c_(matrix,field_230706_i_.field_71466_p,"模型加载中…",x+5,y+19,0xffffc064);}
                String caseKey=label+" / "+animation+" / "+(shiny?"shiny":"none");
                if(loaded && rendered.add(caseKey))System.out.println("BACKPORT_CLIENT_RENDER_LOADED "+caseKey+" game height="+entity.func_213302_cg()+" width="+entity.func_213311_cf());
            }
        }
    }
}
