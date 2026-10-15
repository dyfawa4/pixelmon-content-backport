package local.pixelmon.backport.client;

import com.pixelmonmod.pixelmon.blocks.tileentity.PokeStopTileEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;

/** Uses the same per-player claim check as the native detector and PokeStop. */
public final class DetectorPokeStopLines {
    private DetectorPokeStopLines() {}

    public static boolean hide(PokeStopTileEntity stop) {
        if (stop == null) return false;
        Minecraft mc = Minecraft.func_71410_x();
        if (mc.field_71441_e == null || mc.field_71439_g == null
                || stop.func_145837_r() || stop.func_145831_w() != mc.field_71441_e) return true;
        BlockPos pos = stop.func_174877_v();
        if (!mc.field_71441_e.func_217354_b(pos.func_177958_n() >> 4, pos.func_177952_p() >> 4)
                || mc.field_71441_e.func_175625_s(pos) != stop) return true;
        // Claim packets and cooldown expiry affect the existing marker immediately;
        // no clicked-position cache can leak across players, worlds, or reconnects.
        return !stop.canClaim(mc.field_71439_g);
    }
}
