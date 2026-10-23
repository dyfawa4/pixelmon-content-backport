package local.pixelmon.backport;
import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Curated mature aces: competitive Urshifu/Terrakion are deliberate BST exceptions. */
public final class GymAcePolicy {
    public static final String MARKER="BackportGymLegendaryAce";
    public static final JsonObject DATA;
    static {
        try(var in=GymAcePolicy.class.getClassLoader().getResourceAsStream("backport/gym_aces.json")){
            DATA=new JsonParser().parse(new InputStreamReader(Objects.requireNonNull(in),StandardCharsets.UTF_8)).getAsJsonObject();
        }catch(IOException e){throw new IllegalStateException("Gym ace catalogue",e);}
    }
    private GymAcePolicy(){}
    public static boolean specialPoisonAce(UUID trainer){return new Random(trainer.getLeastSignificantBits()^trainer.getMostSignificantBits()^0x41c718f2L).nextInt(100)<90;}
    public static boolean strong(int dex,int total,boolean legendary,boolean usable){
        if(!legendary || !usable || Arrays.asList(789,790,772,891,803,808).contains(dex))return false;
        return total>=600 || dex==892 && total>=550 || dex==639 && total>=580;
    }
    public static boolean eligible(int dex,int total,boolean legendary,boolean usable,boolean matchesTheme){return matchesTheme && strong(dex,total,legendary,usable);}
}
