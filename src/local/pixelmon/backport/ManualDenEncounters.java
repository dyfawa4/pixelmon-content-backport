package local.pixelmon.backport;
import java.util.*;import java.util.function.Supplier;
import com.pixelmonmod.pixelmon.entities.DenEntity;
import com.pixelmonmod.pixelmon.battles.raids.RaidData;
/** Manual wishes bypass area selection only; legal species rules stay separate. */
public final class ManualDenEncounters {
 public static final String TAG="BackportManualWish";
 private static final ThreadLocal<DenEntity> CURRENT=new ThreadLocal<>();
 private static final Map<DenEntity,RaidData> MANUAL=Collections.synchronizedMap(new WeakHashMap<>());
 private ManualDenEncounters(){}
 public static boolean choosing(DenEntity den){return CURRENT.get()==den;}
 public static boolean manual(DenEntity den,RaidData data){return data!=null&&MANUAL.get(den)==data;}
 public static <T>T choose(DenEntity den,Supplier<T> action){var previous=CURRENT.get();try{CURRENT.set(den);return action.get();}finally{if(previous==null)CURRENT.remove();else CURRENT.set(previous);}}
 public static void remember(DenEntity den){var data=den.getServerData();if(data!=null&&data.getStars()>0)MANUAL.put(den,data);}
 public static void set(DenEntity den,RaidData data){choose(den,()->{den.setData(data);return null;});remember(den);}
}
