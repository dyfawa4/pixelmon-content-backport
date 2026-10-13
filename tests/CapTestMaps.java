import java.util.*;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.registries.*;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.api.battles.BattleType;
import com.pixelmonmod.pixelmon.battles.*;
import com.pixelmonmod.pixelmon.battles.api.rules.*;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import com.pixelmonmod.pixelmon.enums.*;
import local.pixelmon.backport.CapSpecies;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/** Fixtures only in the named private world; all combat begins by user action. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="backport_client_test")
public final class CapTestMaps {
    static final int[] DEX={143,59,9,26,154,471,68,89,450,821,196,127,526,200,706,197,379,700};
    static final String[] TYPES={"normal","fire","water","electric","grass","ice","fighting","poison","ground","flying","psychic","bug","rock","ghost","dragon","dark","steel","fairy"};
    static final String[] LABELS={"一般","火","水","电","草","冰","格斗","毒","地面","飞行","超能力","虫","岩石","幽灵","龙","恶","钢","妖精"};
    static boolean isMap(ServerPlayerEntity p){return p.func_71121_q().func_73046_m().func_240793_aU_().func_76065_j().equals("回迁智皮卡 1.4.0 手动验收");}
    static void tell(ServerPlayerEntity p,String text){FeatureTestMaps.tell(p,text);}
    static void tick(ServerPlayerEntity p){
        if(p.getPersistentData().func_74767_n("BackportCapMapReady"))return;
        var w=p.func_71121_q();TestMaps.cmd(p,"fill -28 64 -22 28 64 22 minecraft:smooth_stone");TestMaps.cmd(p,"fill -28 65 -22 28 76 22 minecraft:air");
        TestMaps.cmd(p,"gamemode survival");TestMaps.cmd(p,"tp @s 0 65 9");
        TestMaps.block(w,-24,65,12,"pixelmon:healer");TestMaps.block(w,-22,65,12,"pixelmon:pc");TestMaps.block(w,-20,65,12,"minecraft:crafting_table");
        TestMaps.label(w,0,70,-18,"戴帽子的皮卡丘 · 1.4.0 手动验收");
        TestMaps.label(w,0,68,-18,"自然刷新不在此人工展示场验收；请另找平原村庄并等待原生神兽计时");
        TestMaps.label(w,0,67,-18,"点击地面测试台准备对象；战斗、Z按钮、换人和抛球由你操作");
        station(p,-12,12,"重置测试队伍与 Z 手环","team");station(p,-6,12,"结束当前测试对战","end");
        station(p,0,12,"野生智皮卡 / 禁用球对照","cap");station(p,6,12,"双打：水系 + 地面系","double");station(p,12,12,"持久战：幸福蛋","persist");station(p,18,12,"非电系队友与场地技能","skills");
        station(p,24,12,"运动冻结：鞘翅 / 水池材料","motion");
        for(int i=0;i<18;i++)station(p,-24+(i%9)*6,-12+(i/9)*5,LABELS[i]+"系野生对象",TYPES[i]);
        TestMaps.supply(w,-14,18,"智皮卡 Z 与4个皮卡丘 Z",Arrays.asList(DedicatedTestMaps.stack("pixelmon:pikashunium_z",2),DedicatedTestMaps.stack("pixelmon:pikanium_z",4)));
        TestMaps.supply(w,-8,18,"可用球与禁止球对照",Arrays.asList(DedicatedTestMaps.stack("pixelmon:poke_ball",64),DedicatedTestMaps.stack("pixelmon:great_ball",64),DedicatedTestMaps.stack("pixelmon:ultra_ball",64),DedicatedTestMaps.stack("pixelmon:master_ball",4),DedicatedTestMaps.stack("pixelmon:park_ball",4),DedicatedTestMaps.stack("pixelmon:origin_ball",4)));
        TestMaps.supply(w,-2,18,"治疗 / 回忆招式",Arrays.asList(DedicatedTestMaps.stack("pixelmon:full_restore",64),DedicatedTestMaps.stack("pixelmon:revive",64),DedicatedTestMaps.stack("pixelmon:rare_candy",64)));
        station(p,6,18,"太晶爆发：电 / 水太晶对照","tera");
        p.getPersistentData().func_74757_a("BackportCapMapReady",true);team(p);
        tell(p,"测试地图已准备。黄色边框是场地受益提示，不保证打穿免疫。各测试台均为人工对象，不代表自然刷新通过。");
    }
    static void station(ServerPlayerEntity p,int x,int z,String title,String action){TestMaps.block(p.func_71121_q(),x,65,z,"minecraft:yellow_concrete");TestMaps.label(p.func_71121_q(),x+.5,66.5,z+.5,title);}
    static Pokemon mon(int dex,String... moves){Pokemon p=FeatureTestMaps.mon(dex);p.setLevelNum(100);TestMaps.moves(p,moves);p.heal();return p;}
    static void team(ServerPlayerEntity player){
        var party=StorageProxy.getParty(player);party.unlockMega(true);party.setMegaItem(EnumMegaItem.BraceletORAS,true);
        Pokemon cap=mon(CapSpecies.DEX,"Ten Million Volts","Calm Mind","Volt Switch","Energy Ball");cap.setHeldItem(new ItemStack(PixelmonItems.pikashunium_z));
        Pokemon shiny=mon(CapSpecies.DEX,"Ten Million Volts","Nasty Plot","Overheat","Psychic");shiny.setShiny(true);shiny.setHeldItem(new ItemStack(PixelmonItems.pikashunium_z));
        Pokemon ordinary=mon(25,"Thunderbolt","Quick Attack","Electric Terrain","Light Screen");ordinary.setHeldItem(new ItemStack(PixelmonItems.pikashunium_z));
        Pokemon electric=mon(26,"Thunderbolt","Surf","Volt Switch","Protect");
        Pokemon field=mon(151,"Grassy Terrain","Psychic Terrain","Misty Terrain","Protect");
        Pokemon flying=mon(145,"Thunderbolt","Roost","Protect","Discharge");
        Pokemon[] fixtures={cap,shiny,ordinary,electric,field,flying};
        for(int i=0;i<6;i++)FeatureStations.replace(player,i,fixtures[i]);
        for(String id:new String[]{"poke_ball","great_ball","ultra_ball","master_ball","park_ball","origin_ball","full_restore"})player.field_71071_by.func_70441_a(DedicatedTestMaps.stack("pixelmon:"+id,32));
        tell(player,"队伍：正常/闪光智皮卡、普通皮卡丘（同样持智皮卡 Z）、雷丘、三种覆盖场地的梦幻、闪电鸟。前三只供名称/模型/图标/Z资格对照。与回迁天生招式池无关的工具队友只用于手动场地测试。");
    }
    static void spawn(ServerPlayerEntity player,int dex){
        Pokemon p=mon(dex,"Splash");if(dex==CapSpecies.DEX){TestMaps.moves(p,"Ten Million Volts","Calm Mind","Protect","Thunderbolt");p.setLevelNum(70);p.heal();}
        PixelmonEntity e=p.getOrCreatePixelmon(player.field_70170_p,0.5,65,-1.5);e.func_184211_a("backport_cap_fixture");player.field_70170_p.func_217376_c(e);
        tell(player,"人工野生对象已准备："+FeatureStations.chinese(dex)+"。请自行进入对战或抛球；测试台不会授予捕捉成就。");
    }
    static int action(ServerPlayerEntity p,String action){
        if(!isMap(p))return 0;var battle=BattleRegistry.getBattle(p);
        if(action.equals("end")){if(battle!=null)battle.endBattleWithoutXP();return 1;}
        if(battle!=null){tell(p,"请先结束当前对战，再准备下一组对象。");return 0;}
        if(action.equals("team")){team(p);return 1;}
        if(action.equals("skills")){
            Pokemon preview=mon(151,"Thunderbolt","Terrain Pulse","Nature Power","Secret Power");FeatureStations.replace(p,4,preview);
            tell(p,"第5位换为非电系梦幻：十万伏特、场地波动、自然之力、秘密之力。展开爆裂场地后自行换入，检查完整黄色边框。重置队伍可恢复覆盖场地工具队友。");return 1;
        }
        if(action.equals("tera")){
            for(int i=0;i<2;i++){
                Pokemon fixture=mon(151,"Tera Blast","Thunder Wave","Terrain Pulse","Protect");
                fixture.getPersistentData().func_74768_a("TeraType",Arrays.asList(local.pixelmon.backport.Tera.TYPES).indexOf(i==0?Element.ELECTRIC:Element.WATER)+1);
                FeatureStations.replace(p,4+i,fixture);
            }
            TestMaps.supply(p.func_71121_q(),12,18,"太晶爆发手动对照用品",Arrays.asList(DedicatedTestMaps.stack("pixelmon_backport:tera_orb",2)));
            tell(p,"第5/6位为电/水太晶梦幻，招式均有太晶爆发。请自己展开爆裂场地、换入队友并使用太晶按钮：未太晶时太晶爆发仍一般；电太晶后该攻击应有完整黄色边框，水太晶不应借电系加成。太晶珠可经治疗机充能，重置队伍恢复原队友。");return 1;
        }
        if(action.equals("motion")){
            TestMaps.cmd(p,"fill 19 64 17 25 66 21 minecraft:water");TestMaps.cmd(p,"fill 20 84 17 24 84 21 minecraft:smooth_stone");
            TestMaps.supply(p.func_71121_q(),22,12,"手动冻结验收用品",Arrays.asList(DedicatedTestMaps.stack("minecraft:elytra",1),DedicatedTestMaps.stack("minecraft:firework_rocket",64),DedicatedTestMaps.stack("minecraft:water_bucket",1)));
            tell(p,"运动验收用品已准备，东南侧水池与高台已生成；请自行穿戴鞘翅、飞行/入水并发起对战。可用 /tp @s 22 85 19 到高台，/tp @s 22 65 19 到水池。对战中自行聊天或执行指令，结束后检查恢复；不会自动发起战斗。");return 1;
        }
        if(action.equals("double")){
            var party=StorageProxy.getParty(p);Pokemon first=party.get(0),ally=party.get(3);if(first==null || ally==null){tell(p,"请先重置测试队伍。");return 0;}
            Pokemon water=mon(9,"Splash"),ground=mon(450,"Splash");
            var a=first.getOrSpawnPixelmon(p);var b=ally.getOrSpawnPixelmon(p);var c=water.getOrSpawnPixelmon(p.field_70170_p,-3,65,-2);var d=ground.getOrSpawnPixelmon(p.field_70170_p,3,65,-2);
            for(var e:Arrays.asList(c,d))e.func_184211_a("backport_cap_fixture");
            BattleRules rules=new BattleRules(BattleType.DOUBLE);rules.set(BattleRuleRegistry.GEN_MODE,EnumOldGenMode.Mega);
            BattleRegistry.startBattle(new BattleParticipant[]{new PlayerParticipant(p,new Pokemon[]{first,ally})},new BattleParticipant[]{new WildPixelmonParticipant(new Pokemon[]{water,ground})},rules);return 1;
        }
        TestMaps.cmd(p,"kill @e[tag=backport_cap_fixture]");
        if(action.equals("cap")){spawn(p,CapSpecies.DEX);return 1;}
        if(action.equals("persist")){spawn(p,242);return 1;}
        for(int i=0;i<TYPES.length;i++)if(action.equals(TYPES[i])){spawn(p,DEX[i]);return 1;}
        return 0;
    }
    @SubscribeEvent public static void click(PlayerInteractEvent.RightClickBlock event){
        if(event.getWorld().field_72995_K || !(event.getPlayer() instanceof ServerPlayerEntity) || event.getHand()!=net.minecraft.util.Hand.MAIN_HAND)return;
        var p=(ServerPlayerEntity)event.getPlayer();if(!isMap(p))return;BlockPos pos=event.getPos();if(pos.func_177956_o()!=65)return;
        int x=pos.func_177958_n(),z=pos.func_177952_p();String action=null;
        if(z==12)action=x==-12?"team":x==-6?"end":x==0?"cap":x==6?"double":x==12?"persist":x==18?"skills":x==24?"motion":null;
        if(z==18 && x==6)action="tera";
        for(int i=0;i<18;i++)if(x==-24+i%9*6 && z==-12+i/9*5)action=TYPES[i];
        if(action!=null){event.setCanceled(true);action(p,action);}
    }
}
