package local.pixelmon.backport;
import java.util.*;
import net.minecraft.item.*;
/** Independent registered item identity; no NBT subtype, name, glint or loot aliasing. */
public final class EndlessHeldItems {
 public interface Endless {}
 // Battle-local usage survives switching and wrapper copies; never saved in an item or Pokemon.
 private static final Map<com.pixelmonmod.pixelmon.battles.controller.BattleController,Map<UUID,Set<Item>>> USED=Collections.synchronizedMap(new WeakHashMap<>());
 public static boolean spent(com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper p){
  if(p==null||p.bc==null)return false;var stack=p.pokemon.getHeldItem();if(!is(stack))return false;
  synchronized(USED){var battle=USED.get(p.bc);return battle!=null&&battle.getOrDefault(p.getPokemonUUID(),Collections.emptySet()).contains(stack.func_77973_b());}
 }
 public static boolean trigger(com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper p){
  var stack=p.pokemon.getHeldItem();if(!is(stack))return false;
  if(p.bc!=null&&!p.bc.simulateMode&&!p.bc.simulateStats&&!AiEffectEvaluation.active())synchronized(USED){USED.computeIfAbsent(p.bc,k->new HashMap<>()).computeIfAbsent(p.getPokemonUUID(),k->new HashSet<>()).add(stack.func_77973_b());}
  return true;
 }
 public static final String TAG="BackportEndless";
 public static final Set<String> IDS=new LinkedHashSet<>();
 static{for(var value:Content.read("backport/endless_held_items.json").getAsJsonArray("items"))IDS.add(value.getAsString());}
 public static boolean eligible(ItemStack s){return s!=null&&!s.func_190926_b()&&IDS.contains(String.valueOf(s.func_77973_b().getRegistryName()));}
 public static boolean is(ItemStack s){return s!=null&&s.func_77973_b() instanceof Endless;}
 public static String name(String id){return "endless_"+id.substring(id.indexOf(':')+1);}
 public static ItemStack of(Item item){String id=String.valueOf(item.getRegistryName());if(!IDS.contains(id))throw new IllegalArgumentException(id);return new ItemStack(Backport.item(name(id)));}
 public static ItemStack migrate(ItemStack stack){if(eligible(stack)&&stack.func_77942_o()&&stack.func_77978_p().func_74767_n(TAG)){var replacement=of(stack.func_77973_b());replacement.func_190920_e(stack.func_190916_E());var tag=stack.func_77978_p().func_74737_b();tag.func_82580_o(TAG);replacement.func_77982_d(tag);return replacement;}return stack;}
 public static final class Migration {
  @net.minecraftforge.eventbus.api.SubscribeEvent public void tick(net.minecraftforge.event.TickEvent.PlayerTickEvent e){if(e.phase!=net.minecraftforge.event.TickEvent.Phase.END||e.player.field_70170_p.field_72995_K||e.player.field_70173_aa%20!=0)return;var p=e.player;for(int i=0;i<p.field_71071_by.func_70302_i_();i++){var old=p.field_71071_by.func_70301_a(i);var converted=migrate(old);if(converted!=old)p.field_71071_by.func_70299_a(i,converted);}for(var mon:com.pixelmonmod.pixelmon.api.storage.StorageProxy.getParty(p.func_110124_au()).getAll())if(mon!=null){var old=mon.getHeldItem();var converted=migrate(old);if(converted!=old)mon.setHeldItem(converted);}}
 }
}
