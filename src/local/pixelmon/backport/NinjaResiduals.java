package local.pixelmon.backport;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.battles.status.*;

/** Native personal weather/terrain consequences, without advancing shared durations. */
public final class NinjaResiduals {
    private NinjaResiduals(){}
    public static void apply(PixelmonWrapper p){
        if(!p.isAlive())return;
        var global=p.bc.globalStatusController;Weather weather=global.getWeather();
        if(!GlobalStatusBase.ignoreWeather(p.bc)){
            if(weather instanceof Sandstorm)((Sandstorm)weather).applyRepeatedEffect(p.bc);
            else if(weather instanceof Hail)((Hail)weather).applyRepeatedEffect(p.bc);
        }
        if(global.getTerrain() instanceof GrassyTerrain)((GrassyTerrain)global.getTerrain()).applyRepeatedEffect(global);
    }
}
