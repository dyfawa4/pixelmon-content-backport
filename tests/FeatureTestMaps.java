import java.util.*;
import local.pixelmon.backport.*;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import com.pixelmonmod.pixelmon.enums.EnumPlate;
import com.pixelmonmod.pixelmon.battles.BattleRegistry;
import com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.PowerConstruct;
import com.pixelmonmod.pixelmon.items.ZygardeCubeItem;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Hand;
/** Interactive, opt-in fixtures. Spawning a specimen never awards a capture achievement. */
public final class FeatureTestMaps {
    static boolean isMap(ServerPlayerEntity p){return p.func_71121_q().func_73046_m().func_240793_aU_().func_76065_j().startsWith("回迁专项 ");}
    static boolean riding(ServerPlayerEntity p){return p.func_71121_q().func_73046_m().func_240793_aU_().func_76065_j().contains("骑乘");}
    static Pokemon mon(int dex){Pokemon pokemon=PokemonFactory.create(PixelmonSpecies.fromNationalDex(dex));pokemon.setLevelNum(70);pokemon.initialize();pokemon.heal();return pokemon;}
    static void tell(ServerPlayerEntity p,String line){p.func_146105_b(new net.minecraft.util.text.StringTextComponent(line),false);}
    static void tick(ServerPlayerEntity p){
        if(!p.getPersistentData().func_74767_n("BackportFeatureReady")){
            var w=p.func_71121_q();TestMaps.cmd(p,"fill -24 64 -18 24 64 18 minecraft:smooth_stone");
            TestMaps.cmd(p,"fill -24 65 -18 24 72 18 minecraft:air");TestMaps.cmd(p,"gamemode creative");TestMaps.cmd(p,"tp @s 0 65 0");
            TestMaps.block(w,-8,65,0,"pixelmon:healer");TestMaps.block(w,-10,65,0,"pixelmon:pc");
            if(riding(p)){
                TestMaps.cmd(p,"fill 7 61 -12 18 64 12 minecraft:water");TestMaps.cmd(p,"fill -18 65 4 -14 66 8 minecraft:stone_bricks");
                TestMaps.label(w,0,68,-7,"新宝可梦骑乘：放出队伍，空手右键；Shift 下马");
                TestMaps.label(w,0,67,-7,"地面道路 / 跳跃障碍 / 水域；/backportqa feature riding 切换测试队伍");action(p,"riding");
            }else{
                TestMaps.label(w,0,68,-7,"捕捉成就必须实际捕捉；生成、赠送、合体均不替代捕捉");
                TestMaps.label(w,0,67,-7,"/backportqa feature capture_next 或 fusion_next 切换场景");
                TestMaps.supply(w,-4,0,"捕捉测试用品",Arrays.asList(DedicatedTestMaps.stack("pixelmon:master_ball",64),DedicatedTestMaps.stack("pixelmon:ultra_ball",64)));
                TestMaps.supply(w,-4,4,"基格尔德与天界之笛复测",Arrays.asList(DedicatedTestMaps.stack("pixelmon:zygarde_cube",1),DedicatedTestMaps.stack("pixelmon:arc_chalice",1)));
                for(var plate:EnumPlate.values())TestMaps.supply(w,-16+plate.ordinal()%9*3,10+plate.ordinal()/9*3,"石板",Collections.singletonList(new ItemStack(plate.getItem(),1)));
                action(p,"capture_next");tell(p,"合体场景：放出第一只，手持对应道具右键。基格尔德复测：/backportqa feature zygarde。");
            }
            p.getPersistentData().func_74757_a("BackportFeatureReady",true);
        }
        if(p.getPersistentData().func_74762_e("BackportFeatureLayoutVersion")<4)FeatureStations.setup(p);
    }
    static int action(ServerPlayerEntity p,String action){
        if(!isMap(p))return 0;
        if(action.equals("mega")){FeatureStations.mega(p);return 1;}
        if(action.equals("eternamax")){FeatureStations.eternamax(p);return 1;}
        if(action.equals("riding")){
            var party=StorageProxy.getParty(p);int[] dexes={1020,1021,1022,1023,967,901};
            for(int i=0;i<6;i++)party.set(i,mon(dexes[i]));p.func_184611_a(Hand.MAIN_HAND,ItemStack.field_190927_a);
            tell(p,"已提供破空焰、猛雷鼓、铁磐岩、铁头壳及摩托蜥、月月熊。请放出后空手右键骑乘。");
        }else if(action.equals("capture_next")){
            var data=p.getPersistentData();
            if(data.func_186855_b("BackportCaptureFixtureUUID")){
                var prior=p.func_71121_q().func_217461_a(data.func_186857_a("BackportCaptureFixtureUUID"));
                if(prior instanceof PixelmonEntity && ((PixelmonEntity)prior).getPokemon().getOwnerPlayerUUID()==null
                    && prior.getPersistentData().func_74767_n("BackportCaptureFixture"))prior.func_70106_y();
            }
            List<Integer> entries=new ArrayList<>();LegendAdvancements.DATA.getAsJsonObject("captures").entrySet().forEach(s->entries.add(Integer.parseInt(s.getKey())));Collections.sort(entries);
            int index=p.getPersistentData().func_74762_e("BackportCaptureIndex")%entries.size();Pokemon pokemon=mon(entries.get(index));
            PixelmonEntity entity=pokemon.getOrCreatePixelmon(p.field_70170_p,4,65,0);entity.getPersistentData().func_74757_a("BackportCaptureFixture",true);p.field_70170_p.func_217376_c(entity);
            data.func_186854_a("BackportCaptureFixtureUUID",entity.func_110124_au());
            p.getPersistentData().func_74768_a("BackportCaptureIndex",index+1);tell(p,"捕捉场景 "+(index+1)+"/"+entries.size()+"："+pokemon.getLocalizedName()+"。实际捕捉后查看传说续章页。");
        }else if(action.equals("fusion_next")){
            int[][] pairs={{646,644},{646,643},{800,791},{800,792},{898,896},{898,897}};
            String[] items={"dna_splicers","dna_splicers","n_solarizer","n_lunarizer","reins_of_unity","reins_of_unity"};
            int index=p.getPersistentData().func_74762_e("BackportFusionIndex")%pairs.length;var party=StorageProxy.getParty(p);
            for(int i=0;i<6;i++)party.set(i,null);party.set(0,mon(pairs[index][0]));party.set(1,mon(pairs[index][1]));
            p.func_184611_a(Hand.MAIN_HAND,DedicatedTestMaps.stack("pixelmon:"+items[index],1));p.getPersistentData().func_74768_a("BackportFusionIndex",index+1);
            tell(p,"合体场景 "+(index+1)+"/6：放出第一只，右键使用手中道具；再次使用可解除合体。");
        }else if(action.equals("zygarde")){
            FeatureStations.zygarde(p);
        }else if(action.equals("menu")){FeatureStations.menu(p,"all");
        }else if(action.equals("flute")){FeatureStations.flute(p);
        }else if(action.equals("end")){var battle=BattleRegistry.getBattle(p);if(battle!=null)battle.endBattleWithoutXP();}
        else return 0;
        return 1;
    }
}
