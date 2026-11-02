package local.pixelmon.backport.mixin;
import java.util.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Edited compass initializes these shared model mappings only in client setup. */
@Pseudo @Mixin(targets="com.chaosthedude.explorerscompass.config.BetterUI",remap=false)
public abstract class CompassServerModelsMixin {
 @Shadow private static List<Map.Entry<String,Integer>> entries;
 @Shadow public static void loadConfig(String name){throw new AssertionError();}
 @Inject(method="getEntries",at=@At("HEAD"))
 private static void backport$initialize(CallbackInfoReturnable<List<Map.Entry<String,Integer>>> ci){
  if(entries!=null||net.minecraftforge.fml.loading.FMLEnvironment.dist!=net.minecraftforge.api.distmarker.Dist.DEDICATED_SERVER)return;
  try{loadConfig("explorerscompass");}catch(RuntimeException failure){org.apache.logging.log4j.LogManager.getLogger("pixelmon_backport").warn("Invalid compass model configuration; search coordinates remain usable",failure);}
  if(entries==null)entries=Collections.emptyList();
 }
}
