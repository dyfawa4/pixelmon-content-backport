package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.entities.projectiles.HookEntity;
import com.pixelmonmod.pixelmon.api.spawning.*;
import local.pixelmon.backport.FishingEnchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=HookEntity.class,remap=false)
public abstract class FishingCatchDurabilityMixin {
    @Unique private boolean backport$landed;
    @Unique private ItemStack backport$castingRod=ItemStack.field_190927_a;
    @Inject(method="func_146034_e",at=@At("HEAD"))
    private void backport$beginReel(ItemStack stack,CallbackInfoReturnable<Integer> ci){
        backport$landed=false;var hook=(HookEntity)(Object)this;
        backport$castingRod=hook.func_234616_v_() instanceof ServerPlayerEntity?FishingEnchantments.rod((ServerPlayerEntity)hook.func_234616_v_(),hook):ItemStack.field_190927_a;
    }
    @Redirect(method="func_146034_e",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/api/spawning/SpawnAction;doSpawn(Lcom/pixelmonmod/pixelmon/api/spawning/AbstractSpawner;)Lnet/minecraft/entity/Entity;"))
    private Entity backport$recordCatch(SpawnAction<?> action,AbstractSpawner spawner){
        Entity entity=action.doSpawn(spawner);backport$landed=entity!=null;return entity;
    }
    @Inject(method="func_146034_e",at=@At("RETURN"),cancellable=true)
    private void backport$catchCost(ItemStack stack,CallbackInfoReturnable<Integer> ci){
        // Return a base damage amount to the native item path: Unbreaking/creative/Mending stay native.
        if(!FishingEnchantments.supported(backport$castingRod))return;
        int cost=backport$landed?FishingEnchantments.catchDamage(backport$castingRod):0;
        if(backport$castingRod==stack)ci.setReturnValue(cost);
        else{
            // Reeling with the other hand still charges the rod that supplied this hook's bonuses.
            var player=(ServerPlayerEntity)((HookEntity)(Object)this).func_234616_v_();
            backport$castingRod.func_222118_a(cost,player,p->{});ci.setReturnValue(0);
        }
    }
}
