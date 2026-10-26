package local.pixelmon.backport;
import java.util.HashMap;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.gen.settings.DimensionStructuresSettings;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
/** Ultra Space's native dimension settings omit gyms; retain native gym spacing there. */
@Mod.EventBusSubscriber(modid=Content.NS)
public final class RockGymGeneration {
    public static boolean apply(ServerWorld world){
        if(!world.func_234923_W_().func_240901_a_().toString().equals("pixelmon:ultra_space"))return false;
        var settings=world.func_72863_F().func_201711_g().func_235957_b_();var gym=GymLocator.nativeGym();
        if(settings.func_236197_a_(gym)!=null)return false;
        var overworld=world.func_73046_m().func_241755_D_();var spacing=overworld==null?null:GymLocator.settings(overworld);
        if(spacing==null)spacing=DimensionStructuresSettings.field_236191_b_.get(gym);
        if(spacing==null){org.apache.logging.log4j.LogManager.getLogger("pixelmon_backport").warn("Native gym spacing is disabled; Ultra Desert rock gym remains disabled");return false;}
        var changed=new HashMap<>(settings.func_236195_a_());changed.put(gym,spacing);
        ((local.pixelmon.backport.mixin.StructureSpacingAccess)(Object)settings).backport$structureSettings(changed);return true;
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void load(WorldEvent.Load event){if(event.getWorld() instanceof ServerWorld)apply((ServerWorld)event.getWorld());}
}
