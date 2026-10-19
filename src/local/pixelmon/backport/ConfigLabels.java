package local.pixelmon.backport;
import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Shared labels also let the server search Chinese configuration names. */
public final class ConfigLabels {
    private static final JsonObject LABELS=load();
    private ConfigLabels(){}
    private static JsonObject load(){
        try(var in=ConfigLabels.class.getResourceAsStream("/backport/config-labels-zh.json")){
            if(in==null)throw new IOException("Missing configuration labels");
            return new JsonParser().parse(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
        }catch(IOException e){throw new IllegalStateException(e);}
    }
    public static String text(String path){
        if(LABELS.has(path))return LABELS.get(path).getAsString();
        String leaf=path.substring(path.lastIndexOf('.')+1);
        if(LABELS.has(leaf))return LABELS.get(leaf).getAsString();
        return path;
    }
    public static String help(String path){
        String key=path+".comment";
        if(LABELS.has(key))return LABELS.get(key).getAsString();
        key=path.substring(path.lastIndexOf('.')+1)+".comment";
        return LABELS.has(key)?LABELS.get(key).getAsString():"";
    }
    public static String value(String raw){
        switch(raw){
            case "true":return "开启";case "false":return "关闭";
            case "RANDOM":return "随机";case "AGGRESSIVE":return "积极进攻";case "TACTICAL":return "战术";
            case "WINNER":return "判为获胜";case "LOSER":return "判为战败";case "DRAW":return "平局";
            case "Once":return "仅一次";case "OncePerPlayer":return "每名玩家一次";case "Unlimited":return "不限次数";
            case "NEVER":return "从不";case "ALWAYS":return "始终";case "NORMAL":return "普通";
            default:return raw;
        }
    }
}
