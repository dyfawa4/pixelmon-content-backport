package local.pixelmon.backport;

import java.util.*;
import java.util.regex.*;

/** Classify the actual main gym, not houses, biome guesses or the selected pool. */
public final class GymThemePaths {
    public static final String[] TYPES={"normal","fire","water","electric","grass","ice","fighting","poison","ground","flying","psychic","bug","rock","ghost","dragon","dark","steel","fairy"};
    public static final String[] CHINESE={"一般","火","水","电","草","冰","格斗","毒","地面","飞行","超能力","虫","岩石","幽灵","龙","恶","钢","妖精"};
    private static final Pattern MAIN=Pattern.compile("(?:pixelmon|pixelmon_backport):gyms/([a-z]+)/gym_([a-z]+)(?=\\]|\\s|$)");
    private GymThemePaths(){}
    public static Set<String> poolTypes(String id){
        Set<String> types=new HashSet<>();
        for(String type:TYPES)for(String ns:new String[]{"pixelmon","pixelmon_backport"})
            if(id.equals(ns+":gyms/"+type+"/start_pool"))types.add(type);
        if(id.equals("pixelmon_backport:gyms/natural/dark/start_pool")){types.add("dark");types.add("grass");}
        return types;
    }
    public static String normalize(String value){
        for(int i=0;i<TYPES.length;i++)if(TYPES[i].equalsIgnoreCase(value) || CHINESE[i].equals(value) || (CHINESE[i]+"系").equals(value))return TYPES[i];
        return null;
    }
    public static String chinese(String type){for(int i=0;i<TYPES.length;i++)if(TYPES[i].equals(type))return CHINESE[i];return type;}
    public static String classify(Iterable<String> pieces){
        Set<String> mains=new HashSet<>();
        for(String piece:pieces){Matcher m=MAIN.matcher(piece);while(m.find())if(m.group(1).equals(m.group(2)) && normalize(m.group(1))!=null)mains.add(m.group(1));}
        return mains.size()==1?mains.iterator().next():null;
    }
}
