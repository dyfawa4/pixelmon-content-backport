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
    public static Weather backport$moveWeather(GlobalStatusController c,PixelmonWrapper p){return ZaAbilities.moveWeather(p,backport$weather(c,p));}
    public static boolean backport$moveHasStatus(GlobalStatusController c,StatusType type,PixelmonWrapper p){
        if(ZaAbilities.personalSun(p)&&(type==StatusType.Sunny||type==StatusType.Rainy||type==StatusType.Sandstorm||type==StatusType.Hail))return type==StatusType.Sunny;
        return backport$hasStatus(c,type,p);
    }
    public static GlobalStatusBase backport$moveStatus(GlobalStatusController c,StatusType type,PixelmonWrapper p){
        if(ZaAbilities.personalSun(p)&&(type==StatusType.Sunny||type==StatusType.Rainy||type==StatusType.Sandstorm||type==StatusType.Hail))return type==StatusType.Sunny?new Sunny(-1):null;
        return backport$status(c,type,p);
    }
    public static void refreshAbility(PixelmonWrapper p){
        if(p.bc==null || p.bc.simulateMode)return;
        var ability=p.getBattleAbility();Weather weather=p.bc.globalStatusController.getWeather();
        // Protosynthesis observes the real field, unlike weather-sensitive
        // consumers such as Forecast. An umbrella is not global weather removal.
        ability.onWeatherChange(p,ability instanceof com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.Protosynthesis?weather:backport$filter(p,weather));
    }
    public static Weather backport$targetWeather(GlobalStatusController c,PixelmonWrapper user){
        // Legacy cantMiss has no target argument. Native useAttack advances
        // this index for each target, including spread moves in doubles.
        PixelmonWrapper target=user.targets!=null && user.targetIndex>=0 && user.targetIndex<user.targets.size()?user.targets.get(user.targetIndex):null;
        return ZaAbilities.moveWeather(user,backport$filter(target,c.getWeather()));
    }
    public static boolean backport$hasStatus(GlobalStatusController c,StatusType type,PixelmonWrapper p){return !(holds(p) && (type==StatusType.Sunny || type==StatusType.Rainy)) && c.hasStatus(type);}
    public static GlobalStatusBase backport$status(GlobalStatusController c,StatusType type,PixelmonWrapper p){return holds(p) && (type==StatusType.Sunny || type==StatusType.Rainy)?null:c.getGlobalStatus(type);}
}
