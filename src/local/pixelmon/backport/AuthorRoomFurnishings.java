package local.pixelmon.backport;
import net.minecraft.block.*;
import net.minecraft.state.properties.*;
import net.minecraft.tileentity.ChestTileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraftforge.registries.ForgeRegistries;
import com.pixelmonmod.pixelmon.blocks.MultiBlock;
import com.pixelmonmod.pixelmon.blocks.enums.MultiBlockPosition;
/** A compact preparation corner against the back wall, clear of the author and doorway. */
public final class AuthorRoomFurnishings {
    public static final String TAG="AuthorArenaFurnished1";
    private AuthorRoomFurnishings(){}
    private static BlockState state(String id,Direction facing){
        Block block=ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id));
        if(block==null||block==Blocks.field_150350_a)throw new IllegalStateException("Missing author furnishing "+id);
        BlockState state=block.func_176223_P();
        if(state.func_235903_d_(net.minecraft.block.HorizontalBlock.field_185512_D).isPresent())state=state.func_206870_a(net.minecraft.block.HorizontalBlock.field_185512_D,facing);
        if(state.func_235903_d_(MultiBlock.MULTIPOS).isPresent())state=state.func_206870_a(MultiBlock.MULTIPOS,MultiBlockPosition.BASE);
        return state;
    }
    private static boolean place(IWorld world,BlockPos pos,BlockState state){
        if(!world.func_175623_d(pos))return false;
        world.func_180501_a(pos,state,2);return true;
    }
    public static void place(IWorld world,BlockPos home){
        Direction front=Direction.SOUTH;
        BlockPos chest=home.func_177982_a(-5,1,-5);
        if(place(world,chest,state("minecraft:chest",front))){
            var tile=world.func_175625_s(chest);
            if(tile instanceof ChestTileEntity)((ChestTileEntity)tile).func_189404_a(new ResourceLocation("minecraft:chests/woodland_mansion"),world.func_201674_k().nextLong());
        }
        place(world,home.func_177982_a(-4,1,-5),state("pixelmon:red_healer",front));
        BlockPos pc=home.func_177982_a(-3,1,-5);BlockState pcState=state("pixelmon:red_pc",front);
        if(world.func_175623_d(pc)&&world.func_175623_d(pc.func_177982_a(0,1,0))){
            place(world,pc,pcState);place(world,pc.func_177982_a(0,1,0),pcState.func_206870_a(MultiBlock.MULTIPOS,MultiBlockPosition.TOP));
        }
        BlockPos head=home.func_177982_a(-2,1,-5),foot=home.func_177982_a(-2,1,-4);
        if(world.func_175623_d(head)&&world.func_175623_d(foot)){
            BlockState bed=state("minecraft:red_bed",Direction.NORTH);
            place(world,head,bed.func_206870_a(BedBlock.field_176472_a,BedPart.HEAD));
            place(world,foot,bed.func_206870_a(BedBlock.field_176472_a,BedPart.FOOT));
        }
        place(world,home.func_177982_a(-1,1,-5),state("pixelmon:end_table",front));
        place(world,home.func_177982_a(-1,1,-4),state("pixelmon:orange_chair",Direction.NORTH));
        place(world,home.func_177982_a(-5,1,-4),state("minecraft:crafting_table",front));
    }
}
