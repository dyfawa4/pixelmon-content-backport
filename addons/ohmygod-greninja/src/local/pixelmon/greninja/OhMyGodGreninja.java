package local.pixelmon.greninja;
/** The core owns the two preserved effects; installing this companion enables them. */
@net.minecraftforge.fml.common.Mod("ohmygod_greninja")
public final class OhMyGodGreninja {
 public OhMyGodGreninja(){net.minecraftforge.fml.network.NetworkRegistry.newSimpleChannel(new net.minecraft.util.ResourceLocation("ohmygod_greninja","mode"),()->"1","1"::equals,"1"::equals);}
}
