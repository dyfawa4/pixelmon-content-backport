import java.util.*;
import local.pixelmon.backport.GymCities;
import net.minecraft.entity.item.ItemFrameEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

/** Separate, persistent eighteen-badge display and collection room. */
public final class BadgeGallery {
    static boolean isMap(ServerPlayerEntity p){return p.func_71121_q().func_73046_m().func_240793_aU_().func_76065_j().startsWith("回迁徽章 ");}
    static List<ItemStack> badges(){
        List<ItemStack> result=new ArrayList<>();for(String type:GymTestMaps.TYPES){
            boolean added=GymCities.DATA.getAsJsonObject("newThemes").has(type);
            String id=added?"pixelmon_backport:"+type+"_gym_badge":"pixelmon:"+GymCities.DATA.getAsJsonObject("nativeBadges").get(type).getAsString();
            result.add(DedicatedTestMaps.stack(id,1));
        }return result;
    }
    static void tick(ServerPlayerEntity p){
        var world=p.func_71121_q();var data=GymTestMaps.data(world);
        if(!data.ready){
            for(int x=-2;x<=1;x++)for(int z=-1;z<=0;z++)world.func_175726_f(new BlockPos(x*16,64,z*16));
            TestMaps.cmd(p,"fill -20 64 -15 20 73 15 minecraft:air");TestMaps.cmd(p,"fill -20 64 -15 20 64 15 minecraft:smooth_quartz");
            TestMaps.cmd(p,"fill -20 65 -12 20 68 -12 minecraft:white_concrete");TestMaps.cmd(p,"fill -20 65 12 20 68 12 minecraft:white_concrete");
            List<ItemStack> all=badges();Set<String> unique=new HashSet<>();
            for(int i=0;i<18;i++){
                ItemStack badge=all.get(i);if(!unique.add(badge.func_77973_b().getRegistryName().toString()))throw new IllegalStateException("Duplicate badge display");
                int x=-16+(i%9)*4,z=i<9?-11:11;Direction facing=i<9?Direction.SOUTH:Direction.NORTH;
                ItemFrameEntity frame=new ItemFrameEntity(world,new BlockPos(x,66,z),facing);frame.func_82334_a(badge.func_77946_l());world.func_217376_c(frame);
                TestMaps.label(world,x+.5,69,z+.5,GymTestMaps.LABELS[i]+"属性 · "+badge.func_200301_q().getString());
                TestMaps.supply(world,x,z+(i<9?2:-2),"击败对应属性馆主获得",Collections.singletonList(badge.func_77946_l()));
                TestMaps.block(world,x,64,z,"minecraft:sea_lantern");
            }
            data.cursor=18;data.ready=true;data.func_76185_a();System.out.println("BACKPORT_BADGE_GALLERY_READY unique badges=18");
        }
        if(!p.getPersistentData().func_74767_n("BackportBadgeWelcome")){
            p.getPersistentData().func_74757_a("BackportBadgeWelcome",true);TestMaps.cmd(p,"gamemode creative");TestMaps.cmd(p,"tp @s 0 65 0 0 0");
            p.func_146105_b(new net.minecraft.util.text.StringTextComponent("十八种属性徽章已展示。木桶中可取样查看；Shift / JEI 查询新增徽章用途与获取方式。"),false);
        }
    }
}
