package local.pixelmon.backport.mixin;
import local.pixelmon.backport.DayCareFootprint;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Optional Jade presentation accessor; gameplay ray hits and registry IDs stay unchanged. */
@Pseudo @Mixin(targets="mcp.mobius.waila.api.impl.DataAccessor",remap=false)
public abstract class JadeDayCareAccessorMixin {
 @Shadow public World world;
 @Shadow public PlayerEntity player;
 @Shadow public RayTraceResult hitResult;
 @Shadow public BlockPos pos;
 @Shadow public BlockState state;
 @Shadow public Block block;
 @Shadow public ResourceLocation blockRegistryName;
 @Shadow public TileEntity tileEntity;
 @Shadow public ItemStack stack;
 @Inject(method="set(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/math/RayTraceResult;Lnet/minecraft/entity/Entity;D)V",at=@At("RETURN"))
 private void backport$wholeDaycare(World w,PlayerEntity p,RayTraceResult hit,Entity entity,double partial,CallbackInfo ci){
  if(!(hitResult instanceof BlockRayTraceResult)||!DayCareFootprint.upper(state))return;
  BlockPos base=pos.func_177977_b();BlockState lower=world.func_180495_p(base);if(!DayCareFootprint.lower(lower))return;
  BlockRayTraceResult old=(BlockRayTraceResult)hitResult;
  pos=base;state=lower;block=lower.func_177230_c();blockRegistryName=block.getRegistryName();tileEntity=world.func_175625_s(base);
  hitResult=new BlockRayTraceResult(old.func_216347_e(),old.func_216354_b(),base,old.func_216353_d());
  stack=block.getPickBlock(state,hitResult,world,pos,player);
 }
}
