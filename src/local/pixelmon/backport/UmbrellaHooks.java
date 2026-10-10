package local.pixelmon.backport;
import com.pixelmonmod.pixelmon.battles.controller.GlobalStatusController;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.battles.status.*;

/** Per-Pokemon weather view; never deletes the shared battle weather. */
public final class UmbrellaHooks {
    private UmbrellaHooks(){}
    public static boolean holds(PixelmonWrapper p){return p!=null && p.getUsableHeldItem() instanceof ReferenceItems.Umbrella;}
    public static Weather backport$filter(PixelmonWrapper p,Weather w){return holds(p) && (w instanceof Sunny || w instanceof Rainy)?null:w;}
    public static Weather backport$weather(GlobalStatusController c,PixelmonWrapper p){return backport$filter(p,c.getWeather());}
    public static Weather backport$targetWeather(GlobalStatusController c,PixelmonWrapper user){
        // Legacy cantMiss has no target argument. Native useAttack advances
        // this index for each target, including spread moves in doubles.
        PixelmonWrapper target=user.targets!=null && user.targetIndex>=0 && user.targetIndex<user.targets.size()?user.targets.get(user.targetIndex):null;
        return backport$filter(target,c.getWeather());
    }
    public static boolean backport$hasStatus(GlobalStatusController c,StatusType type,PixelmonWrapper p){return !(holds(p) && (type==StatusType.Sunny || type==StatusType.Rainy)) && c.hasStatus(type);}
    public static GlobalStatusBase backport$status(GlobalStatusController c,StatusType type,PixelmonWrapper p){return holds(p) && (type==StatusType.Sunny || type==StatusType.Rainy)?null:c.getGlobalStatus(type);}
}
