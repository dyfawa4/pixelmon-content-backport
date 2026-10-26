package local.pixelmon.backport;
import java.util.*;
import java.math.BigDecimal;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.*;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.vector.Vector3d;
import com.pixelmonmod.pixelmon.entities.npcs.NPCTrainer;
import com.pixelmonmod.pixelmon.entities.pixelmon.drops.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
/** Journal the real victory before native cleanup; deliver after its battle-end packets. */
@Mod.EventBusSubscriber(modid=Content.NS)
public final class AuthorSettlement {
    public static final String PENDING="BackportAuthorPendingRewards";
    private static final Map<DropItemQuery,String> TRACKED=new IdentityHashMap<>();
    private static final ThreadLocal<Deque<Set<DropItemQuery>>> CLAIMING=ThreadLocal.withInitial(ArrayDeque::new);
    private AuthorSettlement(){}
    private static CompoundNBT store(ServerPlayerEntity player){var data=player.getPersistentData();var saved=data.func_74775_l("PlayerPersisted");data.func_218657_a("PlayerPersisted",saved);return saved;}
    public static CompoundNBT pending(ServerPlayerEntity player){return store(player).func_74775_l(PENDING);}
    public static boolean queue(ServerPlayerEntity player,NPCTrainer npc){
        if(AuthorBossArenas.won(npc,player.func_110124_au()))return false;
        var root=store(player);var pending=root.func_74775_l(PENDING);String key=npc.func_110124_au().toString();
        if(!pending.func_74764_b(key)){
            var grant=new CompoundNBT();var items=new ListNBT();for(var stack:AuthorBossRewards.prizes(player))items.add(stack.func_77955_b(new CompoundNBT()));grant.func_218657_a("items",items);
            grant.func_74768_a("money",AuthorBossPolicy.money(player.func_70681_au()));var pos=npc.func_213303_ch();grant.func_74780_a("x",pos.field_72450_a);grant.func_74780_a("y",pos.field_72448_b);grant.func_74780_a("z",pos.field_72449_c);
            pending.func_218657_a(key,grant);root.func_218657_a(PENDING,pending);
        }
        var winners=npc.getPersistentData().func_74775_l("BackportAuthorWinners");winners.func_74757_a(player.func_110124_au().toString(),true);npc.getPersistentData().func_218657_a("BackportAuthorWinners",winners);
        AuthorBossArenas.open(npc);return true;
    }
    public static void deliver(ServerPlayerEntity player){
        var battle=com.pixelmonmod.pixelmon.battles.BattleRegistry.getBattle(player);if(battle!=null&&!battle.battleEnded)return;
        var root=store(player);var pending=root.func_74775_l(PENDING);
        for(String key:new ArrayList<>(pending.func_150296_c())){
            if(!pending.func_74764_b(key))continue;
            var grant=pending.func_74775_l(key);var bank=com.pixelmonmod.pixelmon.api.economy.BankAccountProxy.getBankAccount(player);
            if(!grant.func_74767_n("credited")&&bank.isPresent()){bank.get().add(BigDecimal.valueOf(grant.func_74762_e("money")));grant.func_74757_a("credited",true);}
            if(!grant.func_74767_n("counted")){AuthorVictory.earned(player);grant.func_74757_a("counted",true);}
            var items=grant.func_150295_c("items",10);var prizes=new ArrayList<ItemStack>();for(int i=0;i<items.size();i++)prizes.add(ItemStack.func_199557_a(items.func_150305_b(i)));
            if(prizes.isEmpty()){if(grant.func_74767_n("credited"))pending.func_82580_o(key);continue;}
            if(TRACKED.entrySet().stream().anyMatch(e->e.getKey().playerUUID.equals(player.func_110124_au())&&e.getValue().equals(key)&&DropItemQueryList.queryList.contains(e.getKey())))continue;
            var query=AuthorBossRewards.deliver(player,player.func_213303_ch(),prizes);TRACKED.put(query,key);
            player.func_146105_b(new net.minecraft.util.text.StringTextComponent("挑战胜利！战利品请在奖励界面领取。"),false);
        }
        if(pending.func_150296_c().isEmpty())root.func_82580_o(PENDING);else root.func_218657_a(PENDING,pending);
    }
    /** Native successful claims update the remaining journal before the next player save. */
    public static void claiming(ServerPlayerEntity player){var queries=Collections.newSetFromMap(new IdentityHashMap<DropItemQuery,Boolean>());for(var query:DropItemQueryList.queryList)if(query.playerUUID.equals(player.func_110124_au()))queries.add(query);CLAIMING.get().push(queries);}
    public static void claimed(ServerPlayerEntity player,boolean all){
        var scopes=CLAIMING.get();if(scopes.isEmpty())return;var claimed=scopes.pop();if(scopes.isEmpty())CLAIMING.remove();
        if(claimed.stream().noneMatch(TRACKED::containsKey))return;
        var root=store(player);var pending=root.func_74775_l(PENDING);
        for(var entry:new ArrayList<>(TRACKED.entrySet())){
            var query=entry.getKey();if(!query.playerUUID.equals(player.func_110124_au())||!claimed.contains(query))continue;
            String key=entry.getValue();if(!pending.func_74764_b(key)){TRACKED.remove(query);continue;}
            var grant=pending.func_74775_l(key);var left=new ListNBT();
            if(!all)for(var drop:query.droppedItemList)if(drop.item!=null&&!drop.item.func_190926_b())left.add(drop.item.func_77955_b(new CompoundNBT()));
            grant.func_218657_a("items",left);
            if(left.isEmpty()){TRACKED.remove(query);if(grant.func_74767_n("credited")&&grant.func_74767_n("counted"))pending.func_82580_o(key);}
        }
        if(pending.func_150296_c().isEmpty())root.func_82580_o(PENDING);else root.func_218657_a(PENDING,pending);
    }
    public static void disconnect(ServerPlayerEntity player){
        for(var entry:new ArrayList<>(TRACKED.entrySet()))if(entry.getKey().playerUUID.equals(player.func_110124_au())){DropItemQueryList.queryList.remove(entry.getKey());TRACKED.remove(entry.getKey());}
    }
    @SubscribeEvent public static void logout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event){if(event.getPlayer() instanceof ServerPlayerEntity)disconnect((ServerPlayerEntity)event.getPlayer());}
    @SubscribeEvent public static void login(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event){if(event.getPlayer() instanceof ServerPlayerEntity)disconnect((ServerPlayerEntity)event.getPlayer());}
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;var server=net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer();if(server==null||server.func_71259_af()%10!=0)return;
        for(var player:server.func_184103_al().func_181057_v())if(player.getPersistentData().func_74775_l("PlayerPersisted").func_74764_b(PENDING)){
            try{deliver(player);}catch(RuntimeException error){if(server.func_71259_af()%200==0)org.apache.logging.log4j.LogManager.getLogger("AuthorSettlement").error("Author victory reward remains pending for {}",player.func_110124_au(),error);}
        }
    }
}
