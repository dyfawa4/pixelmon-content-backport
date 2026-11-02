package local.pixelmon.backport;
/** Visibility only: registry identity, commands and gameplay remain available. */
public final class SecretItems {
 private SecretItems(){}
 public static boolean hidden(net.minecraft.item.Item item){return item==com.pixelmonmod.pixelmon.api.registries.PixelmonItems.jade_orb||item==com.pixelmonmod.pixelmon.api.registries.PixelmonItems.delta_shard;}
}
