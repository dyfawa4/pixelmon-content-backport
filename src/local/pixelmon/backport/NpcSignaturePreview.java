package local.pixelmon.backport;
import com.pixelmonmod.pixelmon.battles.controller.log.MoveResults;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
/** Retain native multi-hit aggregation across Attack.use's nested result objects. */
public final class NpcSignaturePreview {
 private static final ThreadLocal<Capture> ACTIVE=new ThreadLocal<>();
 private NpcSignaturePreview(){}
 public static Capture begin(PixelmonWrapper actor){return new Capture(actor);}
 public static void record(PixelmonWrapper actor,MoveResults result){var c=ACTIVE.get();if(c!=null&&c.actor==actor){c.damage=result.damage;c.full=result.fullDamage;}}
 public static final class Capture implements AutoCloseable {
  private final PixelmonWrapper actor;private final Capture previous;private int damage=-1,full=-1;
  private Capture(PixelmonWrapper actor){this.actor=actor;previous=ACTIVE.get();ACTIVE.set(this);}
  public void apply(MoveResults result,PixelmonWrapper target,PixelmonWrapper projected){
   if(damage<0)return;
   // The choice's target remains the original wrapper. Express damage on that
   // HP scale after the detached target's proportional Erosion resize.
   double scale=target.getMaxHealth()/(double)Math.max(1,projected.getMaxHealth());
   result.damage=Math.min(target.getHealth(),(int)Math.floor(damage*scale));result.fullDamage=(int)Math.floor(full*scale);
  }
  @Override public void close(){if(previous==null)ACTIVE.remove();else ACTIVE.set(previous);}
 }
}
