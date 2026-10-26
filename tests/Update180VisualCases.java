package qa;
import java.util.*;
import com.google.gson.*;
import com.pixelmonmod.pixelmon.entities.npcs.NPCShopkeeper;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.battles.raids.RaidData;
import com.pixelmonmod.pixelmon.items.group.PixelmonItemGroups;
import local.pixelmon.backport.*;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.block.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.item.ItemGroup;

public final class Update180VisualCases {
    public static final String[] ITEMS={"pixelmon:blue_juice","pixelmon:red_juice","pixelmon:pink_juice","pixelmon:purple_juice","pixelmon:yellow_juice","pixelmon:green_juice","pixelmon:colorful_shake","pixelmon:max_mushroom","pixelmon_backport:utility_umbrella","pixelmon_backport:eject_pack","pixelmon_backport:teal_mask","pixelmon_backport:wellspring_mask","pixelmon_backport:hearthflame_mask","pixelmon_backport:cornerstone_mask","pixelmon_backport:ability_shield","pixelmon:legend_plate","pixelmon:blank_plate","pixelmon:flame_plate","pixelmon:splash_plate","pixelmon:zap_plate","pixelmon:meadow_plate","pixelmon:icicle_plate","pixelmon:fist_plate","pixelmon:toxic_plate","pixelmon:earth_plate","pixelmon:sky_plate","pixelmon:mind_plate","pixelmon:insect_plate","pixelmon:stone_plate","pixelmon:spooky_plate","pixelmon:draco_plate","pixelmon:dread_plate","pixelmon:iron_plate","pixelmon:pixie_plate","pixelmon_backport:ash_greninjite","pixelmon:charizardite_x","pixelmon_backport:shrinking_lamp","pixelmon_backport:enlarging_lamp"};
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void run(ServerPlayerEntity player){
        var world=player.func_71121_q();
        for(int x=29;x<=47;x++)for(int z=30;z<=48;z++)world.func_180501_a(new BlockPos(x,73,z),Blocks.field_180398_cJ.func_176223_P(),2);
        for(int i=0;i<3;i++){
            var den=new TeraDenEntity(world);den.func_226286_f_(33+i*4,74,35);
            check(den.rollEncounter(1,false),"Habitat-compatible visual fixture");var encounter=den.getServerData();
            var pokemon=com.pixelmonmod.pixelmon.api.pokemon.PokemonFactory.create(encounter.getSpecies());pokemon.setForm(encounter.getForm());
            den.configure(pokemon,1,new int[]{11,2,17}[i],(int)(world.func_72820_D()/24000));world.func_217376_c(den);
            var saved=den.serializeNBT();var restored=new TeraDenEntity(world);restored.deserializeNBT(saved);check(restored.teraType()==den.teraType()&&restored.getServerData().getStars()==1,"Model migration retains den state");
        }
        var shop=new NPCShopkeeper(world);shop.init(com.pixelmonmod.pixelmon.entities.npcs.registry.ServerNPCRegistry.shopkeepers.getRandom());shop.init("Suki");shop.setNickName("Suki");shop.setBaseTrainer(com.pixelmonmod.pixelmon.entities.npcs.registry.NPCRegistryTrainers.Steve);shop.setTextureIndex(-1);shop.setCustomSteveTexture("npcchat1.png");shop.func_174805_g(true);shop.func_110163_bv();shop.func_226286_f_(37,74,39);shop.field_70177_z=180;check(world.func_217376_c(shop),"Shopkeeper enters native world");System.out.println("SHOP_SERVER id="+shop.func_145782_y()+" alive="+shop.func_70089_S()+" type="+shop.func_200600_R().getRegistryName());
        player.func_200619_a(world,37,74,46,180,6);player.field_71075_bZ.field_75100_b=true;player.field_71075_bZ.field_75101_c=true;
        var groups=Arrays.asList(ItemGroup.field_78032_a);check(!groups.contains(PixelmonItemGroups.TAB_TCG)&&!groups.contains(PixelmonItemGroups.TAB_TCG_COSMETICS)&&groups.contains(PixelmonItemGroups.TAB_POKE_BALLS),"Only TCG creative groups removed");
        var indexed=new NPCShopkeeper(world);indexed.init(com.pixelmonmod.pixelmon.entities.npcs.registry.ServerNPCRegistry.shopkeepers.getRandom());String expected=indexed.getShopkeeperName("en_us");new NpcNameSync().join(new net.minecraftforge.event.entity.EntityJoinWorldEvent(indexed,world));check(expected!=null&&!expected.isBlank()&&expected.equals(indexed.func_200200_C_().getString()),"Indexed shopkeeper real name copied into native synchronized identity");
        var maps=new LinkedHashMap<ResourceLocation,JsonElement>();maps.put(new ResourceLocation("tcg:pack"),new JsonParser().parse("{\"result\":\"tcg:pack\"}"));maps.put(new ResourceLocation("minecraft:workbench"),new JsonParser().parse("{\"result\":\"minecraft:crafting_table\"}"));TcgRemoval.recipes(maps);check(maps.size()==1&&maps.containsKey(new ResourceLocation("minecraft:workbench")),"TCG recipes only");
        maps.clear();maps.put(new ResourceLocation("minecraft:example"),new JsonParser().parse("{\"pools\":[{\"entries\":[{\"type\":\"minecraft:item\",\"name\":\"tcg:pack\"},{\"type\":\"minecraft:item\",\"name\":\"minecraft:diamond\",\"weight\":7}]}]}"));TcgRemoval.loot(maps);var entries=maps.values().iterator().next().getAsJsonObject().getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries");check(entries.size()==1&&entries.get(0).getAsJsonObject().get("weight").getAsInt()==7,"Other loot and weights retained");
        check(Content.read("backport/tera_den_mesh.json").getAsJsonArray("faces").size()==379,"Exact upstream triangle count");
        System.out.println("VISUAL_180_SERVER_PASS Tera state roundtrip, original379 triangle mesh, TCG exact group/recipe/loot scope; native world and item display prepared");
    }
}
