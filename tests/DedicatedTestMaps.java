import java.util.*;
import java.math.BigDecimal;
import com.google.gson.JsonElement;
import com.pixelmonmod.pixelmon.api.battles.BattleType;
import com.pixelmonmod.pixelmon.battles.api.rules.BattleRules;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.enums.EnumMegaItem;
import com.pixelmonmod.pixelmon.items.medicine.MedicineItem;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.PokeBallRegistry;
import com.pixelmonmod.pixelmon.api.pokemon.species.gender.Gender;
import com.pixelmonmod.pixelmon.api.registries.*;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.battles.*;
import com.pixelmonmod.pixelmon.battles.controller.*;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import com.pixelmonmod.pixelmon.battles.status.*;
import com.pixelmonmod.pixelmon.entities.npcs.*;
import com.pixelmonmod.pixelmon.items.*;
import com.pixelmonmod.pixelmon.enums.TMType;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.*;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.Hand;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.registries.ForgeRegistries;
import local.pixelmon.backport.*;

/** Private fixtures only. Never included in a public addon or share package. */
public final class DedicatedTestMaps {
    static int batch;
    static ItemStack stack(String id,int count){
        if(id.equals("pixelmon:master_ball") || id.equals("pixelmon:ultra_ball"))return PokeBallRegistry.getPokeBall(id.substring(9)).getValue().orElseThrow().getBallItem(count);
        Item i=TestMaps.item(id);if(i==null || i==Items.field_190931_a)throw new AssertionError("Missing map material "+id);return new ItemStack(i,count);
    }
    static String marker(){return "BackportQAReady_"+TestMaps.mode;}
    static void machine(ServerPlayerEntity p,int x,int z,String id){ItemStack item=stack(id,1);Item raw=item.func_77973_b();if(!(raw instanceof BlockItem))throw new AssertionError("Map machine is not a block item "+id);
        ItemStack previous=p.func_184586_b(Hand.MAIN_HAND);p.func_184611_a(Hand.MAIN_HAND,item);
        try{raw.func_195939_a(new ItemUseContext(p,Hand.MAIN_HAND,new BlockRayTraceResult(new Vector3d(x+.5,65,z+.5),Direction.UP,new BlockPos(x,64,z),false)));}finally{p.func_184611_a(Hand.MAIN_HAND,previous);}
        if(p.func_71121_q().func_180495_p(new BlockPos(x,65,z)).func_177230_c().getRegistryName()==null || !p.func_71121_q().func_180495_p(new BlockPos(x,65,z)).func_177230_c().getRegistryName().toString().equals(id))throw new AssertionError("Native machine placement failed "+id);
    }
    static void message(ServerPlayerEntity p,String text){p.func_146105_b(new StringTextComponent(text),false);}
    static void setup(ServerPlayerEntity p){
        ServerWorld w=p.func_71121_q();boolean fresh=!p.getPersistentData().func_74767_n(marker());
        // Ensure the whole arena is loaded before vanilla fill/spawnpoint commands.
        // This also covers first join with a different view distance or world spawn.
        for(int x=-2;x<=1;x++)for(int z=-1;z<=0;z++)w.func_175726_f(new net.minecraft.util.math.BlockPos(x*16,64,z*16));
        TestMaps.stations.clear();TestMaps.cmd(p,"kill @e[type=minecraft:armor_stand,tag=backport_qa_fixture]");
        TestMaps.cmd(p,"gamerule doDaylightCycle false");TestMaps.cmd(p,"gamerule doWeatherCycle false");TestMaps.cmd(p,"gamerule doMobSpawning false");TestMaps.cmd(p,"time set day");TestMaps.cmd(p,"weather clear");
        if(fresh){
            TestMaps.cmd(p,"fill -20 64 -14 20 74 14 minecraft:air");TestMaps.cmd(p,"fill -20 64 -14 20 64 14 minecraft:smooth_quartz");
            TestMaps.cmd(p,"fill -20 65 -14 20 65 -14 minecraft:light_blue_concrete");TestMaps.cmd(p,"fill -20 65 14 20 65 14 minecraft:light_blue_concrete");
            TestMaps.cmd(p,"fill -20 65 -14 -20 65 14 minecraft:light_blue_concrete");TestMaps.cmd(p,"fill 20 65 -14 20 65 14 minecraft:light_blue_concrete");
            TestMaps.cmd(p,"fill -19 64 -13 19 64 -13 minecraft:sea_lantern");TestMaps.cmd(p,"fill -19 64 13 19 64 13 minecraft:sea_lantern");
            TestMaps.npc(w,new NPCNurseJoy(w),6,-10,"治疗护士");TestMaps.npc(w,new NPCRelearner(w),-6,-10,"技能回忆师");
            machine(p,8,-10,PixelmonBlocks.white_healer.getRegistryName().toString());machine(p,-8,-10,PixelmonBlocks.white_pc.getRegistryName().toString());
            TestMaps.block(w,10,65,-10,"minecraft:crafting_table");TestMaps.block(w,12,65,-10,"minecraft:anvil");
        }
        TestMaps.label(w,8.5,66.2,-9.5,"治疗仪／太晶珠充能");TestMaps.label(w,-7.5,66.2,-9.5,"电脑／存档测试");
        TestMaps.station(w,-9,9,"重新准备测试队伍","reset");TestMaps.station(w,9,9,"结束对战（或用指令）","end");
        if(TestMaps.mode.equals("eternatus")){
            TestMaps.station(w,-8,3,"用当前首位开始战斗","eternabattle");TestMaps.station(w,-4,3,"首位剩 1 HP","hurt");TestMaps.station(w,0,3,"首位濒死","faint");TestMaps.station(w,4,3,"展示当前无极汰那","showfirst");
            if(fresh){
                TestMaps.supply(w,-16,9,"合成材料：按说明摆九格",Arrays.asList(stack("minecraft:end_stone_bricks",64),stack("pixelmon:azure_flute",8),stack("pixelmon:meteorite",8),stack("pixelmon:master_ball",8)));
                TestMaps.supply(w,16,9,"成品对照／回复药品",Arrays.asList(stack("pixelmon_backport:eternatus_meteorite",1),stack("pixelmon:max_potion",16),stack("pixelmon:revive",16),stack("pixelmon:rare_candy",16)));
            }
        }else{
            String[] actions={"careteam","breedteam","evolveteam","starterteam","nextteam","interactionbattle","hurt","faint"};
            String[] names={"49 级升级与交互队伍","培育：铝钢桥龙与百变怪","物品进化队伍","帕底亚御三家队伍","更多回迁个体（六只一组）","用当前首位战斗","损伤与灼伤／PP 耗尽","首位濒死"};
            for(int i=0;i<8;i++)TestMaps.station(w,-14+(i%4)*4,3+(i/4)*4,names[i],actions[i]);
            if(fresh){
                machine(p,-11,-10,PixelmonBlocks.black_day_care.getRegistryName().toString());
                TestMaps.cmd(p,"fill 6 61 -2 17 64 7 minecraft:stone");TestMaps.cmd(p,"fill 7 62 -1 16 64 6 minecraft:water");
                List<ItemStack> medicines=new ArrayList<>(),mints=new ArrayList<>(),candies=new ArrayList<>();
                for(Item i:ForgeRegistries.ITEMS){if(i instanceof MedicineItem)medicines.add(new ItemStack(i,16));else if(i instanceof MintItem)mints.add(new ItemStack(i,16));else if(i instanceof ExpCandyItem)candies.add(new ItemStack(i,16));}
                TestMaps.supply(w,-18,9,"药品 1",medicines.subList(0,Math.min(27,medicines.size())));if(medicines.size()>27)TestMaps.supply(w,-18,6,"药品 2",medicines.subList(27,medicines.size()));
                TestMaps.supply(w,-18,2,"全部薄荷",mints);TestMaps.supply(w,-18,-2,"糖果／经验道具",candies);
                List<ItemStack> utility=new ArrayList<>();for(String id:new String[]{"protein","iron","calcium","zinc","carbos","hp_up","pp_up","pp_max","ability_capsule","ability_patch","ever_stone","destiny_knot","copper_hourglass","silver_hourglass","gold_hourglass","diamond_hourglass","old_rod","good_rod","super_rod","oas_rod"})utility.add(stack("pixelmon:"+id,8));TestMaps.supply(w,18,9,"能力／培育／鱼竿",utility);
                List<ItemStack> added=new ArrayList<>();for(net.minecraftforge.fml.RegistryObject<Item> registered:Backport.REGISTERED.values())if(!(registered.get() instanceof TeraItems.Shard))added.add(new ItemStack(registered.get(),registered.get() instanceof TeraItems.Orb?1:8));
                for(int offset=0;offset<added.size();offset+=27)TestMaps.supply(w,18,5-(offset/27)*4,"新增物品 "+(offset/27+1),added.subList(offset,Math.min(offset+27,added.size())));
                List<ItemStack> disks=new ArrayList<>();for(int id:new int[]{171,207,210,225,226,227,228,229})disks.add(TechnicalMoveItem.of(TMType.TM9,BackportTechnicalMoves.resolve(TMType.TM9,id)));TestMaps.supply(w,-18,-6,"太晶爆发与新增光盘",disks);
                List<ItemStack> coins=new ArrayList<>();for(int i=0;i<16;i++)coins.add(stack("pixelmon_backport:gimmighoul_coin",64));TestMaps.supply(w,-18,-10,"索财灵进化：至少 999 硬币",coins);
            }
            TestMaps.label(w,-10.5,66.2,-9.5,"培育屋：放入兼容父母／查看要求");TestMaps.label(w,11.5,66.2,7.5,"钓鱼池：使用原生鱼竿等待咬钩");
        }
        if(!fresh)for(int x=-19;x<=19;x++)for(int z=-13;z<=13;z++){net.minecraft.tileentity.TileEntity tile=w.func_175625_s(new net.minecraft.util.math.BlockPos(x,65,z));if(tile!=null && tile.getTileData().func_74764_b("BackportQATitle"))TestMaps.label(w,x+.5,66.1,z+.5,tile.getTileData().func_74779_i("BackportQATitle"));}
        TestMaps.cmd(p,"spawnpoint @s 0 65 11");place(p);
        if(fresh){reset(p);p.getPersistentData().func_74757_a(marker(),true);}else message(p,"已保留测试进度；电脑、队伍、培育屋和箱子不会在重进时重置。");
        System.out.println("BACKPORT_QA_MAP_READY "+TestMaps.mode+" stations="+TestMaps.stations.size()+" fresh="+fresh);
    }
    static Pokemon mon(int dex,int level){Pokemon p=TestMaps.mon(dex);p.setLevelNum(level);p.heal();return p;}
    static void team(ServerPlayerEntity owner,int[] dex,int level){TestMaps.end(owner);for(int i=0;i<6;i++)StorageProxy.getParty(owner).set(i,i<dex.length?mon(dex[i],level):null);}
    static void reset(ServerPlayerEntity p){
        StorageProxy.getParty(p).starterPicked=true;
        TestMaps.end(p);p.field_71071_by.field_70462_a.clear();p.field_71071_by.field_184439_c.clear();StorageProxy.getParty(p).setBalance(new BigDecimal("1000000"));StorageProxy.getParty(p).unlockMega(true);StorageProxy.getParty(p).setMegaItem(EnumMegaItem.BraceletORAS,true);
        if(TestMaps.mode.equals("eternatus")){
            team(p,new int[]{890,890,1018,143,1024,658},100);for(int i=0;i<2;i++){Pokemon e=StorageProxy.getParty(p).get(i);e.setForm("ordinary");e.setNature(Nature.HARDY);e.getIVs().fillFromArray(new int[6]);e.getEVs().fillFromArray(new int[6]);TestMaps.moves(e,"Dynamax Cannon","Flamethrower","Dragon Pulse","Recover");e.heal();}
        }else careTeam(p);
        p.field_71071_by.field_70462_a.set(0,stack("pixelmon_backport:tera_orb",1));p.field_71071_by.field_70462_a.set(1,stack("pixelmon:red_poke_bag",1));p.field_71071_by.field_70462_a.set(2,stack("pixelmon_backport:water_tera_shard",64));p.field_71071_by.field_70462_a.set(3,stack("pixelmon:ultra_ball",64));p.field_71071_by.func_70296_d();
        place(p);message(p,"队伍与随身道具已准备。木桶取用品；护士旁有真实治疗仪。结束对战：/backportqa map end。");
    }
    static void place(ServerPlayerEntity p){p.func_70012_b(.5,65,11.5,180,10);p.func_70634_a(.5,65,11.5);TestMaps.cmd(p,"tp @s 0.5 65 11.5 180 10");}
    static void careTeam(ServerPlayerEntity p){team(p,new int[]{1018,1019,1013,1024,1012,132},49);for(Pokemon mon:StorageProxy.getParty(p).getAll())if(mon!=null)TestMaps.moves(mon,"Tackle","Protect");message(p,"首位铝钢桥龙 49 级：先喂神奇糖果测试 50 级太晶爆发；队伍同时用于药品、薄荷、技能回忆和钓鱼。");}
    static boolean action(ServerPlayerEntity p,String kind){
        if(!"eternatus".equals(TestMaps.mode) && !"interactions".equals(TestMaps.mode))return false;
        if(kind.equals("eternabattle") || kind.equals("interactionbattle")){battle(p);return true;}
        if(kind.equals("hurt") || kind.equals("faint")){
            TestMaps.end(p);Pokemon first=StorageProxy.getParty(p).get(0);if(first==null)return true;first.setHealth(kind.equals("faint")?0:1);
            if(kind.equals("hurt") && TestMaps.mode.equals("interactions")){first.setStatus(new Burn());for(Attack attack:first.getMoveset())attack.pp=0;}
            show(p);message(p,kind.equals("faint")?"首位已濒死：换装不得复活，复活药可正常复活。":"已设置损伤；右键放出的宝可梦使用药品，健康后再用应不消耗。");return true;
        }
        if(kind.equals("showfirst")){TestMaps.end(p);show(p);return true;}
        if(kind.equals("careteam")){careTeam(p);return true;}
        if(kind.equals("breedteam")){
            team(p,new int[]{1018,132,1024,1013,1011,1019},70);Pokemon mother=StorageProxy.getParty(p).get(0),ditto=StorageProxy.getParty(p).get(1);mother.setGender(Gender.FEMALE);mother.setNature(Nature.ADAMANT);mother.setMintNature(Nature.MODEST);mother.getIVs().fillFromArray(new int[]{31,31,31,31,31,31});ditto.getIVs().fillFromArray(new int[]{31,31,31,31,31,31});mother.setHeldItem(stack("pixelmon:ever_stone",1));ditto.setHeldItem(stack("pixelmon:destiny_knot",1));
            message(p,"铝钢桥龙母体携带不变之石、固执原始性格／内敛薄荷；百变怪携带红线。双方满个体。放入培育屋并满足原生要求；应生出铝钢龙，继承固执而非薄荷。");return true;
        }
        if(kind.equals("evolveteam")){team(p,new int[]{840,1011,1012,1012,884,999},40);StorageProxy.getParty(p).get(3).setForm("artisan");message(p,"啃果虫用蜜汁苹果、斯魔茶普通／杰作用对应茶碗、铝钢龙用复合金属；裹蜜虫学会龙之声再升级。索财灵需 999 硬币并升级。");return true;}
        if(kind.equals("starterteam")){team(p,new int[]{906,909,912,907,910,913},49);message(p,"帕底亚御三家及第二阶段；喂糖果按原生进化与升级学习流程检查。未完成的三个预览御三家仍禁用。");return true;}
        if(kind.equals("nextteam")){List<Integer> dex=new ArrayList<>();for(JsonElement patch:Content.DATA.getAsJsonArray("patches")){int id=patch.getAsJsonObject().getAsJsonObject("species").get("dex").getAsInt();if(!dex.contains(id))dex.add(id);}Collections.sort(dex);int[] group=new int[Math.min(6,dex.size())];for(int i=0;i<group.length;i++)group[i]=dex.get((batch+i)%dex.size());batch=(batch+group.length)%dex.size();team(p,group,70);message(p,"当前六只回迁个体图鉴编号："+Arrays.toString(group)+"。全部补丁种类循环轮换，可检查薄荷、药品、回忆与存取。");return true;}
        message(p,"此地图请使用自己的蓝色测试台，或 reset / end。");return true;
    }
    static void show(ServerPlayerEntity p){Pokemon first=StorageProxy.getParty(p).get(0);if(first==null)return;net.minecraft.entity.Entity e=first.getOrSpawnPixelmon(p);if(e!=null){e.func_226286_f_(.5,65,-2.5);TestMaps.testEntities.add(e);}}
    static void battle(ServerPlayerEntity p){
        TestMaps.end(p);Pokemon user=StorageProxy.getParty(p).get(0);if(user==null || user.getHealth()==0){message(p,"首位没有战斗能力，请先用治疗仪或复活药恢复。");return;}
        Pokemon enemy=mon(242,100);TestMaps.moves(enemy,"Splash");p.func_70634_a(.5,65,11.5);
        net.minecraft.entity.Entity a=user.getOrSpawnPixelmon(p),e=enemy.getOrSpawnPixelmon(p.field_70170_p,.5,65,-3.5);TestMaps.testEntities.add(a);TestMaps.testEntities.add(e);
        BattleController battle=BattleRegistry.startBattle(new BattleParticipant[]{new PlayerParticipant(p,new Pokemon[]{user})},new BattleParticipant[]{new WildPixelmonParticipant(new Pokemon[]{enemy})},new BattleRules(BattleType.SINGLE));
        if(battle==null)throw new AssertionError("Dedicated map battle rejected");message(p,"使用你的当前个体开战，保留形态、道具和招式。对照敌人只用跃起。结束指令：/backportqa map end。");
    }
}
