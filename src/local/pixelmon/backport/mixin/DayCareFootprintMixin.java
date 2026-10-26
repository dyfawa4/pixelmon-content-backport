package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.blocks.daycare.DayCareBlock;
import local.pixelmon.backport.DayCareFootprint;
import net.minecraft.block.*;
import net.minecraft.block.material.PushReaction;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.util.math.*;
import net.minecraft.util.math.shapes.*;
import net.minecraft.world.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=DayCareBlock.class,remap=false)
public abstract class DayCareFootprintMixin extends Block {
    protected DayCareFootprintMixin(AbstractBlock.Properties p){super(p);}
    @Inject(method="func_196258_a",at=@At("HEAD"),cancellable=true)
    private void backport$twoCells(BlockItemUseContext context,CallbackInfoReturnable<BlockState> cir){BlockPos top=context.func_195995_a().func_177984_a();if(World.func_189509_E(top)||!context.func_195991_k().func_180495_p(top).func_196953_a(context))cir.setReturnValue(null);}
    @Inject(method="func_220053_a",at=@At("HEAD"),cancellable=true)
    private void backport$lowerOutline(BlockState state,IBlockReader world,BlockPos pos,ISelectionContext context,CallbackInfoReturnable<VoxelShape> cir){cir.setReturnValue(VoxelShapes.func_197868_b());}
    @Override public void func_220082_b(BlockState state,World world,BlockPos pos,BlockState old,boolean moving){super.func_220082_b(state,world,pos,old,moving);if(state.func_177230_c()!=old.func_177230_c())DayCareFootprint.reserve(world,pos);}
    @Override public void func_196243_a(BlockState state,World world,BlockPos pos,BlockState next,boolean moving){if(state.func_177230_c()!=next.func_177230_c())DayCareFootprint.removedLower(world,pos);super.func_196243_a(state,world,pos,next,moving);}
    @Override public PushReaction func_149656_h(BlockState state){return PushReaction.BLOCK;}
    @Override public BlockState func_196271_a(BlockState state,net.minecraft.util.Direction side,BlockState neighbour,IWorld world,BlockPos pos,BlockPos other){if(side==net.minecraft.util.Direction.UP&&world instanceof World)DayCareFootprint.reserve((World)world,pos);return state;}
}
