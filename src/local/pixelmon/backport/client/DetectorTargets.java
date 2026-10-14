package local.pixelmon.backport.client;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

/** A teleport can leave retired entries in the world's global lists briefly. */
public final class DetectorTargets {
    private DetectorTargets(){}
    public static List<TileEntity> tiles(List<TileEntity> input){
        ClientWorld world=Minecraft.func_71410_x().field_71441_e;
        List<TileEntity> active=new ArrayList<>();
        if(world==null)return active;
        // Looking up a tile can realize a pending tile and change the world's
        // live list, even on the render thread. Snapshot before those lookups.
        for(TileEntity tile:input.toArray(new TileEntity[0])){
            if(tile==null || tile.func_145837_r() || tile.func_145831_w()!=world)continue;
            BlockPos pos=tile.func_174877_v();
            if(world.func_217354_b(pos.func_177958_n()>>4,pos.func_177952_p()>>4)
                && world.func_175625_s(pos)==tile)active.add(tile);
        }
        return active;
    }
    public static Iterable<Entity> entities(Iterable<Entity> input){
        ClientWorld world=Minecraft.func_71410_x().field_71441_e;
        List<Entity> active=new ArrayList<>();
        if(world==null)return active;
        List<Entity> snapshot=new ArrayList<>();
        input.forEach(snapshot::add);
        for(Entity entity:snapshot){
            if(entity==null || entity.field_70128_L || entity.field_70170_p!=world)continue;
            BlockPos pos=entity.func_233580_cy_();
            if(world.func_217354_b(pos.func_177958_n()>>4,pos.func_177952_p()>>4)
                && world.func_73045_a(entity.func_145782_y())==entity)active.add(entity);
        }
        return active;
    }
}
