package local.pixelmon.backport;
import net.minecraft.world.World;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
public final class FridgeSounds {
 public static void play(World world,BlockPos pos,boolean open){if(world==null||world.field_72995_K)return;var sound=net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("minecraft",open?"block.barrel.open":"block.barrel.close"));world.func_184133_a(null,pos,sound,SoundCategory.BLOCKS,.5f,world.field_73012_v.nextFloat()*.1f+.9f);}
}
