package local.pixelmon.backport;

import com.google.gson.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.IOException;

/** Small writable settings file; content registries and save data are not configuration. */
public final class BackportConfig {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private static Path file(){return net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().resolve("pixelmon-backport.json");}
    public static final class Values {
        @com.pixelmonmod.pixelmon.api.config.api.ui.ConfigSetting(minValue=1,maxValue=10) public double ashWaterMultiplier=1.5;
        @com.pixelmonmod.pixelmon.api.config.api.ui.ConfigSetting(minValue=1,maxValue=20) public int ashWaterTurns=5;
        public boolean erosionEnabled=true;
        @com.pixelmonmod.pixelmon.api.config.api.ui.ConfigSetting(minValue=1,maxValue=100) public int erosionReduction=20;
        @com.pixelmonmod.pixelmon.api.config.api.ui.ConfigSetting(minValue=1,maxValue=20) public int erosionTurns=5;
    }
    private static Values values=new Values();
    private BackportConfig(){}
    public static synchronized Values values(){return values;}
    public static synchronized void load(){
        Path FILE=file();
        if(!Files.exists(FILE))return;
        try(var reader=Files.newBufferedReader(FILE,StandardCharsets.UTF_8)){
            Values next=JSON.fromJson(reader,Values.class);if(next==null)throw new IllegalArgumentException("Empty configuration");
            ConfigEditor.validateObject(next);values=next;
        }catch(Exception e){org.apache.logging.log4j.LogManager.getLogger("pixelmon_backport").error("Cannot load {}; using last valid settings",FILE,e);}
    }
    public static synchronized void save() throws IOException {
        Path FILE=file();
        ConfigEditor.validateObject(values);Files.createDirectories(FILE.getParent());
        Path temp=FILE.resolveSibling(FILE.getFileName()+".tmp");Files.writeString(temp,JSON.toJson(values),StandardCharsets.UTF_8);
        try{Files.move(temp,FILE,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
        catch(AtomicMoveNotSupportedException e){Files.move(temp,FILE,StandardCopyOption.REPLACE_EXISTING);}
    }
    public static double ashWaterMultiplier(){return values.ashWaterMultiplier;}
    public static int ashWaterTurns(){return values.ashWaterTurns;}
    public static boolean erosionEnabled(){return values.erosionEnabled;}
    public static int erosionReduction(){return values.erosionReduction;}
    public static int erosionTurns(){return values.erosionTurns;}
}
