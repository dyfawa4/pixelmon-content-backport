package local.pixelmon.backport.client;
import java.util.function.Supplier;
/** Only tooltip construction sees expanded information; real controls keep their key state. */
public final class ExpandedItemTooltips {
 private static final ThreadLocal<Integer> DEPTH=ThreadLocal.withInitial(()->0);
 public static boolean active(){return DEPTH.get()>0;}
 public static <T>T during(Supplier<T> action){int previous=DEPTH.get();DEPTH.set(previous+1);try{return action.get();}finally{if(previous==0)DEPTH.remove();else DEPTH.set(previous);}}
}
