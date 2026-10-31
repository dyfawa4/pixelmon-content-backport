package qa;
import java.util.*;
import java.lang.reflect.*;
import com.pixelmonmod.pixelmon.items.FishingRodItem;
import com.pixelmonmod.pixelmon.entities.projectiles.HookEntity;
import com.pixelmonmod.pixelmon.enums.items.EnumRodType;
import com.pixelmonmod.pixelmon.api.spawning.*;
import com.pixelmonmod.pixelmon.api.spawning.conditions.*;
import com.pixelmonmod.pixelmon.api.spawning.archetypes.entities.pokemon.SpawnInfoPokemon;
import com.pixelmonmod.pixelmon.api.world.MutableLocation;
import com.pixelmonmod.pixelmon.spawning.PixelmonSpawning;
import local.pixelmon.backport.*;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.*;
import net.minecraft.enchantment.*;
import net.minecraft.util.*;
import net.minecraft.inventory.container.*;
import net.minecraftforge.registries.ForgeRegistries;
public final class Update180FishingCases {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static Enchantment ench(String id){return ForgeRegistries.ENCHANTMENTS.getValue(new ResourceLocation("minecraft",id));}
    static String table(ItemStack stack,int seed){var out=new ArrayList<String>();for(var value:EnchantmentHelper.func_77513_b(new Random(seed),stack,30,false))out.add(value.field_76302_b.getRegistryName()+":"+value.field_76303_c);return out.toString();}
    public static void run(ServerPlayerEntity p)throws Exception{
        var vanilla=new ItemStack(Items.field_151112_aM);int rods=0,table=0;
        for(var item:ForgeRegistries.ITEMS)if(item instanceof FishingRodItem){
            rods++;var rod=new ItemStack(item);check(rod.func_77956_u()&&item.func_77619_b()==vanilla.func_77973_b().func_77619_b(),"Vanilla enchanting value: "+item.getRegistryName());
            for(var enchantment:ForgeRegistries.ENCHANTMENTS)check(enchantment.func_92089_a(rod)==(enchantment.func_92089_a(vanilla)||(FishingEnchantments.supported(rod)&&FishingEnchantments.bookOnly(enchantment))),"Native applicability with 1.10 book-only rod exceptions "+enchantment.getRegistryName()+" / "+item.getRegistryName());
            for(int seed=0;seed<100;seed++){check(table(rod,seed).equals(table(vanilla,seed)),"Native enchanting table roll parity "+item.getRegistryName());table++;}
            for(String id:List.of("lure","luck_of_the_sea","unbreaking","mending","vanishing_curse")){
                var container=new RepairContainer(31,p.field_71071_by);var book=new ItemStack(Items.field_151134_bR);EnchantedBookItem.func_92115_a(book,new EnchantmentData(ench(id),1));container.func_75139_a(0).func_75215_d(rod.func_77946_l());container.func_75139_a(1).func_75215_d(book);container.func_82848_d();var result=container.func_75139_a(2).func_75211_c();check(!result.func_190926_b()&&EnchantmentHelper.func_77506_a(ench(id),result)==1,"Native anvil book application "+id+" / "+item.getRegistryName());
            }
            rod.func_77966_a(ench("lure"),3);rod.func_77966_a(ench("mending"),1);rod.func_77966_a(ench("vanishing_curse"),1);rod.func_196085_b(30);rod.func_196082_o().func_74768_a("rodQuality",5);
            var grind=new GrindstoneContainer(32,p.field_71071_by);grind.func_75139_a(0).func_75215_d(rod);var cleaned=grind.func_75139_a(2).func_75211_c();check(!cleaned.func_190926_b()&&EnchantmentHelper.func_77506_a(ench("lure"),cleaned)==0&&EnchantmentHelper.func_77506_a(ench("mending"),cleaned)==0&&EnchantmentHelper.func_77506_a(ench("vanishing_curse"),cleaned)==1&&cleaned.func_77978_p().func_74762_e("rodQuality")==5,"Native grindstone strips ordinary enchantments, preserves curse and rod quality");
        }
        check(rods>=4,"All native Pixelmon rods covered");
        var stack=Update180Cases.stack("pixelmon:super_rod");p.func_184611_a(Hand.MAIN_HAND,stack);p.func_184611_a(Hand.OFF_HAND,ItemStack.field_190927_a);
        var hook=new HookEntity(p.field_70170_p,p,EnumRodType.SuperRod,-1);hook.func_70012_b(p.func_226277_ct_()+1,p.func_226278_cu_(),p.func_226281_cx_(),0,0);p.field_71104_cf=hook;
        var timer=HookEntity.class.getDeclaredField("ticksTillCatch");timer.setAccessible(true);
        for(int level=0;level<=3;level++){
            stack=Update180Cases.stack("pixelmon:super_rod");if(level>0)stack.func_77966_a(ench("lure"),level);p.func_184611_a(Hand.MAIN_HAND,stack);hook.setNewTimeToCatch();int ticks=timer.getInt(hook);check(ticks>=FishingEnchantments.waitTicks(100,level)&&ticks<=FishingEnchantments.waitTicks(300,level),"Actual cast event Lure level "+level+" wait="+ticks);
        }
        var rare=new SpawnInfoPokemon();rare.rarity=4;rare.condition=new SpawnCondition();var common=new SpawnInfoPokemon();common.rarity=100;common.condition=new SpawnCondition();
        var location=new SpawnLocation(p,new MutableLocation(p.field_70170_p,p.func_233580_cy_()),new HashSet<>(),net.minecraft.block.Blocks.field_150355_j,new HashSet<>(),p.field_70170_p.func_226691_t_(p.func_233580_cy_()),true,15,12);
        var spawner=PixelmonSpawning.fishingSpawner;check(spawner!=null,"Native fishing spawner loaded");var candidates=List.<SpawnInfo>of(common,rare);int[] samples=new int[2];float nominal=rare.calculateNominalRarity();check(nominal<5,"Native triple mark classification");
        for(int pass=0;pass<2;pass++){
            stack=Update180Cases.stack("pixelmon:super_rod");if(pass==1)stack.func_77966_a(ench("luck_of_the_sea"),3);p.func_184611_a(Hand.MAIN_HAND,stack);
            for(int draw=0;draw<6000;draw++)if(spawner.selectionAlgorithm.choose(spawner,location,candidates)==rare)samples[pass]++;
        }
        check(samples[0]>0&&samples[1]>samples[0]*1.5,"Actual native weighted selection increases real triple-mark catches: "+Arrays.toString(samples));check(rare.calculateNominalRarity()==nominal,"Luck does not relabel ordinary catches as rare");
        check(FishingEnchantments.rarity(rare,new AbstractSpawner("qa-non-fishing"){},location,4)==4,"Nonfishing rarity unchanged");
        hook.func_70106_y();p.field_71104_cf=null;
        var mending=Update180Cases.stack("pixelmon:super_rod");mending.func_77966_a(ench("mending"),1);mending.func_196085_b(30);p.func_184611_a(Hand.MAIN_HAND,mending);p.field_71090_bL=0;var orb=new net.minecraft.entity.item.ExperienceOrbEntity(p.field_70170_p,p.func_226277_ct_(),p.func_226278_cu_(),p.func_226281_cx_(),5);orb.func_70100_b_(p);check(mending.func_77952_i()<30,"Real XP orb repairs held rod");
        System.out.println("FISHING_ENCHANT180_PASS "+rods+" rods; "+table+" actual enchanting-table parity rolls; native anvil/grindstone and all enchant applicability including binding/vanishing parity; native Lure casts; actual !!! selections "+Arrays.toString(samples)+"; real Mending XP");
    }
}
