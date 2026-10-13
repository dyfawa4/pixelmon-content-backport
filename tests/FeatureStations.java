import java.util.*;
import com.google.gson.JsonObject;
import local.pixelmon.backport.LegendAdvancements;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.registries.*;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import com.pixelmonmod.pixelmon.enums.EnumPlate;
import com.pixelmonmod.pixelmon.enums.EnumMegaItem;
import com.pixelmonmod.pixelmon.items.ZygardeCubeItem;
import com.pixelmonmod.pixelmon.items.ScrollItem;
import com.pixelmonmod.pixelmon.blocks.ScrollBlock;
import com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.PowerConstruct;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.item.*;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.*;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/** Manual interaction stations; commands are restricted to the feature worlds. */
@Mod.EventBusSubscriber(modid="backport_client_test")
public final class FeatureStations {
    static final JsonObject CHINESE=local.pixelmon.backport.Content.read("backport/species_names_zh.json");
    static final JsonObject ADDED_CHINESE=local.pixelmon.backport.Content.read("assets/pixelmon_backport/lang/zh_cn.json");
    static final String[] GROUPS={"legacy","gen4","gen5","gen6","gen7","gen8","gen9","ultra","paradox"};
    static final String[] LABELS={"前三世代","神奥","合众","卡洛斯","阿罗拉","伽勒尔/洗翠","帕底亚","究极异兽","悖谬种"};
    static final String[] FUSIONS={"酋雷姆·黑","酋雷姆·白","奈克洛兹玛·黄昏","奈克洛兹玛·拂晓","蕾冠王·白马","蕾冠王·黑马"};
    static final Set<Integer> LEGACY=new LinkedHashSet<>(Arrays.asList(144,145,146,150,151,243,244,245,249,250,251,377,378,379,380,381,382,383,384,385,386));
    static String chinese(int dex){String name=PixelmonSpecies.fromNationalDex(dex).getName(),key="pixelmon."+name.toLowerCase(Locale.ROOT);return ADDED_CHINESE.has(key)?ADDED_CHINESE.get(key).getAsString():CHINESE.has(key)?CHINESE.get(key).getAsString():name;}
    static List<Integer> choices(String group){
        List<Integer> result=new ArrayList<>();if(group.equals("legacy") || group.equals("all"))result.addAll(LEGACY);
        LegendAdvancements.DATA.getAsJsonObject("captures").entrySet().forEach(e->{if(group.equals("all") || e.getValue().getAsJsonObject().get("category").getAsString().equals(group))result.add(Integer.parseInt(e.getKey()));});
        Collections.sort(result);return result;
    }
    static void menu(ServerPlayerEntity p,String group){
        FeatureTestMaps.tell(p,"点击宝可梦名字生成野生测试对象；生成本身不会授予捕捉成就。");
        for(int dex:choices(group)){
            IFormattableTextComponent name=new StringTextComponent("["+chinese(dex)+"] ");
            name.func_230530_a_(Style.field_240709_b_.func_240718_a_(Color.func_240744_a_(TextFormatting.AQUA)).func_240715_a_(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/backportqa feature spawn "+dex)));
            p.func_146105_b(name,false);
        }
    }
    static int resolve(String value){
        for(int dex:choices("all")){
            var species=PixelmonSpecies.fromNationalDex(dex);
            if(value.equals(Integer.toString(dex)) || value.equals(chinese(dex)) || value.equalsIgnoreCase(species.getName()))return dex;
        }
        return -1;
    }
    static int spawn(ServerPlayerEntity p,String value){
        if(!FeatureTestMaps.isMap(p))return 0;int dex=resolve(value);
        if(dex<0){FeatureTestMaps.tell(p,"未找到该传说/幻之宝可梦、究极异兽或悖谬种；右键分类测试台选择。");return 0;}
        spawnDex(p,dex);return 1;
    }
    static void spawnDex(ServerPlayerEntity p,int dex){
        spawnForm(p,dex,null);
    }
    static void spawnForm(ServerPlayerEntity p,int dex,String form){
        var data=p.getPersistentData();
        if(data.func_186855_b("BackportCaptureFixtureUUID")){
            var old=p.func_71121_q().func_217461_a(data.func_186857_a("BackportCaptureFixtureUUID"));
            if(old instanceof PixelmonEntity && ((PixelmonEntity)old).getPokemon().getOwnerPlayerUUID()==null
                && old.getPersistentData().func_74767_n("BackportCaptureFixture"))old.func_70106_y();
        }
        Pokemon pokemon=FeatureTestMaps.mon(dex);if(form!=null)pokemon.setForm(form);PixelmonEntity entity=pokemon.getOrCreatePixelmon(p.field_70170_p,4,65,0);
        entity.getPersistentData().func_74757_a("BackportCaptureFixture",true);p.field_70170_p.func_217376_c(entity);data.func_186854_a("BackportCaptureFixtureUUID",entity.func_110124_au());
        FeatureTestMaps.tell(p,"已在中央生成 "+(form==null?"":"伽勒尔 ")+chinese(dex)+" (#"+dex+")。请实际捕捉测试成就。");
    }
    static void label(ServerWorld w,double x,double y,double z,String text){
        ArmorStandEntity sign=new ArmorStandEntity(w,x,y,z);sign.func_200203_b(new StringTextComponent(text));sign.func_174805_g(true);sign.func_82142_c(true);sign.func_189654_d(true);sign.func_184211_a("backport_feature_station");w.func_217376_c(sign);
    }
    static void station(ServerWorld w,int x,int z,String title,String color){TestMaps.block(w,x,65,z,"minecraft:"+color+"_concrete");label(w,x+.5,66.4,z+.5,title);}
    static void setup(ServerPlayerEntity p){
        var w=p.func_71121_q();TestMaps.cmd(p,"kill @e[type=minecraft:armor_stand,tag=backport_feature_station]");
        if(FeatureTestMaps.riding(p)){
            station(w,-20,-12,"右键：重新准备骑乘队伍","cyan");station(w,20,-12,"右键：结束当前对战","red");
        }else{
            for(int i=0;i<GROUPS.length;i++)station(w,-20+i*5,-12,"右键选择："+LABELS[i],"cyan");
            for(int i=0;i<FUSIONS.length;i++)station(w,-20+i*4,-4,"右键准备："+FUSIONS[i],"purple");
            station(w,20,4,"右键：基格尔德与多面体","lime");
            station(w,20,12,"右键：准备圣杯合成","yellow");
            station(w,20,-4,"右键：结束当前对战","red");
            // Add fixtures to the old empty test area without resetting the party,
            // captured specimens, stored items or existing advancement progress.
            BlockPos chalice=new BlockPos(16,65,12);
            if(w.func_180495_p(chalice).func_177230_c()==net.minecraft.block.Blocks.field_150350_a)TestMaps.block(w,16,65,12,"pixelmon:arc_chalice");
            List<ItemStack> plates=new ArrayList<>();for(var plate:EnumPlate.values())plates.add(new ItemStack(plate.getItem(),8));
            if(w.func_180495_p(new BlockPos(20,65,16)).func_177230_c()==net.minecraft.block.Blocks.field_150350_a)TestMaps.supply(w,20,16,"圣杯合成：全部18种石板",plates);
            label(w,16.5,68,12.5,"天界之笛合成区：逐一手持18种石板右键圣杯");
            label(w,16.5,67.6,14.5,"最后一块后等待约25秒；观察动画、产物，再靠近拾取");
            label(w,0,69,-17,"青色台选择宝可梦，紫色台准备合体；绿台多面体，黄台圣杯");
        }
        station(w,20,20,"右键：准备场外 Mega（J，可改键）","blue");
        station(w,16,20,"右键：无极巨化成就测试","magenta");
        if(!FeatureTestMaps.riding(p)){
            station(w,12,20,"右键：原始回归与剑盾之王","orange");
            station(w,8,20,"右键：轮换特殊形态转换用品","pink");
            station(w,4,20,"右键：熊徒弟与两种卷轴","brown");
            station(w,0,20,"右键：轮换伽勒尔三鸟捕捉","light_blue");
        }
        p.getPersistentData().func_74768_a("BackportFeatureLayoutVersion",5);
    }
    static void replace(ServerPlayerEntity p,int slot,Pokemon pokemon){
        var party=StorageProxy.getParty(p);Pokemon old=party.get(slot);
        if(old!=null)old.getPixelmonEntity().ifPresent(e->e.retrieve("ManualFormFixture"));
        party.set(slot,pokemon);
    }
    static void specialField(ServerPlayerEntity p){
        int[] dex={382,383,888,889};String[] items={"blue_orb","red_orb","rusted_sword","rusted_shield"};
        for(int i=0;i<dex.length;i++){Pokemon mon=FeatureTestMaps.mon(dex[i]);mon.setHeldItem(DedicatedTestMaps.stack("pixelmon:"+items[i],1));replace(p,i,mon);}
        FeatureTestMaps.tell(p,"前四格替换为盖欧卡、固拉多、苍响、藏玛然特，携带对应宝珠/剑/盾。请手动放出，按J完成变化后查看成就；检查换掉道具、收回、进入对战和读档时还原。真实入队计入物种获得成就；变化成就仍需完成对应操作。");
    }
    static void forms(ServerPlayerEntity p){
        int[] dex={492,483,484,487,641,642,645,905,720};
        String[] items={"gracidea","adamant_crystal","lustrous_globe","griseous_core","reveal_glass","reveal_glass","reveal_glass","reveal_glass","prison_bottle"};
        int index=Math.floorMod(p.getPersistentData().func_74762_e("BackportFormFixtureIndex"),dex.length);
        replace(p,0,FeatureTestMaps.mon(dex[index]));p.func_184611_a(Hand.MAIN_HAND,DedicatedTestMaps.stack("pixelmon:"+items[index],1));
        p.getPersistentData().func_74768_a("BackportFormFixtureIndex",index+1);
        if(dex[index]==492)TestMaps.cmd(p,"time set day");
        FeatureTestMaps.tell(p,"第一格已替换为 "+chinese(dex[index])+"，主手为对应原生转换道具。请放出后实际使用道具、等待动画，再检查成就及还原后保留。再次右键轮换；真实入队计入物种获得成就；目标形态成就仍需完成对应操作。");
    }
    static void urshifu(ServerPlayerEntity p){
        replace(p,0,FeatureTestMaps.mon(891));replace(p,1,FeatureTestMaps.mon(891));
        List<ItemStack> scrolls=new ArrayList<>();
        for(var type:ScrollBlock.Type.values())scrolls.add(ScrollItem.makeScroll(type,false,type.getDefaultResource()));
        TestMaps.supply(p.func_71121_q(),-20,20,"熊徒弟进化：水之卷轴 / 恶之卷轴",scrolls);
        FeatureTestMaps.tell(p,"前两格已替换为熊徒弟，西侧木桶有两种原生卷轴。请分别摆放并通过原生交互进化，不要同时在8格内摆两种卷轴。检查一击流“招招致命”和连击流“流水连击”，熊徒弟真实入队计入物种获得；武道熊师形态成就需完成进化。");
    }
    static void galar(ServerPlayerEntity p){
        int index=Math.floorMod(p.getPersistentData().func_74762_e("BackportGalarFixtureIndex"),3);
        spawnForm(p,144+index,"galarian");p.getPersistentData().func_74768_a("BackportGalarFixtureIndex",index+1);
    }
    static void mega(ServerPlayerEntity p){
        var party=StorageProxy.getParty(p);party.unlockMega(false);party.setMegaItem(EnumMegaItem.BraceletORAS,false);
        int[] dex={6,6,149};Item[] stones={PixelmonItems.charizardite_x,PixelmonItems.charizardite_y,local.pixelmon.backport.Backport.item("dragoninite")};
        for(int i=0;i<3;i++){Pokemon old=party.get(i);if(old!=null)old.getPixelmonEntity().ifPresent(e->e.retrieve("FieldMegaFixture"));Pokemon mon=FeatureTestMaps.mon(dex[i]);mon.setHeldItem(new ItemStack(stones[i]));party.set(i,mon);}
        FeatureTestMaps.tell(p,"前三格已准备喷火龙X、喷火龙Y、快龙及对应进化石，并装备Mega手环。选中后放出，按J进化/还原；控制设置可改键。请检查收回、存入电脑、重进及开始战斗时还原。其他队伍格保留。");
    }
    static void eternamax(ServerPlayerEntity p){
        var party=StorageProxy.getParty(p);Pokemon old=party.get(0);if(old!=null)old.getPixelmonEntity().ifPresent(e->e.retrieve("EternamaxFixture"));
        party.set(0,FeatureTestMaps.mon(890));p.func_184611_a(Hand.MAIN_HAND,new ItemStack(local.pixelmon.backport.Backport.item("eternatus_meteorite")));
        FeatureTestMaps.tell(p,"第一格为普通无极汰那，主手是无极陨星。请打开宝可梦资料，将陨星放入它的携带物格，检查无极巨化形态及普通成就“无极之巅”。");
    }
    static void zygarde(ServerPlayerEntity p){
        var party=StorageProxy.getParty(p);Pokemon pokemon=FeatureTestMaps.mon(718);pokemon.setAbility(new PowerConstruct());party.set(0,pokemon);
        ItemStack cube=new ItemStack(PixelmonItems.zygarde_cube);for(var core:ZygardeCubeItem.CoreType.values())if(core!=ZygardeCubeItem.CoreType.RANDOM)ZygardeCubeItem.addCore(cube,core);
        p.func_184611_a(Hand.MAIN_HAND,cube);PixelmonEntity entity=pokemon.getOrSpawnPixelmon(p);entity.func_70012_b(18.5,65,2.5,0,0);
        FeatureTestMaps.tell(p,"已放出属于你的基格尔德，并手持完整多面体；直接右键基格尔德，检查形态和学习招式。真实入队计入物种获得；请实际交互检查形态流程。");
    }
    static void flute(ServerPlayerEntity p){
        var w=p.func_71121_q();TestMaps.block(w,16,65,12,"minecraft:air");TestMaps.block(w,16,65,12,"pixelmon:arc_chalice");
        List<ItemStack> plates=new ArrayList<>();for(var plate:EnumPlate.values())plates.add(new ItemStack(plate.getItem(),8));
        TestMaps.supply(w,20,16,"圣杯合成：全部18种石板",plates);p.func_184611_a(Hand.MAIN_HAND,plates.get(0).func_77946_l());
        FeatureTestMaps.tell(p,"空圣杯已放好，第一种石板在主手，全部18种在右侧木桶。逐一手持右键投入，最后等待约25秒，然后靠近拾取笛子；不直接给成品。");
    }
    static String actionAt(BlockPos pos,boolean riding){
        if(pos.func_177956_o()!=65)return null;int x=pos.func_177958_n(),z=pos.func_177952_p();
        if(x==20 && z==20)return "mega";
        if(x==16 && z==20)return "eternamax";
        if(riding)return z==-12 && x==-20?"riding":z==-12 && x==20?"end":null;
        if(z==20)return x==12?"special_field":x==8?"forms":x==4?"urshifu":x==0?"galar":null;
        if(z==-12)for(int i=0;i<GROUPS.length;i++)if(x==-20+i*5)return "menu:"+GROUPS[i];
        if(z==-4){if(x==20)return "end";for(int i=0;i<FUSIONS.length;i++)if(x==-20+i*4)return "fusion:"+i;}
        return x==20 && z==4?"zygarde":x==20 && z==12?"flute":null;
    }
    @SubscribeEvent public static void click(PlayerInteractEvent.RightClickBlock event){
        if(event.getWorld().field_72995_K || event.getHand()!=Hand.MAIN_HAND || !(event.getPlayer() instanceof ServerPlayerEntity))return;
        ServerPlayerEntity p=(ServerPlayerEntity)event.getPlayer();if(!FeatureTestMaps.isMap(p))return;
        String action=actionAt(event.getPos(),FeatureTestMaps.riding(p));if(action==null)return;
        event.setCanceled(true);
        if(action.startsWith("menu:"))menu(p,action.substring(5));
        else if(action.startsWith("fusion:")){p.getPersistentData().func_74768_a("BackportFusionIndex",Integer.parseInt(action.substring(7)));FeatureTestMaps.action(p,"fusion_next");}
        else FeatureTestMaps.action(p,action);
    }
}
