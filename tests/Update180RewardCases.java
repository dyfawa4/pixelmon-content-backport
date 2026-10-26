package qa;
import java.util.*;
import local.pixelmon.backport.*;
import net.minecraft.item.*;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import com.pixelmonmod.pixelmon.blocks.PokeChestBlock;
import com.pixelmonmod.pixelmon.entities.npcs.registry.DropItemRegistry;
import com.pixelmonmod.pixelmon.api.util.helpers.RandomHelper;
/** Exact discrete gates plus transformed native chest methods and actual reward stacks. */
public final class Update180RewardCases {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static String key(ItemStack stack){return stack.func_77973_b().getRegistryName()+"|"+stack.func_77978_p();}
    static final class Gate extends Random {
        final int gate;boolean first=true;Gate(int gate){this.gate=gate;}
        @Override public int nextInt(int bound){if(first){first=false;check(bound==100,"First roll is exact percentage gate");return gate;}return super.nextInt(bound);}
    }
    public static void run(ServerPlayerEntity player){try{
        int chestHits=0,authorHits=0;
        for(int gate=0;gate<100;gate++){
            if(PokeChestLoot.masterBall(PokeChestLoot.tier3Drop(new Gate(gate))))chestHits++;
            if(AuthorBossRewards.masterBall(new Gate(gate)))authorHits++;
        }
        check(chestHits==1&&authorHits==50,"Exactly 1/100 wild and 50/100 author gates");
        var saved=new ArrayList<>(DropItemRegistry.tier3);
        try{
            var master=saved.stream().filter(PokeChestLoot::masterBall).findFirst().orElseThrow();
            DropItemRegistry.tier3.clear();for(int i=0;i<80;i++)DropItemRegistry.tier3.add(master);
            DropItemRegistry.tier3.add(new ItemStack(Items.field_151045_i));
            for(int gate=0;gate<100;gate++)check(PokeChestLoot.masterBall(PokeChestLoot.tier3Drop(new Gate(gate)))==(gate==0),"Wild percentage independent of duplicate Master entries");
            DropItemRegistry.tier3.clear();check(PokeChestLoot.masterBall(PokeChestLoot.tier3Drop(new Gate(0)))&&!PokeChestLoot.masterBall(PokeChestLoot.tier3Drop(new Gate(1))),"Empty pool cannot change percentage or return empty");
        }finally{DropItemRegistry.tier3.clear();DropItemRegistry.tier3.addAll(saved);}
        var visible=PokeChestBlock.class.getDeclaredMethod("getChestDrop");visible.setAccessible(true);
        var hidden=PokeChestBlock.class.getDeclaredMethod("getHiddenDrop");hidden.setAccessible(true);
        PokeChestBlock masterChest=null,ordinary=null;
        for(var block:ForgeRegistries.BLOCKS)if(block instanceof PokeChestBlock){String id=block.getRegistryName().toString();if(id.equals("pixelmon:master_chest"))masterChest=(PokeChestBlock)block;if(id.equals("pixelmon:poke_chest"))ordinary=(PokeChestBlock)block;}
        check(masterChest!=null&&ordinary!=null,"Native registered chest blocks");
        int nativeHits=0;var probe=new Random();
        for(int i=0;i<1200;i++){
            long seed=77313L+i;probe.setSeed(seed);boolean expected=probe.nextInt(100)==0;
            RandomHelper.rand.setSeed(seed);var drop=(ItemStack)visible.invoke(masterChest);
            check(PokeChestLoot.valid(drop)&&PokeChestLoot.masterBall(drop)==expected,"Transformed native visible chest exact gate");if(expected)nativeHits++;
            RandomHelper.rand.setSeed(seed);int tierRoll=probeFor(seed,100);
            if(tierRoll>=85){boolean hiddenExpected=probeForSecond(seed)==0;var hiddenDrop=(ItemStack)hidden.invoke(masterChest);check(PokeChestLoot.masterBall(hiddenDrop)==hiddenExpected,"Hidden chest keeps native tier selection then exact 1% gate");}
            RandomHelper.rand.setSeed(seed);check(!PokeChestLoot.masterBall((ItemStack)visible.invoke(ordinary)),"Ordinary visible chest remains tier1");
        }
        var pool=AuthorBossRewards.randomPool();var keys=new HashSet<String>();
        for(var row:pool){var stack=AuthorBossRewards.stack(row);check(PokeChestLoot.valid(stack)&&!PokeChestLoot.masterBall(stack)&&!DrownedBosses.exclusive(stack.func_77973_b()),"Random pool excludes independent prizes");check(keys.add(key(stack)),"No duplicate item and NBT");}
        for(String id:List.of("pixelmon:red_chain","sophisticatedbackpacks:netherite_backpack","pixelmon:soul_dew","pixelmon:l_exp_candy","pixelmon:xl_exp_candy","pixelmon:blank_plate","pixelmon:ultranecrozium_z","pixelmon:adamant_orb","pixelmon:lustrous_orb","pixelmon:griseous_orb","pixelmon:omega_shard","pixelmon:alpha_shard","pixelmon:ability_patch","pixelmon:gold_bottle_cap","pixelmon:exp_all"))check(pool.stream().anyMatch(row->AuthorBossRewards.stack(row).func_77973_b().getRegistryName().toString().equals(id)),"Original and native Ultimate/Legendary coverage: "+id);
        for(String tierId:List.of("legendary","ultimate")){
            var tier=com.pixelmonmod.pixelmon.api.pokemon.boss.BossTierRegistry.getBossTierOrNotBoss(tierId);
            List<com.pixelmonmod.pixelmon.api.pokemon.boss.DropSet> sets=BossLootCatalog.field(tier,"dropSets");
            for(var set:sets){com.pixelmonmod.pixelmon.api.util.WeightedSet<com.pixelmonmod.pixelmon.api.pokemon.boss.DropSet.Drop> choices=BossLootCatalog.field(set,"drops");for(var it=choices.iterator();it.hasNext();){var stack=it.next().getDisplayItem();if(PokeChestLoot.valid(stack)&&!PokeChestLoot.masterBall(stack)&&!stack.func_77973_b().getRegistryName().toString().equals(MysteriousPotionLoot.ITEM))check(keys.contains(key(stack)),"Every live tier item included");}}
        }
        for(int gate=0;gate<100;gate++){
            var prizes=AuthorBossRewards.randomPrizes(new Gate(gate));int masters=0;var seen=new HashSet<String>();for(var stack:prizes){if(PokeChestLoot.masterBall(stack))masters++;else check(seen.add(key(stack)),"Seven draws are unique");}
            check(seen.size()==7&&masters==(gate<50?1:0)&&prizes.size()==7+masters,"Seven random rewards plus optional single Master Ball");
        }
        check(!AuthorBossRewards.DATA.get("replacement").getAsBoolean(),"No replacement metadata");
        System.out.println("REWARD180_PASS exact gates wild=1/100 author=50/100; 1200 transformed native visible chest rolls hits="+nativeHits+"; hidden tier routing, tier1 preserved, pool-size independence, mergedPool="+pool.size()+", item/NBT uniqueness, all native tier drops covered, seven distinct draws, no extra Master leak");
    }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
    static int probeFor(long seed,int bound){return new Random(seed).nextInt(bound);}
    static int probeForSecond(long seed){var random=new Random(seed);random.nextInt(100);return random.nextInt(100);}
}
