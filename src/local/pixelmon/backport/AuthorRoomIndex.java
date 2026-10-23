package local.pixelmon.backport;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.registries.ForgeRegistries;

/** Per-dimension building locations survive the defeat/removal of their occupants. */
public final class AuthorRoomIndex extends WorldSavedData {
    private final Map<Long,Boolean> rooms=new LinkedHashMap<>();
    public AuthorRoomIndex(){super("backport_author_rooms");}
    public static AuthorRoomIndex get(ServerWorld world){return world.func_217481_x().func_215752_a(AuthorRoomIndex::new,"backport_author_rooms");}
    public void record(BlockPos home,boolean physical){if(!Boolean.valueOf(physical).equals(rooms.put(home.func_218275_a(),physical)))func_76185_a();}
    public List<BlockPos> positions(boolean physical){
        List<BlockPos> result=new ArrayList<>();rooms.forEach((p,type)->{if(type==physical)result.add(BlockPos.func_218283_e(p));});return result;
    }
    @Override public void func_76184_a(CompoundNBT tag){
        rooms.clear();ListNBT list=tag.func_150295_c("Rooms",10);
        for(int i=0;i<list.size();i++){CompoundNBT row=list.func_150305_b(i);if(row.func_74764_b("Home"))rooms.put(row.func_74763_f("Home"),row.func_74767_n("Physical"));}
    }
    @Override public CompoundNBT func_189551_b(CompoundNBT tag){
        ListNBT list=new ListNBT();rooms.forEach((p,type)->{CompoundNBT row=new CompoundNBT();row.func_74772_a("Home",p);row.func_74757_a("Physical",type);list.add(row);});tag.func_218657_a("Rooms",list);return tag;
    }
    public static boolean intact(ServerWorld world,BlockPos home,boolean physical){
        if(physical)return false;
        if(!world.func_175667_e(home.func_177982_a(-6,0,-6))||!world.func_175667_e(home.func_177982_a(6,7,6)))return false;
        String floor=id(world,home);
        // Accept the former emerald floor in previously saved test/natural rooms too.
        if(physical?!floor.equals("minecraft:green_wool")&&!floor.equals("minecraft:emerald_block"):!floor.equals("minecraft:white_wool")&&!floor.equals("minecraft:chiseled_quartz_block"))return false;
        for(int x:new int[]{-6,6})for(int z:new int[]{-6,6})for(int y:new int[]{2,7}){
            String material=id(world,home.func_177982_a(x,y,z));
            if(!AuthorRoomPalette.wall(material))return false;
        }
        return true;
    }
    private static String id(ServerWorld world,BlockPos pos){ResourceLocation id=ForgeRegistries.BLOCKS.getKey(world.func_180495_p(pos).func_177230_c());return id==null?"":id.toString();}
}
