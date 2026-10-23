package local.pixelmon.backport.mixin;

import local.pixelmon.backport.AuthorBossLocator;
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
public abstract class CompassAuthorSearchMixin {
    @Shadow public ServerWorld world;
    @Shadow public Structure<?> structure;
    @Shadow public PlayerEntity player;
    @Shadow public ItemStack stack;
    @Shadow public ResourceLocation structureKey;
    @Shadow public BlockPos startPos;
    @Shadow public boolean finished;
    @Shadow public int x;
    @Shadow public int z;
    @Shadow private void finish(boolean found){throw new AssertionError();}
    @Unique private AuthorBossLocator.Search backport$authorSearch;
    @Unique private boolean backport$authorStale(){return player.field_70128_L||player.field_70170_p!=world||stack.func_77978_p()==null||!structureKey.toString().equals(stack.func_77978_p().func_74779_i("StructureKey"));}
    @Unique private void backport$closeAuthor(){if(backport$authorSearch!=null){backport$authorSearch.close();backport$authorSearch=null;}}
    @Inject(method="<init>",at=@At("RETURN"))
    private void backport$authorInit(CallbackInfo ci){if(structure instanceof AuthorBossLocator.RoomAlias){finished=!AuthorBossLocator.available(world,(AuthorBossLocator.RoomAlias)structure);if(!finished)backport$authorSearch=new AuthorBossLocator.Search(world,startPos,(AuthorBossLocator.RoomAlias)structure);}}
    @Inject(method="hasWork",at=@At("HEAD"),cancellable=true)
    private void backport$authorWork(CallbackInfoReturnable<Boolean> ci){if(structure instanceof AuthorBossLocator.RoomAlias){if(backport$authorStale()){backport$closeAuthor();finished=true;}ci.setReturnValue(!finished);}}
    @Inject(method="finish",at=@At("HEAD"),cancellable=true)
    private void backport$authorFinish(boolean found,CallbackInfo ci){if(structure instanceof AuthorBossLocator.RoomAlias){backport$closeAuthor();if(backport$authorStale()){finished=true;ci.cancel();}}}
    @Inject(method="doWork",at=@At("HEAD"),cancellable=true)
    private void backport$authorStep(CallbackInfoReturnable<Boolean> ci){
        if(!(structure instanceof AuthorBossLocator.RoomAlias))return;
        if(finished||backport$authorStale()){backport$closeAuthor();finished=true;ci.setReturnValue(false);return;}
        try{
            BlockPos found=backport$authorSearch.step();
            if(found!=null){
                if(local.pixelmon.backport.StrongholdLocator.alreadyFound(player,found))backport$authorSearch.skipFound();
                else{x=found.func_177958_n();z=found.func_177952_p();finish(true);}
            }
            else if(backport$authorSearch.finished())finish(false);
        }catch(RuntimeException failure){org.apache.logging.log4j.LogManager.getLogger("pixelmon_backport").warn("Author room compass search failed: {}",failure.toString());finish(false);}
        // Yield while a nonblocking FULL chunk request is pending, without invoking native structure sampling.
        ci.setReturnValue(false);
    }
}
