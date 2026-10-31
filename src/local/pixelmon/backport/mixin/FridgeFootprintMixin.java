package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.blocks.multi.FridgeBlock;
import local.pixelmon.backport.DayCareFootprint;
import net.minecraft.block.*;
import net.minecraft.block.material.PushReaction;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
/** Keep the native inventory/renderer at the base and reserve the visible upper cell. */
@Mixin(value=FridgeBlock.class,remap=false)
public abstract class FridgeFootprintMixin extends ContainerBlock {
 protected FridgeFootprintMixin(AbstractBlock.Properties properties){super(properties);}
 @Inject(method="func_225533_a_",at=@At("RETURN"))
 private void backport$doorSound(BlockState state,World world,BlockPos pos,net.minecraft.entity.player.PlayerEntity player,net.minecraft.util.Hand hand,net.minecraft.util.math.BlockRayTraceResult hit,CallbackInfoReturnable<net.minecraft.util.ActionResultType> ci){if(!world.field_72995_K&&hand==net.minecraft.util.Hand.MAIN_HAND){var now=world.func_180495_p(pos);if(now.func_177230_c()==state.func_177230_c()&&!now.func_177229_b(FridgeBlock.OPEN).equals(state.func_177229_b(FridgeBlock.OPEN)))local.pixelmon.backport.FridgeSounds.play(world,pos,now.func_177229_b(FridgeBlock.OPEN));}}
 @Inject(method="func_196258_a",at=@At("HEAD"),cancellable=true)
 private void backport$space(BlockItemUseContext context,CallbackInfoReturnable<BlockState> ci){BlockPos top=context.func_195995_a().func_177984_a();if(World.func_189509_E(top)||!context.func_195991_k().func_180495_p(top).func_196953_a(context))ci.setReturnValue(null);}
 @Override public void func_220082_b(BlockState s,World w,BlockPos pos,BlockState old,boolean moving){super.func_220082_b(s,w,pos,old,moving);if(s.func_177230_c()!=old.func_177230_c())DayCareFootprint.reserve(w,pos);}
 @Inject(method="func_196243_a",at=@At("HEAD"))
 private void backport$remove(BlockState s,World w,BlockPos pos,BlockState next,boolean moving,CallbackInfo ci){if(s.func_177230_c()!=next.func_177230_c())DayCareFootprint.removedLower(w,pos);}
 @Override public PushReaction func_149656_h(BlockState state){return PushReaction.BLOCK;}
 @Override public BlockState func_196271_a(BlockState s,Direction side,BlockState neighbour,IWorld w,BlockPos pos,BlockPos other){if(side==Direction.UP&&w instanceof World)DayCareFootprint.reserve((World)w,pos);return s;}
}
