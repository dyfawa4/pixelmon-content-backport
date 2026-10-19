package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.CommonProxy;
import com.pixelmonmod.pixelmon.items.SpawnDenItem;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
/** Explicit native placement remains a Max den; only unclassified world dens convert. */
@Mixin(value=SpawnDenItem.class,remap=false)
public abstract class MaxDenPlacementMixin {
    @Redirect(method="func_77659_a",at=@At(value="INVOKE",target="Lnet/minecraft/entity/player/PlayerEntity;func_184812_l_()Z"))
    private boolean backport$survivalPlacement(PlayerEntity player){return !player.func_175149_v()&&player.field_71075_bZ.field_75099_e;}
    @Redirect(method="func_77659_a",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/CommonProxy;spawnEntitySafely(Lnet/minecraft/entity/Entity;Lnet/minecraft/world/World;)V"))
    private void backport$maxDen(Entity den,World world,World originalWorld,PlayerEntity player,Hand hand){
        den.getPersistentData().func_74768_a("BackportDenKind",0);
        var stack=player.func_184586_b(hand);
        // Keep the native server-thread scheduling. Consume the captured stack
        // only after insertion succeeds, never a newly switched hand item.
        world.func_73046_m().execute(()->{
            if(stack.func_190926_b()||player.func_184586_b(hand)!=stack)return;
            if(world.func_217376_c(den)&&!player.field_71075_bZ.field_75098_d)stack.func_190918_g(1);
        });
    }
}
