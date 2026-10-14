package local.pixelmon.backport.mixin;

import local.pixelmon.backport.TeraDenLocator;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SharedSeedRandom;
import net.minecraft.util.math.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.IStructureReader;
import net.minecraft.world.gen.feature.structure.*;
import net.minecraft.world.gen.settings.StructureSeparationSettings;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Pseudo @Mixin(targets="com.chaosthedude.explorerscompass.util.StructureSearchWorker",remap=false)
public abstract class CompassTeraSearchMixin {
    @Shadow public ServerWorld world;
    @Shadow public Structure<?> structure;
    @Shadow public StructureSeparationSettings separationSettings;
    @Shadow public PlayerEntity player;
    @Shadow public ItemStack stack;
    @Shadow public ResourceLocation structureKey;
    @Shadow public boolean finished;
    @Shadow public int x;
    @Shadow public int z;
    @Shadow private void finish(boolean found){throw new AssertionError();}
    @Unique private TeraDenLocator.Candidate backport$pendingDen;
    @Unique private final java.util.Set<Long> backport$checkedDens=new java.util.HashSet<>();
    @Unique private void backport$releaseDen(){if(backport$pendingDen!=null){backport$pendingDen.close(world);backport$pendingDen=null;}}
    @Unique private boolean backport$staleDen(){return structure instanceof TeraDenLocator.TypedDen && (player.field_70128_L || player.field_70170_p!=world || stack.func_77978_p()==null || !structureKey.toString().equals(stack.func_77978_p().func_74779_i("StructureKey")));}
    @Inject(method="<init>",at=@At("RETURN"))
    private void backport$denSettings(CallbackInfo ci){if(structure instanceof TeraDenLocator.TypedDen){var alias=(TeraDenLocator.TypedDen)structure;separationSettings=TeraDenLocator.settings(world,alias);finished=!TeraDenLocator.available(world,alias);}}
    @Inject(method="hasWork",at=@At("HEAD"),cancellable=true)
    private void backport$cancelDen(CallbackInfoReturnable<Boolean> ci){if(backport$staleDen()){backport$releaseDen();finished=true;ci.setReturnValue(false);}else if(!finished && backport$pendingDen!=null)ci.setReturnValue(true);}
    @Inject(method="finish",at=@At("HEAD"),cancellable=true)
    private void backport$keepDenTarget(boolean found,CallbackInfo ci){backport$releaseDen();if(backport$staleDen()){finished=true;ci.cancel();}}
    @Inject(method="doWork",at=@At("HEAD"),cancellable=true)
    private void backport$waitForDen(CallbackInfoReturnable<Boolean> ci){
        if(!(structure instanceof TeraDenLocator.TypedDen))return;
        if(finished || backport$staleDen()){backport$releaseDen();finished=true;ci.setReturnValue(false);return;}
        if(backport$pendingDen==null)return;
        try{
            if(backport$pendingDen.ready(world)){
                BlockPos found=backport$pendingDen.find(world);backport$releaseDen();
                if(found!=null){x=found.func_177958_n();z=found.func_177952_p();finish(true);ci.setReturnValue(false);return;}
            }
        }catch(RuntimeException failure){org.apache.logging.log4j.LogManager.getLogger("pixelmon_backport").warn("Tera den compass candidate failed: {}",failure.toString());backport$releaseDen();finish(false);ci.setReturnValue(false);return;}
        // false yields this tick; hasWork keeps the worker registered while waiting.
        if(backport$pendingDen!=null)ci.setReturnValue(false);
    }
    @Redirect(method="doWork",at=@At(value="INVOKE",target="Lnet/minecraft/world/gen/feature/structure/Structure;func_236392_a_(Lnet/minecraft/world/gen/settings/StructureSeparationSettings;JLnet/minecraft/util/SharedSeedRandom;II)Lnet/minecraft/util/math/ChunkPos;"))
    private ChunkPos backport$nativeDenCandidate(Structure<?> requested,StructureSeparationSettings settings,long seed,SharedSeedRandom random,int x,int z){
        return (requested instanceof TeraDenLocator.TypedDen?((TeraDenLocator.TypedDen)requested).nativeDen():requested).func_236392_a_(settings,seed,random,x,z);
    }
    @Redirect(method="doWork",at=@At(value="INVOKE",target="Lnet/minecraft/world/gen/feature/structure/StructureManager;func_235013_a_(Lnet/minecraft/util/math/SectionPos;Lnet/minecraft/world/gen/feature/structure/Structure;Lnet/minecraft/world/IStructureReader;)Lnet/minecraft/world/gen/feature/structure/StructureStart;"))
    private StructureStart<?> backport$verifyRealDen(StructureManager manager,SectionPos pos,Structure<?> requested,IStructureReader chunk){
        if(!(requested instanceof TeraDenLocator.TypedDen))return local.pixelmon.backport.GymLocator.filteredStart(manager,pos,requested,chunk);
        StructureStart<?> start=manager.func_235013_a_(pos,((TeraDenLocator.TypedDen)requested).nativeDen(),chunk);
        if(start!=null && start.func_75069_d()){
            var b=start.func_75071_a();long key=new ChunkPos(b.field_78897_a>>4,b.field_78896_c>>4).func_201841_a();
            if(backport$checkedDens.add(key))backport$pendingDen=new TeraDenLocator.Candidate(world,start);
        }
        return null; // A native Max den start alone is never a successful Tera result.
    }
}
