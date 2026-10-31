package local.pixelmon.backport;
import java.util.*;
import com.google.gson.*;
import com.pixelmonmod.pixelmon.battles.attacks.ImmutableAttack;
import com.pixelmonmod.pixelmon.api.events.npc.TeachMoveEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import java.util.function.Predicate;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.battles.BattleRegistry;
import com.pixelmonmod.pixelmon.items.TechnicalMoveItem;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import local.pixelmon.backport.mixin.LearnMoveControllerAccess;
import local.pixelmon.backport.mixin.LearnMoveRequestAccess;

/** Tera Blast is machine-only. Existing known moves and TM compatibility are untouched. */
public final class TeraMachineLearning {
    public static boolean isTera(ImmutableAttack move){return move!=null&&"Tera Blast".equals(move.getAttackName());}
    public static void remove(JsonElement element){
        if(element.isJsonArray())for(Iterator<JsonElement> it=element.getAsJsonArray().iterator();it.hasNext();){JsonElement value=it.next();if(value.isJsonPrimitive()&&value.getAsJsonPrimitive().isString()&&"Tera Blast".equalsIgnoreCase(value.getAsString()))it.remove();else remove(value);}
        else if(element.isJsonObject())for(Map.Entry<String,JsonElement> field:element.getAsJsonObject().entrySet())remove(field.getValue());
    }
    public static List<ImmutableAttack> reminders(List<ImmutableAttack> source){List<ImmutableAttack> result=new ArrayList<>(source);result.removeIf(TeraMachineLearning::isTera);return result;}
    private static final ThreadLocal<UUID> MACHINE_CHECK=new ThreadLocal<>();
    private static final class MachineCost implements Predicate<ServerPlayerEntity> {
        final UUID owner,pokemon;final ItemStack offered;final Predicate<ServerPlayerEntity> cost;
        MachineCost(ServerPlayerEntity player,UUID id,ItemStack stack,Predicate<ServerPlayerEntity> original){owner=player.func_110124_au();pokemon=id;offered=stack.func_77946_l();cost=original;}
        boolean valid(ServerPlayerEntity player){
            if(!owner.equals(player.func_110124_au())||BattleRegistry.getBattle(player)!=null)return false;
            var mon=StorageProxy.getParty(player).get(pokemon);
            if(mon==null||!DisposableTeraDisc.compatible(mon)||mon.getMoveset().hasAttack(DisposableTeraDisc.move().getAttack()))return false;
            try(PokeInventory inventory=new PokeInventory(player)){
                for(ItemStack stack:inventory.stacks)if(!stack.func_190926_b()&&stack.func_77973_b()==offered.func_77973_b()
                    &&(stack.func_77973_b() instanceof DisposableTeraDisc||ItemStack.func_77970_a(stack,offered)))return true;
            }
            return false;
        }
        public boolean test(ServerPlayerEntity player){return valid(player)&&(cost==null||cost.test(player));}
    }
    public static void sendMachine(ServerPlayerEntity player,UUID id,ImmutableAttack attack,Predicate<ServerPlayerEntity> cost,ItemStack offered){
        if(!isTera(attack)){if(cost==null)LearnMoveController.sendLearnMove(player,id,attack);else LearnMoveController.sendLearnMove(player,id,attack,cost);return;}
        if(!(offered.func_77973_b() instanceof TechnicalMoveItem))return;
        if(!(offered.func_77973_b() instanceof DisposableTeraDisc)){
            var machine=TechnicalMoveItem.getMove(offered);if(machine==null||!isTera(machine.getAttack()))return;
        }
        // Only supersede stale requests for this exact Pokemon and move.
        LearnMoveControllerAccess.backport$requests().get(player.func_110124_au()).removeIf(value->{var request=(LearnMoveRequestAccess)value;return id.equals(request.backport$pokemon())&&isTera(request.backport$move());});
        LearnMoveController.sendLearnMove(player,id,attack,new MachineCost(player,id,offered,cost));
    }
    public static boolean postCheck(IEventBus bus,Event event,ServerPlayerEntity player,UUID id,ImmutableAttack attack){
        UUID before=MACHINE_CHECK.get();boolean allowed=false;
        if(isTera(attack))for(Object value:LearnMoveControllerAccess.backport$requests().get(player.func_110124_au())){
            var request=(LearnMoveRequestAccess)value;
            if(id.equals(request.backport$pokemon())&&attack.equals(request.backport$move())){allowed=request.backport$condition() instanceof MachineCost&&((MachineCost)request.backport$condition()).valid(player);break;}
        }
        try{if(allowed)MACHINE_CHECK.set(player.func_110124_au());else MACHINE_CHECK.remove();return bus.post(event);}
        finally{if(before==null)MACHINE_CHECK.remove();else MACHINE_CHECK.set(before);}
    }
    @SubscribeEvent public void available(TeachMoveEvent.CanLearnMove event){if(isTera(event.getAttack())&&(event.getPlayer()==null||!event.getPlayer().func_110124_au().equals(MACHINE_CHECK.get())))event.setCanceled(true);}
    @SubscribeEvent public void teach(TeachMoveEvent.MoveLearnt.Pre event){if(isTera(event.getAttack()))event.setCanceled(true);}
}
