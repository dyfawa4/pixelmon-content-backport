package local.pixelmon.backport;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.*;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.gen.feature.structure.*;
import net.minecraftforge.registries.ForgeRegistries;

/** FTB accepts configured-feature IDs; also recognize registered structure types. */
public final class QuestStructures {
    private QuestStructures() {}

    public static boolean inside(ServerWorld world, BlockPos pos, ResourceLocation id) {
        if (world == null || pos == null || id == null
                || !world.func_217354_b(pos.func_177958_n() >> 4, pos.func_177952_p() >> 4)
                || !ForgeRegistries.STRUCTURE_FEATURES.containsKey(id)) return false;
        Structure<?> requested = ForgeRegistries.STRUCTURE_FEATURES.getValue(id);
        if (requested == null) return false;
        if(requested instanceof AuthorBossLocator.RoomAlias){
            if(((AuthorBossLocator.RoomAlias)requested).physical)return false;
            for(BlockPos home:AuthorRoomIndex.get(world).positions(false))
                if(Math.abs(pos.func_177958_n()-home.func_177958_n())<=6&&Math.abs(pos.func_177952_p()-home.func_177952_p())<=6
                    &&pos.func_177956_o()>=home.func_177956_o()&&pos.func_177956_o()<=home.func_177956_o()+7&&AuthorRoomIndex.intact(world,home,false))return true;
            return false;
        }
        Structure<?> actual = requested instanceof GymLocator.TypedGym ? GymLocator.nativeGym()
                : requested instanceof TeraDenLocator.TypedDen ? ((TeraDenLocator.TypedDen) requested).nativeDen() : requested;
        if (actual == null) return false;
        StructureStart<?> start = world.func_241112_a_().func_235010_a_(pos, true, actual);
        if (start == null || !start.func_75069_d()) return false;
        if (requested instanceof GymLocator.TypedGym)
            return GymLocator.matches(start, ((GymLocator.TypedGym) requested).type);
        if (requested instanceof TeraDenLocator.TypedDen) {
            // A compass alias is only a view. Require the corresponding real
            // structure and converted Tera entity, never an empty alias start.
            MutableBoundingBox b = start.func_75071_a();
            AxisAlignedBB box = new AxisAlignedBB(b.field_78897_a-2, b.field_78895_b-4, b.field_78896_c-2,
                    b.field_78893_d+3, b.field_78894_e+5, b.field_78892_f+3);
            return !world.func_175647_a(TeraDenEntity.class, box, den -> !den.field_70128_L && !den.isDecoration()).isEmpty();
        }
        return true;
    }
}
