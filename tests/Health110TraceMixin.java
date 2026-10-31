package qa.mixin;
import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.*;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
@Mixin(value=Pokemon.class,remap=false)
public class Health110TraceMixin {
 @Unique private static int traced;
 @Inject(method={"setHealth","setHealthCache"},at=@At("HEAD")) private void health(int hp,CallbackInfo ci){Pokemon p=(Pokemon)(Object)this;if(qa.Health110Probe.watched==null||java.util.Arrays.stream(qa.Health110Probe.watched).noneMatch(w->w.pokemon==p)||java.util.Arrays.stream(Thread.currentThread().getStackTrace()).anyMatch(l->l.getClassName().endsWith("IVStore")||l.getClassName().endsWith("EVStore"))||traced++>90)return;System.out.println("HEALTH_TRACE dex="+p.getSpecies().getDex()+" old="+p.getHealth()+" requested="+hp+" max="+p.getStat(com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType.HP));for(var line:Thread.currentThread().getStackTrace())if(line.getClassName().startsWith("com.pixelmonmod")||line.getClassName().startsWith("local.pixelmon"))System.out.println("HEALTH_TRACE "+line);}
}
