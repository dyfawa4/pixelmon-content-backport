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
    private static volatile List<String> tierItems=Collections.emptyList();
    private AuthorBossRewards(){}
    /** Snapshot before the existing Ultimate-to-Legendary override loses its original items. */
    public static void captureTierPools(Map<ResourceLocation,JsonElement> data){
        var items=new ArrayList<String>();
        for(var value:data.values())if(value.isJsonObject()){
            var tier=value.getAsJsonObject();
            if(!tier.has("id")||!Set.of("legendary","ultimate").contains(tier.get("id").getAsString())||!tier.has("dropSets"))continue;
            for(var set:tier.getAsJsonArray("dropSets"))for(var drop:set.getAsJsonObject().getAsJsonArray("drops")){
                var row=drop.getAsJsonObject();if(row.has("item"))items.add(row.get("item").getAsString());
            }
        }
        tierItems=List.copyOf(items);
    }
    public static ItemStack stack(JsonObject entry){
        ItemStack stack;
        if(entry.has("stack")){
            try{return ItemStack.func_199557_a(net.minecraft.nbt.JsonToNBT.func_180713_a(entry.get("stack").getAsString()));}
            catch(com.mojang.brigadier.exceptions.CommandSyntaxException e){throw new IllegalStateException("Invalid author reward NBT",e);}
        }
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
    private static void addUnique(List<JsonObject> pool,JsonObject entry){
        var candidate=stack(entry);if(!PokeChestLoot.valid(candidate)||prizeBall(candidate)||DrownedBosses.exclusive(candidate.func_77973_b())||new ResourceLocation(MysteriousPotionLoot.ITEM).equals(candidate.func_77973_b().getRegistryName()))return;
        for(var row:pool){var previous=stack(row);if(previous.func_77973_b()==candidate.func_77973_b()&&Objects.equals(previous.func_77978_p(),candidate.func_77978_p()))return;}
        pool.add(entry);
    }
    public static List<JsonObject> randomPool(){
        var pool=new ArrayList<JsonObject>();for(var entry:available())addUnique(pool,entry);
        for(var item:tierItems){var entry=new JsonObject();if(item.startsWith("{"))entry.addProperty("stack",item);else{entry.addProperty("item",item);entry.addProperty("count",1);}addUnique(pool,entry);}
        for(String id:List.of("legendary","ultimate")){
            var tier=com.pixelmonmod.pixelmon.api.pokemon.boss.BossTierRegistry.getBossTierOrNotBoss(id);
            List<com.pixelmonmod.pixelmon.api.pokemon.boss.DropSet> sets=BossLootCatalog.field(tier,"dropSets");
            for(var set:sets){com.pixelmonmod.pixelmon.api.util.WeightedSet<com.pixelmonmod.pixelmon.api.pokemon.boss.DropSet.Drop> drops=BossLootCatalog.field(set,"drops");
                for(var it=drops.iterator();it.hasNext();){var stack=it.next().getDisplayItem();if(!PokeChestLoot.valid(stack))continue;var entry=new JsonObject();entry.addProperty("stack",stack.func_77955_b(new net.minecraft.nbt.CompoundNBT()).toString());addUnique(pool,entry);}
            }
        }
        return pool;
    }
    public static String ballName(int roll){if(roll<0||roll>=100)throw new IllegalArgumentException("Percentage roll");return roll<15?SpectralBallLogic.NAME:roll<20?RadiantBallLogic.NAME:roll<50?"master_ball":"gs_ball";}
    /** The JEI catalogue derives percentages from the actual roll, never a stale second table. */
    public static Map<String,Integer> ballChances(){var chances=new LinkedHashMap<String,Integer>();for(int roll=0;roll<100;roll++)chances.merge(ballName(roll),1,Integer::sum);return chances;}
    public static boolean prizeBall(ItemStack stack){return PokeChestLoot.masterBall(stack)||com.pixelmonmod.pixelmon.items.PokeBallPart.getPokeBall(stack).map(ball->Set.of("master_ball","gs_ball",RadiantBallLogic.NAME,SpectralBallLogic.NAME).contains(ball.getName())).orElse(false);}
    public static boolean masterBall(Random random){return ballName(random.nextInt(100)).equals("master_ball");}
    public static List<ItemStack> randomPrizes(Random random){
        var pool=randomPool();var prizes=new ArrayList<ItemStack>();
        prizes.add(PokeBallItem.of(PokeBallRegistry.getPokeBall(ballName(random.nextInt(100))).getValueUnsafe(),1));
        for(int i:AuthorBossPolicy.draw(pool.size(),DATA.get("extra_draws").getAsInt(),random))prizes.add(stack(pool.get(i)));
        return prizes;
    }
    /** Caller persists its one-shot victory marker before delivery. */
    static List<ItemStack> prizes(ServerPlayerEntity player){
        Random random=player.func_70681_au();List<ItemStack> prizes=randomPrizes(random);
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
    private static void money(ServerPlayerEntity player,List<ItemStack> prizes){
        int money=AuthorBossPolicy.money(player.func_70681_au());
        com.pixelmonmod.pixelmon.api.economy.BankAccountProxy.getBankAccount(player).orElseThrow().add(BigDecimal.valueOf(money));
        player.func_146105_b(new StringTextComponent("挑战胜利！获得"+(prizes.stream().anyMatch(PokeChestLoot::masterBall)?"大师球×1、":"")+money+"宝可梦币及七份不重复战利品。"),false);
    }
    public static void trainer(ServerPlayerEntity player,NPCTrainer npc){
        List<ItemStack> prizes=prizes(player);money(player,prizes);
        deliver(player,npc.func_213303_ch(),prizes);
    }
    public static DropItemQuery deliver(ServerPlayerEntity player,net.minecraft.util.math.vector.Vector3d position,List<ItemStack> prizes){
        ArrayList<DroppedItem> drops=new ArrayList<>();for(ItemStack prize:prizes)drops.add(new DroppedItem(prize,drops.size()));
        // Use the native Boss screen and its existing server-owned claim/drop/full-bag handling.
        // Do not call the trainer register overload: it clears other players' pending queries.
        // Native single-item claims clear every query owned by that player when
        // one list empties. Settle only their previous query before reusing IDs.
        if(DropItemQueryList.queryList.stream().anyMatch(q->q.playerUUID.equals(player.func_110124_au())))DropItemQueryList.takeAllItems(player);
        var query=new DropItemQuery(position,player.func_110124_au(),drops);DropItemQueryList.queryList.add(query);
        BossPickupSound.register(player,true);
        NetworkHelper.sendPacket(new ItemDropPacket(ItemDropMode.Boss,new TranslationTextComponent("pixelmon.backport.author.rewards.title"),drops),player);
        return query;
    }
}
