package local.pixelmon.backport;
import java.util.*;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.*;
/** One receipt sound per successful UI action, after the native inventory transfer. */
public final class BossPickupSound {
 private static final Set<UUID> BOSSES=new HashSet<>();
 private static final Map<UUID,Integer> BEFORE=new HashMap<>();
 public static void register(ServerPlayerEntity player,boolean boss){if(boss)BOSSES.add(player.func_110124_au());else BOSSES.remove(player.func_110124_au());}
 private static int count(ServerPlayerEntity player){int n=0;for(var stack:player.field_71071_by.field_70462_a)n+=stack.func_190916_E();return n;}
 public static void before(ServerPlayerEntity player){if(BOSSES.contains(player.func_110124_au()))BEFORE.put(player.func_110124_au(),count(player));}
 public static void after(ServerPlayerEntity player){Integer n=BEFORE.remove(player.func_110124_au());if(n!=null&&count(player)>n)player.func_213823_a(net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("minecraft:entity.item.pickup")),SoundCategory.PLAYERS,.2f,1f);}
 public static void remove(ServerPlayerEntity player){BOSSES.remove(player.func_110124_au());}
}
