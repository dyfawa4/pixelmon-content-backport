package local.pixelmon.backport;
import java.util.*;
/** Stable per-building palette; never reroll an existing room on chunk reload. */
public final class AuthorRoomPalette {
    public static final String TAG="AuthorArenaWall";
    private static final Map<String,String> WALLS=new HashMap<>();
    public static final List<String> ULTRA=List.of("minecraft:iron_block","pixelmon:silver_block","minecraft:bone_block","pixelmon:platinum_block","pixelmon:aluminium_block");
    static {
        WALLS.put("plains","minecraft:stone_bricks");WALLS.put("ocean","minecraft:prismarine_bricks");
        WALLS.put("dark_forest","minecraft:brown_terracotta");WALLS.put("forest","minecraft:white_terracotta");
        WALLS.put("badlands","minecraft:yellow_glazed_terracotta");WALLS.put("desert","minecraft:orange_terracotta");
        WALLS.put("swamp","minecraft:bricks");WALLS.put("taiga","minecraft:stripped_spruce_log");
        WALLS.put("jungle","minecraft:lime_glazed_terracotta");WALLS.put("paradox","minecraft:purpur_block");
        WALLS.put("nether","minecraft:polished_basalt");
    }
    private AuthorRoomPalette(){}
    public static String choose(String theme,String biome,long salt){
        if("ultra".equals(theme))return biome.contains("desert")?"pixelmon:crystal_block":ULTRA.get(new Random(salt^0x47594d50414c4554L).nextInt(ULTRA.size()));
        return WALLS.getOrDefault(theme,"minecraft:stone_bricks");
    }
    public static boolean wall(String material){return WALLS.containsValue(material)||ULTRA.contains(material)||material.equals("pixelmon:crystal_block");}
}
