package local.pixelmon.backport;
import java.util.*;
import com.pixelmonmod.pixelmon.api.config.PixelmonConfigProxy;
import com.pixelmonmod.pixelmon.api.pokemon.boss.*;
import com.pixelmonmod.pixelmon.items.PixelmonItem;
import net.minecraft.entity.player.*;
import net.minecraft.item.*;
import net.minecraft.util.*;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
public final class BossSpawnCheckItem extends PixelmonItem {
 public BossSpawnCheckItem(){super(new Item.Properties().func_200916_a(Backport.CREATIVE_TAB).func_200917_a(1).func_208103_a(Rarity.RARE));}
 private static String duration(long millis){long seconds=millis<=0?0:millis/1000+(millis%1000==0?0:1);return seconds/60+"分"+seconds%60+"秒";}
 private static String nextAttempt(World w){
  var spawner=com.pixelmonmod.pixelmon.spawning.PixelmonSpawning.megaBossSpawner;
  if(w.field_72995_K||spawner==null)return "Boss刷新计时：暂不可用，请在服务端世界使用。";
  long next=spawner.nextSpawnTime,now=System.currentTimeMillis();
  if(next<=0)return "Boss刷新计时：等待原生刷新器初始化。";
  return "距离下一次Boss刷新尝试："+(next<=now?"即将尝试（等待刷新器检查）":duration(next-now))+"；尝试不保证生成。";
 }
 public static List<String> lines(World w){
  var c=PixelmonConfigProxy.getSpawning().getBossSpawning();var tiers=new ArrayList<>(BossTierRegistry.getBossTiers());var order=List.of("common","uncommon","rare","epic","legendary","ultimate","drowned");tiers.sort(Comparator.comparingInt(t->order.contains(t.getID())?order.indexOf(t.getID()):Integer.MAX_VALUE));
  int all=tiers.stream().filter(t->t.canRandomlySpawn()&&t.getWeight()>0).mapToInt(BossTier::getWeight).sum();
  int mega=tiers.stream().filter(t->t.canRandomlySpawn()&&t.isMega()&&t.getWeight()>0).mapToInt(BossTier::getWeight).sum();
  List<String> rows=new ArrayList<>();rows.add("Boss 刷新配置 · "+w.func_234923_W_().func_240901_a_());
  float rate=c.getBossRate(w.func_234923_W_());rows.add("自然宝可梦默认Boss触发概率："+String.format(Locale.ROOT,"%.4f%%",rate>0?Math.min(1,1d/rate)*100:0)+"（1 / "+rate+"；物种独立配置可覆盖）");
  rows.add("独立Boss检查：基础间隔 "+duration(c.getBossSpawnTicks()*50L)+"（"+c.getBossSpawnTicks()+" 刻），检查通过率 "+String.format(Locale.ROOT,"%.2f%%",100d*Math.max(0,Math.min(1,c.getBossSpawnChance())))+"；实际间隔受随机浮动、人数等条件影响。");
  rows.add("已触发Boss后的等级概率（普通池 / Mega池）：");
  for(var t:tiers)if(t.canRandomlySpawn()&&t.getWeight()>0)rows.add(Map.of("common","普通","uncommon","罕见","rare","稀有","epic","史诗","legendary","传说","ultimate","终极","drowned","溺水").getOrDefault(t.getID(),t.getName())+" ["+t.getID()+"]："+String.format(Locale.ROOT,"%.2f%% / %s",all==0?0:100d*t.getWeight()/all,t.isMega()&&mega>0?String.format(Locale.ROOT,"%.2f%%",100d*t.getWeight()/mega):"—"));
  rows.add("是否优先Mega："+c.isBossesAlwaysMegaIfPossible()+"；实际生成仍受物种、群系、人数、空间及配置限制。以上条件概率不是每次刷新概率。");rows.add(nextAttempt(w));return rows;
 }
 public static List<ITextComponent> components(World w){List<ITextComponent> out=new ArrayList<>();for(String line:lines(w)){var text=new StringTextComponent(line);for(var tier:BossTierRegistry.getBossTiers())if(line.contains("["+tier.getID()+"]")){text.func_230530_a_(Style.field_240709_b_.func_240718_a_(Color.func_240743_a_(tier.getColor().getRGB()&0xffffff)));break;}out.add(text);}return out;}
 @Override public ActionResult<ItemStack> func_77659_a(World w,PlayerEntity p,Hand h){var stack=p.func_184586_b(h);if(w.field_72995_K||!(p instanceof ServerPlayerEntity)||p.func_184811_cZ().func_185141_a(this))return ActionResult.func_226250_c_(stack);p.func_184811_cZ().func_185145_a(this,60);for(ITextComponent line:components(w))p.func_146105_b(line,false);return ActionResult.func_226248_a_(stack);}
 @Override public void func_77624_a(ItemStack s,World w,List<ITextComponent> out,net.minecraft.client.util.ITooltipFlag f){out.add(new TranslationTextComponent("pixelmon.backport.boss_check.description"));}
}
