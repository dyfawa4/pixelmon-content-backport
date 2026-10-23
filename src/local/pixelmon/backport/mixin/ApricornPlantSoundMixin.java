package local.pixelmon.backport.mixin;

import com.pixelmonmod.pixelmon.items.ApricornItem;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.*;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Native planting bypasses BlockItem's placement sound. Broadcast once after placement. */
@Mixin(value=ApricornItem.class,remap=false)
public abstract class ApricornPlantSoundMixin {
 @Inject(method="func_195939_a",at=@At(value="INVOKE",target="Lnet/minecraft/block/Block;func_180633_a(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;)V",shift=At.Shift.AFTER),require=1)
 private void backport$plantSound(ItemUseContext context,CallbackInfoReturnable<ActionResultType> cir){
  var world=context.func_195991_k();if(world.field_72995_K)return;
  var type=((ApricornItem)(Object)this).apricorn;var position=context.func_195995_a().func_177984_a();
  if(type==null||world.func_180495_p(position).func_177230_c()!=type.saplingBlock())return;
  var sound=ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("minecraft:item.crop.plant"));
  if(sound!=null)world.func_184133_a(null,position,sound,SoundCategory.BLOCKS,1,1);
 }
}
