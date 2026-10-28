package local.pixelmon.backport;
import java.util.*;

/** Pure encounter rules shared by natural generation, test displays and roster selection. */
public final class AuthorBossPolicy {
    public static final String NAME="神秘任务男";
    public static final int HEALTH=300,DAMAGE_LIMIT=20,CITY_SPACING=64,CITY_SEPARATION=24;
    public static final Map<String,List<String>> TYPES=new LinkedHashMap<>();
    static {
        TYPES.put("plains",List.of("NORMAL"));TYPES.put("ocean",List.of("WATER"));
        TYPES.put("dark_forest",List.of("DARK","PSYCHIC"));TYPES.put("forest",List.of("GRASS","BUG"));
        TYPES.put("badlands",List.of("GROUND","ROCK","ELECTRIC"));TYPES.put("mountain",List.of("FIRE","GROUND"));
        TYPES.put("desert",List.of("STEEL","FIRE"));TYPES.put("swamp",List.of("POISON","PSYCHIC"));
        TYPES.put("taiga",List.of("DRAGON"));TYPES.put("jungle",List.of("WATER","BUG","GRASS"));
        TYPES.put("snow",List.of("ICE"));TYPES.put("nether",List.of("FIRE","DARK","GHOST"));
    }
    public static String theme(String dimension,String category,String biome){
        String dim=dimension.toLowerCase(Locale.ROOT),name=biome.toLowerCase(Locale.ROOT),cat=category.toLowerCase(Locale.ROOT);
        if(dim.contains("drowned"))return null;
        if(dim.equals("minecraft:the_end"))return "paradox";
        if(dim.contains("ultra"))return "ultra";
        if(dim.equals("minecraft:the_nether"))return "nether";
        if(cat.equals("mushroom")||name.contains("mushroom_fields")||name.contains("mushroom_island"))return null;
        if(cat.equals("ocean")||cat.equals("beach")||name.contains("beach"))return "ocean";
        if(name.contains("dark_forest")||name.contains("roofed_forest"))return "dark_forest";
        if(cat.equals("mesa")||name.contains("badlands"))return "badlands";
        if(cat.equals("taiga"))return "taiga";
        if(cat.equals("icy")||name.contains("snowy_tundra")||name.contains("snowy_plains"))return "snow";
        if(cat.equals("jungle"))return "jungle";
        if(cat.equals("swamp"))return "swamp";
        if(cat.equals("desert")||cat.equals("savanna"))return "desert";
        if(cat.equals("extreme_hills")||name.contains("mountain"))return "mountain";
        if(cat.equals("forest"))return "forest";
        return "plains";
    }
    public static boolean physical(String dimension,String category,String biome){
        return false;
    }
    public static int money(Random random){return 500000+random.nextInt(500001);}
    /** Biome-independent superset for cheap compass scans; final selection uses the theme overload. */
    public static boolean candidate(long seed,int x,int z,boolean physical){
        return !physical&&selected(seed,x,z,false);
    }
    /** All themes retain both existing candidate points; 3000-block spacing controls density. */
    public static boolean candidate(long seed,int x,int z,boolean physical,String theme){
        return !physical&&theme!=null&&selected(seed,x,z,false);
    }
    private static boolean selected(long seed,int x,int z,boolean ocean){
        int rx=Math.floorDiv(x,CITY_SPACING),rz=Math.floorDiv(z,CITY_SPACING);
        Random random=new Random(seed+(long)rx*341873128712L+(long)rz*132897987541L+198473905);
        int range=CITY_SPACING-CITY_SEPARATION;
        int oldX=random.nextInt(range),oldZ=random.nextInt(range);
        boolean legacy=x==rx*CITY_SPACING+oldX&&z==rz*CITY_SPACING+oldZ;
        // Skip the legacy index so even a hash collision cannot lose the second land opportunity.
        int extra=random.nextInt(range*range-1),old=oldX*range+oldZ;
        if(extra>=old)extra++;
        return legacy||x==rx*CITY_SPACING+extra/range&&z==rz*CITY_SPACING+extra%range;
    }
    public static float combatHealth(float health,float maximum,int players){
        float target=HEALTH*Math.max(1,players);
        return Math.min(target,Math.max(1,maximum>0?health/maximum*target:target));
    }
    public static float damage(float amount){return Float.isFinite(amount)?Math.min(DAMAGE_LIMIT,Math.max(0,amount)):DAMAGE_LIMIT;}
    public static List<Integer> draw(int pool,int count,Random random){
        if(pool<count)throw new IllegalArgumentException("Boss prize pool has fewer than three entries");
        List<Integer> indices=new ArrayList<>();for(int i=0;i<pool;i++)indices.add(i);
        Collections.shuffle(indices,random);return new ArrayList<>(indices.subList(0,count));
    }
    private AuthorBossPolicy(){}
}
