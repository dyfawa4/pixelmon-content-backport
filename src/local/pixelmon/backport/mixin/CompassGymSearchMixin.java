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
    private boolean backport$stale(){
        return structure instanceof GymLocator.TypedGym && (player.field_70170_p!=world || stack.func_77978_p()==null
            || !structureKey.toString().equals(stack.func_77978_p().func_74779_i("StructureKey")));
    }
    @Inject(method="hasWork",at=@At("HEAD"),cancellable=true)
    private void backport$stopSupersededSearch(CallbackInfoReturnable<Boolean> ci){
        if(backport$stale()){finished=true;ci.setReturnValue(false);}
    }
    @Inject(method="finish",at=@At("HEAD"),cancellable=true)
    private void backport$keepCurrentTarget(boolean found,CallbackInfo ci){
        if(backport$stale()){finished=true;ci.cancel();}
    }
    @Inject(method="<init>",at=@At("RETURN"))
    private void backport$gymSettings(CallbackInfo ci){
        if(structure instanceof GymLocator.TypedGym){separationSettings=GymLocator.settings(world);finished=!GymLocator.available(world,((GymLocator.TypedGym)structure).type);}
    }
    @Redirect(method="doWork",at=@At(value="INVOKE",target="Lnet/minecraft/world/gen/feature/structure/StructureManager;func_235013_a_(Lnet/minecraft/util/math/SectionPos;Lnet/minecraft/world/gen/feature/structure/Structure;Lnet/minecraft/world/IStructureReader;)Lnet/minecraft/world/gen/feature/structure/StructureStart;"))
    private StructureStart<?> backport$actualGym(StructureManager manager,SectionPos pos,Structure<?> requested,IStructureReader chunk){return GymLocator.filteredStart(manager,pos,requested,chunk);}
}
