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
    public static final String TAG="AuthorArenaFurnished5";
    public static final String PRIZE="BackportAuthorTablePrize190";
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
    public static void place(IWorld world,BlockPos home){place(world,home,Rotation.NONE);}
    public static void place(IWorld world,BlockPos home,Rotation rotation){
        Direction front=rotation.func_185831_a(Direction.SOUTH);
        BlockPos chest=AuthorRoomRotation.pos(home,-5,1,-5,rotation);
        if(place(world,chest,state("minecraft:chest",front))){
            var tile=world.func_175625_s(chest);
            if(tile instanceof ChestTileEntity)((ChestTileEntity)tile).func_189404_a(new ResourceLocation("minecraft:chests/woodland_mansion"),world.func_201674_k().nextLong());
        }
        place(world,AuthorRoomRotation.pos(home,-4,1,-5,rotation),state("pixelmon:red_healer",rotation.func_185831_a(Direction.NORTH)));
        BlockPos pc=AuthorRoomRotation.pos(home,-3,1,-5,rotation);BlockState pcState=state("pixelmon:red_pc",rotation.func_185831_a(Direction.NORTH));
        if(world.func_175623_d(pc)&&world.func_175623_d(pc.func_177982_a(0,1,0))){
            place(world,pc,pcState);place(world,pc.func_177982_a(0,1,0),pcState.func_206870_a(MultiBlock.MULTIPOS,MultiBlockPosition.TOP));
        }
        BlockPos head=AuthorRoomRotation.pos(home,-2,1,-5,rotation),foot=AuthorRoomRotation.pos(home,-2,1,-4,rotation);
        if(world.func_175623_d(head)&&world.func_175623_d(foot)){
            BlockState bed=state("minecraft:red_bed",rotation.func_185831_a(Direction.NORTH));
            place(world,head,bed.func_206870_a(BedBlock.field_176472_a,BedPart.HEAD));
            place(world,foot,bed.func_206870_a(BedBlock.field_176472_a,BedPart.FOOT));
        }
        BlockPos table=AuthorRoomRotation.pos(home,-1,1,-5,rotation);
        place(world,table,state("pixelmon:end_table",front));
        prize(world,table);
        if(world.func_180495_p(table).func_177230_c()==ForgeRegistries.BLOCKS.getValue(new ResourceLocation("pixelmon:end_table")))place(world,table.func_177984_a(),state("pixelmon:lightworkplace",front));
        place(world,AuthorRoomRotation.pos(home,-1,1,-4,rotation),state("pixelmon:orange_chair",rotation.func_185831_a(Direction.NORTH)));
        place(world,AuthorRoomRotation.pos(home,-5,1,-4,rotation),state("minecraft:crafting_table",front));
    }
    private static void prize(IWorld world,BlockPos pos){
        var tile=world.func_175625_s(pos);
        if(!(tile instanceof com.pixelmonmod.pixelmon.blocks.tileentity.EndTableTileEntity)||tile.getTileData().func_74767_n(PRIZE))return;
        var table=(com.pixelmonmod.pixelmon.blocks.tileentity.EndTableTileEntity)tile;
        // Existing player contents are never overwritten; mark the migration
        // even when occupied so looting/emptying it later cannot refill a prize.
        if(table.func_191420_l()){
            var random=world.func_201674_k();var id=new ResourceLocation("minecraft",random.nextBoolean()?"diamond":"netherite_scrap");
            int count=1+random.nextInt(3),slot=random.nextInt(table.func_70302_i_());
            table.func_70299_a(slot,new net.minecraft.item.ItemStack(ForgeRegistries.ITEMS.getValue(id),count));
        }
        tile.getTileData().func_74757_a(PRIZE,true);tile.func_70296_d();
    }
    /** Only rotate exact legacy machines; same-block state updates retain their tile data. */
    public static void upgrade(IWorld world,BlockPos home){
        turn(world,home.func_177982_a(-4,1,-5),"pixelmon:red_healer");
        turn(world,home.func_177982_a(-3,1,-5),"pixelmon:red_pc");
        turn(world,home.func_177982_a(-3,2,-5),"pixelmon:red_pc");
        BlockPos chair=home.func_177982_a(-1,1,-4);BlockState old=world.func_180495_p(chair);
        if(old.func_177230_c()==ForgeRegistries.BLOCKS.getValue(new ResourceLocation("pixelmon:orange_chair"))
            &&old.func_235903_d_(HorizontalBlock.field_185512_D).orElse(null)==Direction.SOUTH)
            world.func_180501_a(chair,old.func_206870_a(HorizontalBlock.field_185512_D,Direction.NORTH),2);
    }
    private static void turn(IWorld world,BlockPos pos,String id){
        BlockState current=world.func_180495_p(pos);
        if(current.func_177230_c()==ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id))
            &&current.func_235903_d_(HorizontalBlock.field_185512_D).orElse(null)==Direction.SOUTH)
            world.func_180501_a(pos,current.func_206870_a(HorizontalBlock.field_185512_D,Direction.NORTH),2);
    }
}
