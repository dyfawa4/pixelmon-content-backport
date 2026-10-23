package local.pixelmon.backport;
import java.util.*;
import java.math.BigDecimal;
import com.google.gson.*;
import com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.PokeBallRegistry;
import com.pixelmonmod.pixelmon.items.PokeBallItem;
import net.minecraft.item.*;
import net.minecraft.enchantment.*;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import com.pixelmonmod.pixelmon.entities.npcs.NPCTrainer;
import com.pixelmonmod.pixelmon.entities.pixelmon.drops.*;
import com.pixelmonmod.pixelmon.comm.packetHandlers.itemDrops.*;
import com.pixelmonmod.pixelmon.api.util.helpers.NetworkHelper;
import net.minecraftforge.registries.ForgeRegistries;

public final class AuthorBossRewards {
    public static final String ASH_STONE_FIRST="BackportAuthorAshStoneFirstVictory";
    public static final JsonObject DATA=Content.read("backport/author_boss_rewards.json");
    public static final JsonArray POOL=DATA.getAsJsonArray("entries");
    private AuthorBossRewards(){}
    public static ItemStack stack(JsonObject entry){
        ItemStack stack;
        if(entry.has("ball"))stack=PokeBallItem.of(PokeBallRegistry.getPokeBall(entry.get("ball").getAsString()).getValueUnsafe(),entry.get("count").getAsInt());
        else {
            ResourceLocation id=new ResourceLocation(entry.get("item").getAsString());
            Item item=ForgeRegistries.ITEMS.getValue(id);
            if(item==null||item==Items.field_190931_a)return ItemStack.field_190927_a;
            stack=new ItemStack(item,entry.get("count").getAsInt());
        }
        if(entry.has("enchantment")){
            var enchantment=ForgeRegistries.ENCHANTMENTS.getValue(new ResourceLocation(entry.get("enchantment").getAsString()));
            if(enchantment==null)throw new IllegalStateException("Missing boss prize enchantment");
            EnchantedBookItem.func_92115_a(stack,new EnchantmentData(enchantment,entry.get("enchantment_level").getAsInt()));
        }
        return stack;
    }
    public static List<JsonObject> available(){
        List<JsonObject> list=new ArrayList<>();for(var value:POOL){JsonObject entry=value.getAsJsonObject();if(!stack(entry).func_190926_b())list.add(entry);}return list;
    }
    /** Caller persists its one-shot victory marker before delivery. */
    private static List<ItemStack> prizes(ServerPlayerEntity player){
        Random random=player.func_70681_au();List<JsonObject> pool=available();
        List<ItemStack> prizes=new ArrayList<>();prizes.add(PokeBallItem.of(PokeBallRegistry.getPokeBall("master_ball").getValueUnsafe(),1));
        for(int i:AuthorBossPolicy.draw(pool.size(),DATA.get("extra_draws").getAsInt(),random))prizes.add(stack(pool.get(i)));
        if(ashStone(player,random))prizes.add(new ItemStack(Backport.item("ash_greninjite")));
        return prizes;
    }
    public static boolean rareStone(Random random){return random.nextInt(100)==0;}
    public static boolean ashStone(ServerPlayerEntity player,Random random){
        var root=player.getPersistentData();var saved=root.func_74775_l("PlayerPersisted");
        boolean first=!saved.func_74767_n(ASH_STONE_FIRST);
        if(first){saved.func_74757_a(ASH_STONE_FIRST,true);root.func_218657_a("PlayerPersisted",saved);return true;}
        return rareStone(random);
    }
    private static void money(ServerPlayerEntity player){
        int money=AuthorBossPolicy.money(player.func_70681_au());
        com.pixelmonmod.pixelmon.api.economy.BankAccountProxy.getBankAccount(player).orElseThrow().add(BigDecimal.valueOf(money));
        player.func_146105_b(new StringTextComponent("挑战胜利！获得大师球×1、"+money+"宝可梦币及五份不重复战利品。"),false);
    }
    public static void trainer(ServerPlayerEntity player,NPCTrainer npc){
        List<ItemStack> prizes=prizes(player);money(player);
        ArrayList<DroppedItem> drops=new ArrayList<>();for(ItemStack prize:prizes)drops.add(new DroppedItem(prize,drops.size()));
        // Use the native Boss screen and its existing server-owned claim/drop/full-bag handling.
        // Do not call the trainer register overload: it clears other players' pending queries.
        // Native single-item claims clear every query owned by that player when
        // one list empties. Settle only their previous query before reusing IDs.
        if(DropItemQueryList.queryList.stream().anyMatch(q->q.playerUUID.equals(player.func_110124_au())))DropItemQueryList.takeAllItems(player);
        DropItemQueryList.queryList.add(new DropItemQuery(npc.func_213303_ch(),player.func_110124_au(),drops));
        BossPickupSound.register(player,true);
        NetworkHelper.sendPacket(new ItemDropPacket(ItemDropMode.Boss,new TranslationTextComponent("pixelmon.backport.author.rewards.title"),drops),player);
    }
}
