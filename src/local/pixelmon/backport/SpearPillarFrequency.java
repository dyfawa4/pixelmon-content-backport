package local.pixelmon.backport;
import java.util.*;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.settings.*;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import local.pixelmon.backport.mixin.StructureSpacingAccess;
/** Native 50-chunk cells ->15, followed by a90% gate: expected10x candidates, before spacing. */
@Mod.EventBusSubscriber(modid=Content.NS)
public final class SpearPillarFrequency {
    public static final ResourceLocation ID=new ResourceLocation("pixelmon:spear_pillar");
    public static boolean apply(DimensionStructuresSettings settings){
        Structure<?> pillar=ForgeRegistries.STRUCTURE_FEATURES.getValue(ID);if(pillar==null)return false;
        var old=settings.func_236197_a_(pillar);if(old==null||old.func_236668_a_()==15&&old.func_236671_b_()==12)return false;
        var changed=new HashMap<>(settings.func_236195_a_());changed.put(pillar,new StructureSeparationSettings(15,12,old.func_236673_c_()));
        ((StructureSpacingAccess)(Object)settings).backport$structureSettings(changed);return true;
    }
    @SubscribeEvent public static void load(WorldEvent.Load event){if(event.getWorld() instanceof ServerWorld)apply(((ServerWorld)event.getWorld()).func_72863_F().func_201711_g().func_235957_b_());}
}
