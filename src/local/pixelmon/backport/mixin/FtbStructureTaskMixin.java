package local.pixelmon.backport.mixin;

import local.pixelmon.backport.QuestStructures;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo @Mixin(targets="dev.ftb.mods.ftbquests.quest.task.StructureTask", remap=false)
public abstract class FtbStructureTaskMixin {
    @Shadow public RegistryKey<?> structure;

    @Inject(method="canSubmit", at=@At("RETURN"), cancellable=true)
    private void backport$registeredStructure(@Coerce Object team, ServerPlayerEntity player, CallbackInfoReturnable<Boolean> ci) {
        if (!ci.getReturnValueZ() && structure != null && player.field_70170_p instanceof ServerWorld
                && QuestStructures.inside((ServerWorld) player.field_70170_p, player.func_233580_cy_(), structure.func_240901_a_()))
            ci.setReturnValue(true);
    }
}
