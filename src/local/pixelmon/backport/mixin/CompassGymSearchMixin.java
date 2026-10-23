package local.pixelmon.backport.mixin;

import local.pixelmon.backport.GymLocator;
import net.minecraft.util.math.*;
import net.minecraft.world.IStructureReader;
import net.minecraft.world.gen.feature.structure.*;
import net.minecraft.world.gen.settings.StructureSeparationSettings;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets="com.chaosthedude.explorerscompass.util.StructureSearchWorker",remap=false)
public abstract class CompassGymSearchMixin {
    @Shadow public ServerWorld world;
    @Shadow public Structure<?> structure;
    @Shadow public StructureSeparationSettings separationSettings;
    @Shadow public boolean finished;
    @Shadow public PlayerEntity player;
    @Shadow public ItemStack stack;
    @Shadow public ResourceLocation structureKey;
    @Shadow public BlockPos startPos;
    @Shadow public int samples,x,z;
    @Shadow private void finish(boolean found){throw new AssertionError();}
    @Unique private GymLocator.Search backport$gymSearch;
    @Unique private boolean backport$handlesGym(){return structure instanceof GymLocator.TypedGym||structure==GymLocator.nativeGym();}
    @Unique private void backport$closeGym(){if(backport$gymSearch!=null){backport$gymSearch.close();backport$gymSearch=null;}}
    @Unique private boolean backport$stale(){
        return backport$handlesGym() && (player.field_70128_L||player.field_70170_p!=world || stack.func_77978_p()==null
            || !structureKey.toString().equals(stack.func_77978_p().func_74779_i("StructureKey")));
    }
    @Inject(method="hasWork",at=@At("HEAD"),cancellable=true)
    private void backport$stopSupersededSearch(CallbackInfoReturnable<Boolean> ci){
        if(backport$stale()){backport$closeGym();finished=true;ci.setReturnValue(false);}
        else if(backport$handlesGym())ci.setReturnValue(!finished);
    }
    @Inject(method="finish",at=@At("HEAD"),cancellable=true)
    private void backport$keepCurrentTarget(boolean found,CallbackInfo ci){
        backport$closeGym();if(backport$stale()){finished=true;ci.cancel();}
    }
    @Inject(method="<init>",at=@At("RETURN"))
    private void backport$gymSettings(CallbackInfo ci){
        if(backport$handlesGym()){separationSettings=GymLocator.settings(world);finished=!GymLocator.available(world,structure instanceof GymLocator.TypedGym?((GymLocator.TypedGym)structure).type:"any");}
    }
    @Unique private int backport$limit(String name)throws ReflectiveOperationException{
        Object config=Class.forName("com.chaosthedude.explorerscompass.config.ConfigHandler").getField("GENERAL").get(null);
        return ((net.minecraftforge.common.ForgeConfigSpec.IntValue)config.getClass().getField(name).get(config)).get();
    }
    @Unique private boolean backport$visited(BlockPos found)throws ReflectiveOperationException{
        BlockPos p=player.func_233580_cy_();if(Math.abs((long)p.func_177958_n()-found.func_177958_n())<=32&&Math.abs((long)p.func_177952_p()-found.func_177952_p())<=32)return true;
        var key=com.mojang.datafixers.util.Pair.of(PlayerEntity.func_146094_a(player.func_146103_bH()),com.mojang.datafixers.util.Pair.of(found.func_177958_n(),found.func_177952_p()));
        return ((java.util.List<?>)Class.forName("com.chaosthedude.explorerscompass.util.StructureUtils").getField("haveFound").get(null)).contains(key);
    }
    @Inject(method="doWork",at=@At("HEAD"),cancellable=true)
    private void backport$asyncGym(CallbackInfoReturnable<Boolean> ci){
        if(!backport$handlesGym())return;
        if(finished||backport$stale()){backport$closeGym();finished=true;ci.setReturnValue(false);return;}
        try{
            if(backport$gymSearch==null)backport$gymSearch=new GymLocator.Search(world,startPos,structure instanceof GymLocator.TypedGym?((GymLocator.TypedGym)structure).type:"any",Math.min(32768,backport$limit("maxRadius")));
            BlockPos found=backport$gymSearch.step();samples=backport$gymSearch.examined();
            if(found!=null&&!backport$visited(found)){x=found.func_177958_n();z=found.func_177952_p();finish(true);}
            else if(backport$gymSearch.finished()||samples>=backport$limit("maxSamples"))finish(false);
        }catch(ReflectiveOperationException|RuntimeException failure){org.apache.logging.log4j.LogManager.getLogger("pixelmon_backport").warn("Gym compass search failed",failure);finish(false);}
        ci.setReturnValue(false); // Yield until the next tick instead of managed-blocking.
    }
}
