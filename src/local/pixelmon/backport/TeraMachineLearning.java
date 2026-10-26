package local.pixelmon.backport;
import java.util.*;
import com.google.gson.*;
import com.pixelmonmod.pixelmon.battles.attacks.ImmutableAttack;
import com.pixelmonmod.pixelmon.api.events.npc.TeachMoveEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Tera Blast is machine-only. Existing known moves and TM compatibility are untouched. */
public final class TeraMachineLearning {
    public static boolean isTera(ImmutableAttack move){return move!=null&&"Tera Blast".equals(move.getAttackName());}
    public static void remove(JsonElement element){
        if(element.isJsonArray())for(Iterator<JsonElement> it=element.getAsJsonArray().iterator();it.hasNext();){JsonElement value=it.next();if(value.isJsonPrimitive()&&value.getAsJsonPrimitive().isString()&&"Tera Blast".equalsIgnoreCase(value.getAsString()))it.remove();else remove(value);}
        else if(element.isJsonObject())for(Map.Entry<String,JsonElement> field:element.getAsJsonObject().entrySet())remove(field.getValue());
    }
    public static List<ImmutableAttack> reminders(List<ImmutableAttack> source){List<ImmutableAttack> result=new ArrayList<>(source);result.removeIf(TeraMachineLearning::isTera);return result;}
    @SubscribeEvent public void available(TeachMoveEvent.CanLearnMove event){if(isTera(event.getAttack()))event.setCanceled(true);}
    @SubscribeEvent public void teach(TeachMoveEvent.MoveLearnt.Pre event){if(isTera(event.getAttack()))event.setCanceled(true);}
}
