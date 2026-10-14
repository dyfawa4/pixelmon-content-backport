package local.pixelmon.backport.mixin;

import java.util.List;
import local.pixelmon.backport.StrongholdLocator;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Pseudo @Mixin(targets="com.chaosthedude.explorerscompass.util.StructureSearchWorker",remap=false)
public abstract class CompassStrongholdSearchMixin {
    @Shadow public ServerWorld world;
    @Shadow public Structure<?> structure;
    @Shadow public PlayerEntity player;
    @Shadow public ItemStack stack;
    @Shadow public ResourceLocation structureKey;
    @Shadow public BlockPos startPos;
    @Shadow public boolean finished;
    @Shadow public int samples,x,z;
    @Shadow public abstract boolean hasWork();
    @Shadow private void finish(boolean found){throw new AssertionError();}
    @Unique private List<BlockPos> backport$rings;
    @Unique private int backport$ringIndex;
    @Unique private boolean backport$staleStronghold(){return StrongholdLocator.handles(structure)&&(player.field_70128_L||player.field_70170_p!=world||stack.func_77978_p()==null||!structureKey.toString().equals(stack.func_77978_p().func_74779_i("StructureKey")));}
    @Inject(method="<init>",at=@At("RETURN"))
    private void backport$ringAvailability(CallbackInfo ci){if(StrongholdLocator.handles(structure))finished=!StrongholdLocator.available(world);}
    @Inject(method="hasWork",at=@At("HEAD"),cancellable=true)
    private void backport$cancelRing(CallbackInfoReturnable<Boolean> ci){if(backport$staleStronghold()){finished=true;ci.setReturnValue(false);}}
    @Inject(method="finish",at=@At("HEAD"),cancellable=true)
    private void backport$keepRingTarget(boolean found,CallbackInfo ci){if(backport$staleStronghold()){finished=true;ci.cancel();}}
    @Inject(method="doWork",at=@At("HEAD"),cancellable=true)
    private void backport$searchRings(CallbackInfoReturnable<Boolean> ci){
        if(!StrongholdLocator.handles(structure))return;
        if(backport$staleStronghold()){finished=true;ci.setReturnValue(false);return;}
        if(!hasWork()){finish(false);ci.setReturnValue(false);return;}
        if(backport$rings==null)backport$rings=StrongholdLocator.candidates(world,startPos);
        if(backport$ringIndex>=backport$rings.size()){finish(false);ci.setReturnValue(false);return;}
        BlockPos candidate=backport$rings.get(backport$ringIndex++);x=candidate.func_177958_n();z=candidate.func_177952_p();
        // Reuse the native compass radius/sample limits after assigning the real point.
        if(!hasWork()){finish(false);ci.setReturnValue(false);return;}
        BlockPos p=player.func_233580_cy_();
        boolean near=Math.abs((long)p.func_177958_n()-x)<=32&&Math.abs((long)p.func_177952_p()-z)<=32;
        if(!near&&!StrongholdLocator.alreadyFound(player,candidate)){finish(true);ci.setReturnValue(false);return;}
        samples++;ci.setReturnValue(true);
    }
}
