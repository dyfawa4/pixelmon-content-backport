import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import com.google.common.collect.ImmutableList;
import com.pixelmonmod.pixelmon.api.drops.CustomDropScreenFactory;
import com.pixelmonmod.pixelmon.api.events.PokeLootEvent;
import com.pixelmonmod.pixelmon.api.events.drops.CustomDropsEvent;
import com.pixelmonmod.pixelmon.api.pokemon.boss.*;
import com.pixelmonmod.pixelmon.api.pokemon.drops.ItemWithChance;
import com.pixelmonmod.pixelmon.api.util.WeightedSet;
import com.pixelmonmod.pixelmon.battles.raids.WeightedItemStacks;
import com.pixelmonmod.pixelmon.blocks.enums.EnumPokeChestType;
import com.pixelmonmod.pixelmon.blocks.tileentity.PokeChestTileEntity;
import com.pixelmonmod.pixelmon.entities.npcs.NPCTrainer;
import com.pixelmonmod.pixelmon.entities.npcs.registry.DropItemRegistry;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import com.pixelmonmod.pixelmon.entities.pixelmon.drops.*;
import local.pixelmon.backport.*;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import io.netty.buffer.Unpooled;

/** Opt-in integration cases against native transformed classes, isolated save only. */
@Mod.EventBusSubscriber(modid="backport_tera_visual_test")
public final class RewardGameTest {
    static final Path RESULT=Path.of("reward-game-result.json");static boolean finished;static final JsonArray cases=new JsonArray();
    static boolean enabled(){return "backport-tera-visuals-145".equals(System.getProperty("backport.manual.world"))&&Files.isRegularFile(Path.of("reward-private-test"));}
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static void pass(String name){cases.add(name);System.out.println("REWARD_GAME_CASE_PASS "+name);}
    static int count(ServerPlayerEntity p,Item item){int count=0;for(ItemStack stack:p.field_71071_by.field_70462_a)if(stack.func_77973_b()==item)count+=stack.func_190916_E();return count;}
    static void run(ServerPlayerEntity p)throws Exception{
        Item emerald=ForgeRegistries.ITEMS.getValue(new ResourceLocation("minecraft","emerald"));
        check(Items.field_151121_aF==ForgeRegistries.ITEMS.getValue(new ResourceLocation("minecraft","paper")),"Wrong command icon item mapping");
        ItemStack valid=new ItemStack(Items.field_151045_i,5);CompoundNBT tag=new CompoundNBT();tag.func_74778_a("RewardFixture","keep-nbt");valid.func_77982_d(tag);
        ItemStack zero=new ItemStack(Items.field_151045_i,0);
        List<ItemStack> source=Arrays.asList(null,ItemStack.field_190927_a,new ItemStack(Items.field_190931_a),zero,valid);
        var cleaned=RewardSanitizer.stacks(source);check(cleaned.size()==1&&cleaned.get(0)==valid&&valid.func_190916_E()==5&&valid.func_77978_p().equals(tag),"Lost quantity/NBT/identity");check(source.size()==5,"Mutated caller list");check(RewardSanitizer.stacks(cleaned)==cleaned,"Changed already-valid list");check(RewardSanitizer.stacks(Collections.emptyList()).isEmpty(),"Invented no-drop reward");pass("null-air-zero-NBT-immutable-empty");

        DropSet.Drop itemDrop=new DropSet.Drop(3,"",valid,false);
        DropSet.Drop airDrop=new DropSet.Drop(1000,"",ItemStack.field_190927_a,false);
        // The private runner profile exceeds vanilla's 16-character name parser limit.
        // Select the sole private player without changing native command substitution.
        DropSet.Drop commandDrop=new DropSet.Drop(2,"命令奖励测试",ItemStack.field_190927_a,true,"give @a minecraft:emerald 2");
        WeightedSet<DropSet.Drop> weights=WeightedSet.newWeightedSet(3,itemDrop,1000,airDrop,2,commandDrop);
        DropSet pool=new DropSet(1,1,weights);Set<DropSet.Drop> seen=new HashSet<>();
        for(int n=0;n<500;n++){var rolled=pool.getDrops();check(rolled.size()==1,"Unexpected empty boss roll");var drop=rolled.iterator().next();check(drop==itemDrop||drop==commandDrop,"Boss rolled air");seen.add(drop);}
        check(seen.size()==2,"Valid weighted reward lost");check(weights.size()==3,"Changed caller-owned weighted set");
        check(new DropSet(1,1,WeightedSet.newWeightedSet(1,airDrop)).getDrops().isEmpty(),"All-invalid boss set retained air");
        check(new DropSet(0,2,WeightedSet.newWeightedSet(1,itemDrop)).getDrops().isEmpty(),"Changed intentional zero chance");
        ItemStack icon=commandDrop.getDisplayItem();check(PokeChestLoot.valid(icon)&&icon.func_77973_b()==Items.field_151121_aF,"Command-only icon invalid");pass("boss-weighted-rolls-500-command-preserved-display-only-icon");

        WeightedItemStacks raid=WeightedItemStacks.create().add(1000,ItemStack.field_190927_a).add(1,valid).add(1,null).add(-1,valid);
        for(int n=0;n<500;n++)check(raid.get().func_190916_E()==5&&raid.get().func_77978_p().equals(tag),"Raid rolled air or lost NBT");
        check(raid.pop().func_190916_E()==5&&raid.pop().func_190926_b(),"Raid pop depleted incorrectly");
        check(new ItemWithChance(null,1).getItem().func_190926_b()&&new ItemWithChance(null,1).getItemStack().func_190926_b(),"Null legacy reward crashed");pass("raid-weighted-rolls-500-pop-empty-legacy-null");

        for(EnumPokeChestType type:EnumPokeChestType.values()){
            var chest=new PokeChestTileEntity();chest.setChestType(type);
            check(PokeChestLoot.drops(new PokeLootEvent.GetDrops(p,chest,new ItemStack[]{ItemStack.field_190927_a,valid})).length==1,"Chest air "+type);
            check(PokeChestLoot.drops(new PokeLootEvent.GetDrops(p,chest,new ItemStack[0])).length==(type==EnumPokeChestType.MASTERBALL?1:0),"Chest empty policy "+type);
            chest.setOwner(UUID.randomUUID());check(PokeChestLoot.drops(new PokeLootEvent.GetDrops(p,chest,new ItemStack[0])).length==0,"Owned chest invented reward");
            chest.setOwner(null);chest.setCustomDrops(valid);chest.setCustomDropEnabled(true);check(chest.isCustomDrop(),"Custom fixture not enabled");check(PokeChestLoot.drops(new PokeLootEvent.GetDrops(p,chest,new ItemStack[0])).length==0,"Custom chest invented reward");
        }
        for(List<ItemStack> tier:Arrays.asList(DropItemRegistry.tier1,DropItemRegistry.tier2,DropItemRegistry.tier3,DropItemRegistry.ultraSpace))check(tier.stream().allMatch(PokeChestLoot::valid),"Loaded chest tier invalid");pass("all-chest-types-owned-custom-master-fallback-loaded-tiers");

        p.field_71071_by.func_174888_l();
        for(boolean pokemon:new boolean[]{false,true}){
            var rewards=new ArrayList<DroppedItem>();rewards.add(new DroppedItem(valid.func_77946_l(),17));rewards.add(new DroppedItem(ItemStack.field_190927_a,18));rewards.add(new DroppedItem(commandDrop,29));
            if(pokemon){var mon=new PixelmonEntity(p.func_71121_q(),TeraVisualTestMap.pokemon(609,"",false));BossTier tier=BossTierRegistry.getBossTiers().stream().filter(BossTier::isBoss).findFirst().orElseThrow();mon.setBossTier(tier);DropItemQueryList.register(mon,rewards,p);}
            else DropItemQueryList.register(new NPCTrainer(p.func_71121_q()),rewards,p);
            var query=DropItemQueryList.queryList.stream().filter(q->q.playerUUID.equals(p.func_110124_au())).findFirst().orElseThrow();
            check(query.droppedItemList.size()==2&&query.droppedItemList.get(0).id==17&&query.droppedItemList.get(1).id==29,"Query removed wrong reward/ID");
            for(DroppedItem drop:query.droppedItemList){PacketBuffer packet=new PacketBuffer(Unpooled.buffer());try{drop.toBytes(packet);var decoded=DroppedItem.fromBytes(packet);check(decoded.id==drop.id&&PokeChestLoot.valid(decoded.item),"Packet ID or icon mismatch");}finally{packet.release();}}
            int diamonds=count(p,Items.field_151045_i),emeralds=count(p,emerald);
            DropItemQueryList.takeItem(p,18);check(query.droppedItemList.size()==2,"Removed reward ID claimed another item");
            DropItemQueryList.takeItem(p,17);check(count(p,Items.field_151045_i)==diamonds+5,"Specific reward wrong quantity");
            DropItemQueryList.takeAllItems(p);check(count(p,emerald)==emeralds+2&&count(p,Items.field_151121_aF)==0,"Command/icon claim: emeralds="+count(p,emerald)+" expected="+(emeralds+2)+" paper="+count(p,Items.field_151121_aF));
            check(DropItemQueryList.queryList.stream().noneMatch(q->q.playerUUID.equals(p.func_110124_au())),"Query not cleared");
        }
        pass("native-boss-trainer-query-packet-ID-single-all-claim-command-no-paper");

        var tiers=Arrays.asList(DropItemRegistry.tier1,DropItemRegistry.tier2,DropItemRegistry.tier3,DropItemRegistry.ultraSpace);
        List<List<ItemStack>> tierSnapshots=new ArrayList<>();for(var tier:tiers)tierSnapshots.add(new ArrayList<>(tier));
        Item badge=ForgeRegistries.ITEMS.getValue(new ResourceLocation("pixelmon","bolt_badge"));check(badge!=null&&badge!=Items.field_190931_a,"Gym badge fixture missing");
        var gym=new NPCTrainer(p.func_71121_q());gym.isGymLeader=true;
        var gymRewards=new ArrayList<DroppedItem>();gymRewards.add(new DroppedItem(new ItemStack(badge),41));gymRewards.add(new DroppedItem(ItemStack.field_190927_a,42));
        int badges=count(p,badge);DropItemQueryList.register(gym,gymRewards,p);
        var gymQuery=DropItemQueryList.queryList.stream().filter(q->q.playerUUID.equals(p.func_110124_au())).findFirst().orElseThrow();
        check(gymQuery.droppedItemList.size()==1&&gymQuery.droppedItemList.get(0).id==41,"Gym badge removed or remapped");DropItemQueryList.takeAllItems(p);check(count(p,badge)==badges+1,"Gym badge not granted once");
        for(int n=0;n<tiers.size();n++)check(tiers.get(n).equals(tierSnapshots.get(n)),"Gym reward leaked into chest tier");
        for(int n=0;n<100;n++)for(DropSet.Drop drop:pool.getDrops())check(drop==itemDrop||drop==commandDrop,"Gym reward leaked into boss pool");
        check(raid.get().func_190926_b(),"Gym reward leaked into raid pool");pass("gym-badge-preserved-once-no-chest-boss-raid-pool-sharing");

        var builder=CustomDropScreenFactory.builder().setTitle(new StringTextComponent("奖励测试")).setItems(source);
        builder.sendTo(p);var field=CustomDropScreenFactory.Builder.class.getDeclaredField("items");field.setAccessible(true);
        var items=(List<?>)field.get(builder);check(items.size()==1&&items.get(0)==valid,"Custom server/client indices differ");pass("custom-screen-shared-server-packet-index-list");
        var sample=new PixelmonEntity(p.func_71121_q(),TeraVisualTestMap.pokemon(609,"",false));
        for(BossTier tier:BossTierRegistry.getBossTiers())for(DropSet.Drop drop:tier.getDrops(p,sample))check(RewardSanitizer.valid(drop)&&PokeChestLoot.valid(drop.getDisplayItem()),"Loaded boss tier air "+tier.getID());pass("loaded-boss-tier-reward-sample");
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||finished||!enabled())return;var server=ServerLifecycleHooks.getCurrentServer();if(server==null||server.func_184103_al().func_181057_v().isEmpty())return;
        var p=server.func_184103_al().func_181057_v().get(0);if(p.field_70173_aa<160)return;
        JsonObject result=new JsonObject();try{run(p);result.addProperty("passed",true);}catch(Throwable error){error.printStackTrace();result.addProperty("passed",false);result.addProperty("error",error.toString());}
        result.add("cases",cases);try{Files.writeString(RESULT,new GsonBuilder().setPrettyPrinting().create().toJson(result));}catch(Exception error){error.printStackTrace();}finished=true;System.out.println("REWARD_GAME_FINISHED "+result.get("passed"));
    }
    @Mod.EventBusSubscriber(modid="backport_tera_visual_test",value=net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static class Client {static int closing;@SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){if(e.phase==TickEvent.Phase.END&&enabled()&&Files.isRegularFile(RESULT)&&++closing==60)net.minecraft.client.Minecraft.func_71410_x().func_71400_g();}}
}
