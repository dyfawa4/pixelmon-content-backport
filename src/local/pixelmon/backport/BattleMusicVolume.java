package local.pixelmon.backport;

import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import net.minecraft.client.gui.widget.AbstractSlider;
import net.minecraft.util.text.*;

/** Local audio preference, deliberately independent of the server gameplay config. */
public final class BattleMusicVolume {
    public interface Refresh { void backport$refreshBattleVolume(); }
    private static volatile Refresh engine;
    private static volatile float volume=1;
    private static boolean loaded;
    private static Path file(){return net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().resolve("pixelmon-backport-audio.json");}
    private BattleMusicVolume(){}
    public static void engine(Refresh value){engine=value;}
    public static synchronized float get(){
        if(!loaded){
            loaded=true;
            if(Files.exists(file()))try(var reader=Files.newBufferedReader(file(),StandardCharsets.UTF_8)){
                float saved=new JsonParser().parse(reader).getAsJsonObject().get("battleMusicVolume").getAsFloat();
                if(Float.isFinite(saved))volume=Math.max(0,Math.min(1,saved));
            }catch(Exception e){org.apache.logging.log4j.LogManager.getLogger("pixelmon_backport").warn("Cannot load local battle music volume",e);}
        }
        return volume;
    }
    public static synchronized void set(float value){
        get();volume=Float.isFinite(value)?Math.max(0,Math.min(1,value)):1;
        if(engine!=null)engine.backport$refreshBattleVolume();
        try{
            Path dest=file();Files.createDirectories(dest.getParent());
            Path temp=dest.resolveSibling(dest.getFileName()+".tmp");
            var json=new JsonObject();json.addProperty("battleMusicVolume",volume);
            Files.writeString(temp,json.toString()+"\n",StandardCharsets.UTF_8);
            try{Files.move(temp,dest,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException e){Files.move(temp,dest,StandardCopyOption.REPLACE_EXISTING);}
        }catch(Exception e){org.apache.logging.log4j.LogManager.getLogger("pixelmon_backport").warn("Cannot save local battle music volume",e);}
    }
    public static final class Slider extends AbstractSlider {
        public Slider(int x,int y){super(x,y,150,20,StringTextComponent.field_240750_d_,get());func_230979_b_();}
        @Override protected void func_230979_b_(){func_238482_a_(new TranslationTextComponent("pixelmon_backport.options.battle_music",Math.round(field_230683_b_*100)));}
        @Override protected void func_230972_a_(){set((float)field_230683_b_);}
    }
}
