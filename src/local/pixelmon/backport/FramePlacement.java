package local.pixelmon.backport;

import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistries;

/** Native placement and consumption remain owned by PaintingItem. */
public final class FramePlacement {
    private FramePlacement(){}
    public static boolean spawn(World world,Entity frame){
        boolean added=world.func_217376_c(frame);
        if(added&&!world.field_72995_K){
            SoundEvent sound=ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("minecraft","entity.item_frame.place"));
            if(sound!=null)world.func_184133_a(null,frame.func_233580_cy_(),sound,SoundCategory.BLOCKS,1.0f,1.0f);
        }
        return added;
    }
}
