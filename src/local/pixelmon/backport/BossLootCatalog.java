package local.pixelmon.backport;
import java.util.*;
import java.lang.reflect.Field;
import com.pixelmonmod.pixelmon.api.pokemon.boss.*;
import com.pixelmonmod.pixelmon.api.util.WeightedSet;
import net.minecraft.item.*;
/** Inspect live reward definitions without rolling rewards or marking a player's Mega discoveries. */
public final class BossLootCatalog {
 private BossLootCatalog(){}
 private static final Map<String,String> NAMES=Map.of("common","普通","uncommon","优秀","rare","罕见","epic","史诗","legendary","传说","ultimate","终极","drowned","溺水","haunted","幽灵","equal","实力相当");
 private static String percent(double chance){return new java.math.BigDecimal(Double.toString(chance)).multiply(java.math.BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString()+"%";}
 @SuppressWarnings("unchecked") public static <T>T field(Object value,String name){try{Field f=value.getClass().getDeclaredField(name);f.setAccessible(true);return (T)f.get(value);}catch(ReflectiveOperationException e){throw new IllegalStateException("Native Boss reward definition unavailable: "+name,e);}}
 public static List<LootSourceCatalog.Source> sources(){
  List<LootSourceCatalog.Source> result=new ArrayList<>();
  for(var tier:BossTierRegistry.getBossTiers())if(tier.isBoss()){
   List<LootSourceCatalog.Entry> entries=new ArrayList<>();int number=0;
   List<DropSet> sets=field(tier,"dropSets");for(var set:sets){number++;double chance=field(set,"chance");int quantity=field(set,"quantity");WeightedSet<DropSet.Drop> choices=field(set,"drops");
    for(var it=choices.iterator();it.hasNext();){var drop=it.next();var stack=drop.getDisplayItem();if(stack==null||stack.func_190926_b())continue;
     String note="第"+number+"组，触发概率"+percent(chance)+"，抽取"+quantity+"次；池内权重"+drop.getWeight()+"；重复项按原生规则合并";
     var access=(local.pixelmon.backport.mixin.RewardDropAccess)(Object)drop;if(access.backport$commands()!=null&&access.backport$commands().length>0)note+="；此图标表示命令奖励："+drop.getName();
     entries.add(new LootSourceCatalog.Entry(stack,note));
    }
   }
   if(tier.isMega()){
    double first=field(tier,"megaStoneObtainChance"),repeat=field(tier,"megaStoneRepeatChance");Set<Item> seen=Collections.newSetFromMap(new IdentityHashMap<>());
    for(var stone:com.pixelmonmod.pixelmon.api.registries.PixelmonItems.megaStones)if(!DrownedBosses.exclusive(stone)&&seen.add(stone))entries.add(new LootSourceCatalog.Entry(new ItemStack(stone),"仅击败有对应Mega形态的Boss；首次获得概率"+percent(first)+"，重复获得概率"+percent(repeat)+"；按Boss种类选择对应石头"));
   }
   if(tier.getID().equals("drowned"))DrownedBosses.catalogue(entries);
   result.add(new LootSourceCatalog.Source("bosses","boss/"+tier.getID(),NAMES.getOrDefault(tier.getID(),tier.getID())+"Boss","基础掉落随宝可梦种类变化；此页为当前加载的等级额外奖池与Mega石条件",entries));
  }
  List<LootSourceCatalog.Entry> author=new ArrayList<>();author.add(new LootSourceCatalog.Entry(new ItemStack(Backport.item("ash_greninjite")),"每位玩家首次击败作者必定额外获得1颗，此后独立1%；不占五份随机战利品，不可从溺水Boss获得"));result.add(new LootSourceCatalog.Source("bosses","boss/author_exclusive","作者道馆·专属奖励","击败作者后在胜利领奖界面领取",author));
  return result;
 }
}
