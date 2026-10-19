package local.pixelmon.backport;

import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.LightningBoltEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.World;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.world.BlockEvent;

/** Track the native mining bolt itself, rather than a broad player lightning flag. */
public final class UltraPlantAchievement {
    private static final String OWNER="pixelmon_backport:ultra_plant_miner";
    private UltraPlantAchievement(){}
    public static boolean spawn(World world,Entity entity,BlockEvent.BreakEvent event){
        boolean added=world.func_217376_c(entity);
        if(added && !world.field_72995_K && !event.isCanceled()
            && entity instanceof LightningBoltEntity && event.getPlayer() instanceof ServerPlayerEntity
            && event.getPlayer().field_70170_p==world){
            entity.getPersistentData().func_186854_a(OWNER,event.getPlayer().func_110124_au());
        }
        return added;
    }
    /** Called only after the native, Forge-approved strike callback has run. */
    public static void struck(Entity target,LightningBoltEntity bolt){
        if(!(target instanceof ServerPlayerEntity) || target.field_70170_p.field_72995_K
            || target.field_70170_p!=bolt.field_70170_p)return;
        var data=bolt.getPersistentData();
        if(!data.func_186855_b(OWNER) || !target.func_110124_au().equals(data.func_186857_a(OWNER)))return;
        ServerPlayerEntity player=(ServerPlayerEntity)target;
        var advancement=player.func_184102_h().func_191949_aK().func_192778_a(new ResourceLocation(Content.NS,"ultra_space/ultra_plant_lightning"));
        if(advancement!=null){
            player.func_192039_O().func_192750_a(advancement,"earned");
            data.func_82580_o(OWNER);
        }
    }
}
