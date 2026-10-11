import java.util.*;
import java.math.BigDecimal;
import com.pixelmonmod.pixelmon.api.battles.BattleType;
import com.pixelmonmod.pixelmon.items.PokeBagItem;
import com.pixelmonmod.pixelmon.items.heldItems.MegaStoneItem;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.registries.*;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.battles.*;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.api.rules.*;
import com.pixelmonmod.pixelmon.battles.controller.*;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import com.pixelmonmod.pixelmon.entities.npcs.*;
import com.pixelmonmod.pixelmon.entities.DenEntity;
import com.pixelmonmod.pixelmon.battles.raids.RaidData;
import com.pixelmonmod.pixelmon.entities.npcs.registry.*;
import com.pixelmonmod.pixelmon.enums.*;
import local.pixelmon.backport.*;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.*;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.minecraftforge.registries.ForgeRegistries;

/** Only included in the private QA harness. Real native NPCs/containers/battles. */
@Mod.EventBusSubscriber(modid="backport_client_test")
public final class TestMaps {
    static boolean prepared;static String mode;
    static final Map<BlockPos,String> stations=new LinkedHashMap<>();
    static final Set<Entity> testEntities=new HashSet<>();
    static final Set<String> observed=new HashSet<>();
    static String scenario="";
    static void cmd(ServerPlayerEntity p,String text){ServerLifecycleHooks.getCurrentServer().func_195571_aL().func_197059_a(p.func_195051_bN().func_197033_a(4).func_197031_a(),text);}
    static boolean isMap(ServerPlayerEntity p){return ServerLifecycleHooks.getCurrentServer().func_240793_aU_().func_76065_j().startsWith("回迁测试 ");}
    static Item item(String id){return ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));}
    static void block(ServerWorld w,int x,int y,int z,String id){Block b=ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id));w.func_180501_a(new BlockPos(x,y,z),b.func_176223_P(),3);}
    static void label(ServerWorld w,double x,double y,double z,String text){ArmorStandEntity a=new ArmorStandEntity(w,x,y,z);a.func_200203_b(new StringTextComponent(text));a.func_174805_g(true);a.func_82142_c(true);a.func_189654_d(true);a.func_184211_a("backport_qa_fixture");w.func_217376_c(a);}
    static void station(ServerWorld w,int x,int z,String name,String action){block(w,x,65,z,"minecraft:cyan_concrete");label(w,x+.5,66.1,z+.5,name);stations.put(new BlockPos(x,65,z),action);}
    static void supply(ServerWorld w,int x,int z,String title,List<ItemStack> items){
        block(w,x,65,z,"minecraft:barrel");TileEntity tile=w.func_175625_s(new BlockPos(x,65,z));IInventory inv=(IInventory)tile;
        for(int i=0;i<Math.min(inv.func_70302_i_(),items.size());i++)inv.func_70299_a(i,items.get(i));tile.getTileData().func_74778_a("BackportQATitle",title);tile.func_70296_d();label(w,x+.5,66.1,z+.5,title);
    }
    static void npc(ServerWorld w,Entity npc,int x,int z,String name){npc.func_70012_b(x+.5,65,z+.5,0,0);npc.func_200203_b(new StringTextComponent(name));npc.func_174805_g(true);npc.func_184211_a("backport_qa_fixture");w.func_217376_c(npc);label(w,x+.5,67.2,z+.5,name);}
    static void setup(ServerPlayerEntity p){
        String title=ServerLifecycleHooks.getCurrentServer().func_240793_aU_().func_76065_j();mode=title.contains("01 ")?"shop":title.contains("02 ")?"forms":title.contains("03 ")?"single":title.contains("05 ")?"raids":"double";
        if(title.contains("06 ") || title.contains("07 ")){mode=title.contains("06 ")?"eternatus":"interactions";DedicatedTestMaps.setup(p);prepared=true;return;}
        ServerWorld w=p.func_71121_q();
        cmd(p,"kill @e[tag=backport_qa_fixture]");
        // Clean only old test opponents inside this private test arena.
        cmd(p,"kill @e[type=pixelmon:pixelmon,x=-16,y=64,z=-12,dx=32,dy=10,dz=24]");
        cmd(p,"gamerule doDaylightCycle false");cmd(p,"gamerule doWeatherCycle false");cmd(p,"gamerule doMobSpawning false");cmd(p,"time set day");cmd(p,"weather clear");
        cmd(p,"fill -16 64 -12 16 73 12 minecraft:air");cmd(p,"fill -16 64 -12 16 64 12 minecraft:smooth_quartz");
        cmd(p,"fill -16 65 -12 16 67 -12 minecraft:light_blue_concrete");cmd(p,"fill -16 65 12 16 67 12 minecraft:light_blue_concrete");
        cmd(p,"fill -16 65 -12 -16 67 12 minecraft:light_blue_concrete");cmd(p,"fill 16 65 -12 16 67 12 minecraft:light_blue_concrete");
        cmd(p,"fill -15 64 -11 15 64 -11 minecraft:sea_lantern");cmd(p,"fill -15 64 11 15 64 11 minecraft:sea_lantern");
        for(int x=-12;x<=12;x+=4)block(w,x,64,0,"minecraft:cyan_concrete");
        label(w,0,68,-10,title);label(w,0,67,-10,"右键蓝色测试台准备场景；供应箱与原生 NPC 可反复使用");
        station(w,-9,4,"重置队伍、道具与充能","reset");station(w,9,4,"结束当前对战","end");
        List<ItemStack> orb=new ArrayList<>();orb.add(new ItemStack(Backport.REGISTERED.get("tera_orb").get()));orb.add(new ItemStack(PixelmonItems.red_poke_bag));
        for(String type:TeraItems.NAMES)orb.add(new ItemStack(Backport.REGISTERED.get(type+"_tera_shard").get(),64));supply(w,-12,7,"太晶珠／背包／各属性碎片",orb);
        List<ItemStack> held=new ArrayList<>();for(String name:new String[]{"clear_amulet","covert_cloak","fairy_feather","utility_umbrella","eternatus_meteorite","metal_alloy","unremarkable_teacup","masterpiece_teacup","dragoninite","raichunite_x","raichunite_y","teal_mask","wellspring_mask","hearthflame_mask","cornerstone_mask"})held.add(new ItemStack(Backport.item(name)));
        held.add(new ItemStack(item("minecraft:shears")));held.add(new ItemStack(item("pixelmon:ability_capsule"),8));held.add(new ItemStack(item("pixelmon:ability_patch"),8));supply(w,12,7,"换形、特性、Mega 与携带道具",held);
        List<ItemStack> mega=new ArrayList<>();Set<Item> megaSeen=Collections.newSetFromMap(new IdentityHashMap<>());for(String name:Backport.REGISTERED.keySet()){Item registered=Backport.item(name);if(registered instanceof MegaStoneItem && megaSeen.add(registered))mega.add(new ItemStack(registered));}
        supply(w,12,3,"新增 Mega 石 1",mega.subList(0,Math.min(27,mega.size())));if(mega.size()>27)supply(w,12,0,"新增 Mega 石 2",mega.subList(27,mega.size()));
        NPCRelearner learner=new NPCRelearner(w);npc(w,learner,-6,-7,"技能回忆师");
        NPCNurseJoy nurse=new NPCNurseJoy(w);npc(w,nurse,6,-7,"治疗护士");
        // Put a genuine healer beside the nurse; it is the orb recharge path.
        DedicatedTestMaps.machine(p,8,-7,PixelmonBlocks.white_healer.getRegistryName().toString());
        DedicatedTestMaps.machine(p,-8,-7,PixelmonBlocks.white_pc.getRegistryName().toString());
        label(w,8.5,67,-6.5,"治疗仪：回复队伍／太晶珠充能");
        label(w,-7.5,67,-6.5,"电脑：存取／持久化测试");
        block(w,10,65,-7,"minecraft:crafting_table");block(w,12,65,-7,"minecraft:anvil");
        List<ItemStack> basics=new ArrayList<>();for(String id:new String[]{"pixelmon:rare_candy","pixelmon:revive","pixelmon:max_potion","pixelmon:pp_max","pixelmon:ability_capsule","pixelmon:ability_patch","minecraft:shears","minecraft:red_dye","minecraft:blue_dye","minecraft:yellow_dye","minecraft:white_dye","minecraft:black_dye"}){Item i=item(id);if(i!=null)basics.add(new ItemStack(i,16));}supply(w,-12,3,"回复／特性／美容与换形工具",basics);
        if(mode.equals("shop")){
            NPCShopkeeper shop=new NPCShopkeeper(w);shop.init(ServerNPCRegistry.shopkeepers.getRandom());shop.loadItems();
            shop.getItemList().clear();for(Map.Entry<String,net.minecraftforge.fml.RegistryObject<Item>> e:Backport.REGISTERED.entrySet()){
                String id=e.getValue().getId().toString();BaseShopItem base=new BaseShopItem(id,new ItemStack(e.getValue().get()),100,20);NPCRegistryShopkeepers.shopItems.put(id,base);shop.getItemList().add(new ShopItemWithVariation(new ShopItem(base,1,1,false),1));
            }
            npc(w,shop,0,-5,"新增道具购买／卖出测试");
            station(w,-5,0,"背包内太晶珠与 52 水碎片","bag");station(w,5,0,"背包太晶实战","bagbattle");
        }else if(mode.equals("forms")){
            station(w,-6,0,"厄诡椪与面具","ogerpon");station(w,-2,0,"换形与物品进化队伍","forms");station(w,2,0,"小智甲贺忍蛙实战","ash");station(w,6,0,"ZA 新 Mega 队伍","za");
            station(w,0,2,"无极汰那与陨星合成测试","eternamax");
            List<ItemStack> materials=new ArrayList<>();materials.add(new ItemStack(item("minecraft:end_stone_bricks"),6));for(String id:new String[]{"pixelmon:azure_flute","pixelmon:meteorite","pixelmon:master_ball"})materials.add(DedicatedTestMaps.stack(id,1));supply(w,-12,0,"无极陨星：完整九格合成材料",materials);
            String[] actions={"raichux","raichuy","dragonite","starmie","greninja","zygarde","darkrai"};
            String[] labels={"开始雷丘 X 战斗","开始雷丘 Y 战斗","开始快龙 Mega 战斗","开始宝石海星 Mega 战斗","开始甲贺忍蛙 Mega 战斗","开始基格尔德 Mega 战斗","开始达克莱伊 Mega 战斗"};
            for(int i=0;i<actions.length;i++)station(w,-12+i*4,-3,labels[i],actions[i]);
        }else if(mode.equals("single")){
            station(w,-8,0,"开始水太晶测试战斗","tera");station(w,-4,0,"开始星晶太晶爆发战斗","stellar");station(w,0,0,"开始 Mega／太晶测试战斗","mega");station(w,4,0,"开始 Z／太晶测试战斗","z");station(w,8,0,"开始太乐巴戈斯战斗","terapagos");
        }else if(mode.equals("raids")){
            station(w,-5,2,"重新生成四个巢穴","raids");station(w,5,2,"准备 90 级团战队伍","raidteam");
            label(w,0,67,10,"先打左侧 1 星；胜利后检查碎片、光盘和捕捉；6 星难度较高");
            supply(w,12,-3,"两种创造放置工具（仅测试）",Arrays.asList(new ItemStack(item("pixelmon:den_spawner")),new ItemStack(Backport.REGISTERED.get("tera_den_spawner").get())));
            raidFixtures(w);
        }else {
            station(w,-6,0,"双打 Mega／太晶布局","double");station(w,0,0,"双打太晶与换人","doubletera");station(w,6,0,"双打晶光星群目标","doublestar");
        }
        reset(p);prepared=true;System.out.println("BACKPORT_QA_MAP_READY "+mode+" stations="+stations.size());
    }
    static void tick(ServerPlayerEntity p){
        if(!prepared){setup(p);return;}
        BattleController b=BattleRegistry.getBattle(p);if(b==null)return;
        for(BattleParticipant team:b.participants)for(PixelmonWrapper mon:team.allPokemon){
            if(Tera.active(mon) && observed.add("tera:"+mon.getPokemonUUID()))System.out.println("BACKPORT_QA_TERA_ACTIVE "+scenario+" dex="+mon.getSpecies().getDex()+" form="+mon.getForm().getName());
            if(mon.isMega && observed.add("mega:"+mon.getPokemonUUID()))System.out.println("BACKPORT_QA_MEGA_ACTIVE "+scenario+" dex="+mon.getSpecies().getDex()+" form="+mon.getForm().getName());
        }
    }
    static void reset(ServerPlayerEntity p){
        if("eternatus".equals(mode) || "interactions".equals(mode)){DedicatedTestMaps.reset(p);return;}
        end(p);p.field_71071_by.field_70462_a.clear();p.field_71071_by.field_184439_c.clear();
        StorageProxy.getParty(p).starterPicked=true;
        StorageProxy.getParty(p).setBalance(new BigDecimal("1000000"));StorageProxy.getParty(p).unlockMega(true);StorageProxy.getParty(p).setMegaItem(EnumMegaItem.BraceletORAS,true);
        for(int i=0;i<6;i++)StorageProxy.getParty(p).set(i,null);
        int[] party=mode.equals("forms")?new int[]{1017,741,676,884,1012,890}:new int[]{143,1017,1024,149,26,658};
        for(int i=0;i<party.length;i++)StorageProxy.getParty(p).set(i,mon(party[i]));
        p.field_71071_by.field_70462_a.set(0,new ItemStack(Backport.REGISTERED.get("tera_orb").get()));p.field_71071_by.field_70462_a.set(1,new ItemStack(PixelmonItems.red_poke_bag));
        if(!mode.equals("raids"))p.field_71071_by.field_70462_a.set(2,new ItemStack(Backport.REGISTERED.get("water_tera_shard").get(),64));
        p.field_71071_by.field_70462_a.set(3,DedicatedTestMaps.stack(mode.equals("raids")?"pixelmon:ultra_ball":"pixelmon:master_ball",64));
        if(mode.equals("raids"))raidTeam(p);
        cmd(p,"spawnpoint @s 0 65 7");cmd(p,"tp @s 0.5 65 7.5 180 12");p.field_71071_by.func_70296_d();p.func_146105_b(new StringTextComponent("右键蓝色台开始。对战中输入 /backportqa map end 结束；/backportqa map reset 重置。"),false);
    }
    static void end(ServerPlayerEntity p){BattleController b=BattleRegistry.getBattle(p);if(b!=null)b.endBattleWithoutXP();for(Entity e:testEntities)e.func_70106_y();testEntities.clear();observed.clear();}
    static void raidTeam(ServerPlayerEntity p){int[] dex={143,6,9,983,1024,658};for(int i=0;i<6;i++){Pokemon mon=mon(dex[i]);mon.setLevelNum(90);moves(mon,"Tera Blast","Flamethrower","Surf","Ice Beam");mon.heal();Tera.changeType(mon,Element.WATER);StorageProxy.getParty(p).set(i,mon);}}
    static void raidLabel(ServerWorld w,double x,double y,double z,String text){ArmorStandEntity a=new ArmorStandEntity(w,x,y,z);a.func_200203_b(new StringTextComponent(text));a.func_174805_g(true);a.func_82142_c(true);a.func_189654_d(true);a.func_184211_a("backport_qa_fixture");a.func_184211_a("backport_qa_den");w.func_217376_c(a);}
    static void raidFixtures(ServerWorld w){
        List<Entity> old=new ArrayList<>();for(Entity e:w.func_241136_z_())if(e.func_184216_O().contains("backport_qa_den"))old.add(e);for(Entity e:old)e.func_70106_y();
        int[] stars={1,3,6},dex={133,983,149},types={2,1,4};String[] names={"1 星水太晶伊布","3 星火太晶仆刀将军","6 星电太晶快龙"};
        for(int i=0;i<3;i++){TeraDenEntity den=new TeraDenEntity(w);den.func_226286_f_(-9+i*9+.5,65,-3.5);Pokemon mon=mon(dex[i]);den.configure(mon,stars[i],types[i],(int)(w.func_72820_D()/24000));den.func_184211_a("backport_qa_fixture");den.func_184211_a("backport_qa_den");w.func_217376_c(den);raidLabel(w,-9+i*9+.5,68,-3.5,names[i]);}
        DenEntity max=new DenEntity(w);max.func_189654_d(true);max.getPersistentData().func_74768_a("BackportDenKind",0);max.func_226286_f_(.5,65,-8.5);Pokemon mon=mon(133);max.setData(new RaidData(max.func_145782_y(),1,mon.getSpecies(),mon.getForm()));max.setLastRollDay((int)(w.func_72820_D()/24000));max.func_184211_a("backport_qa_fixture");max.func_184211_a("backport_qa_den");w.func_217376_c(max);raidLabel(w,.5,67,-8.5,"原生极巨巢穴：对照掉落与战斗");
    }
    static Pokemon mon(int dex){Pokemon p=PokemonFactory.create(PixelmonSpecies.fromNationalDex(dex));p.setLevelNum(70);p.initialize();p.heal();return p;}
    static void moves(Pokemon p,String... names){p.getMoveset().clear();for(String name:names)p.getMoveset().add(new Attack(name));}
    static void bag(ServerPlayerEntity p){
        ItemStack bag=new ItemStack(PixelmonItems.red_poke_bag);p.field_71071_by.field_70462_a.clear();p.field_71071_by.field_70462_a.set(0,bag);
        PokeBagItem.PokeBag contents=PokeBagItem.PokeBag.readFromItemStack(p,bag);
        contents.inventory.func_70299_a(0,new ItemStack(Backport.REGISTERED.get("tera_orb").get()));contents.inventory.func_70299_a(1,new ItemStack(Backport.REGISTERED.get("water_tera_shard").get(),52));
        Tera.changeType(StorageProxy.getParty(p).get(0),Element.FIRE);p.field_71071_by.func_70296_d();
    }
    static void action(ServerPlayerEntity p,String kind){
        if(kind.equals("reset")){reset(p);return;}if(kind.equals("end")){end(p);return;}
        if(DedicatedTestMaps.action(p,kind))return;
        end(p);scenario=kind;boolean doub=kind.startsWith("double");
        if(kind.equals("raids")){raidFixtures(p.func_71121_q());return;}if(kind.equals("raidteam")){raidTeam(p);return;}
        if(kind.equals("bag")){bag(p);return;}
        if(kind.equals("eternamax")){StorageProxy.getParty(p).set(0,mon(890));p.func_146105_b(new StringTextComponent("已给予普通无极汰那。左侧木桶取合成材料，右后方工作台合成；手动携带无极陨星检查变形，取下检查恢复。"),false);return;}
        if(kind.equals("forms") || kind.equals("za") || kind.equals("ogerpon")){
            int[] dex=kind.equals("forms")?new int[]{1017,741,676,884,1012,890}:kind.equals("za")?new int[]{26,149,121,658,718,491}:new int[]{1017,1017,1017,1017};
            for(int i=0;i<6;i++)StorageProxy.getParty(p).set(i,i<dex.length?mon(dex[i]):null);
            if(kind.equals("za")){
                String[] stones={"raichunite_x","dragoninite","starminite","greninjite","zygardite","darkranite"};
                StorageProxy.getParty(p).get(4).setForm("complete");
                for(int i=0;i<stones.length;i++)StorageProxy.getParty(p).get(i).setHeldItem(new ItemStack(Backport.item(stones[i])));
                p.func_146105_b(new StringTextComponent("ZA 队伍已配好对应 Mega 石；基格尔德为完整形态。"),false);
            }
            return;
        }
        Map<String,Integer> za=new LinkedHashMap<>();za.put("raichux",26);za.put("raichuy",26);za.put("dragonite",149);za.put("starmie",121);za.put("greninja",658);za.put("zygarde",718);za.put("darkrai",491);
        Map<String,String> stones=new LinkedHashMap<>();stones.put("raichux","raichunite_x");stones.put("raichuy","raichunite_y");stones.put("dragonite","dragoninite");stones.put("starmie","starminite");stones.put("greninja","greninjite");stones.put("zygarde","zygardite");stones.put("darkrai","darkranite");
        Pokemon user=mon(za.containsKey(kind)?za.get(kind):kind.contains("star") || kind.equals("terapagos")?1024:kind.equals("ash")?658:kind.equals("mega") || kind.equals("z") || kind.equals("double")?6:143);
        Pokemon ally=mon(9),enemy=mon(143),other=mon(242);
        moves(user,kind.equals("ash")?"Water Shuriken":user.getSpecies().getDex()==1024?"Tera Starstorm":"Tera Blast","Flamethrower","Surf","Rock Slide");moves(ally,"Protect","Surf","Ice Beam","Tackle");moves(enemy,"Splash");moves(other,"Splash");
        if(kind.equals("ash")){user.setForm("battle_bond");moves(enemy,"Splash");enemy.setHealth(1);}
        if(za.containsKey(kind)){
            if(kind.equals("zygarde")){user.setForm("complete");moves(user,"Core Enforcer","Thousand Arrows","Dragon Pulse","Protect");}
            user.setHeldItem(new ItemStack(Backport.item(stones.get(kind))));
        }
        if(kind.equals("mega") || kind.equals("double") || kind.equals("z"))user.setHeldItem(new ItemStack(item(kind.equals("z")?"pixelmon:firium_z":"pixelmon:charizardite_x")));
        if(kind.equals("stellar"))Tera.changeType(user,Element.MYSTERY);else Tera.changeType(user,Element.WATER);
        StorageProxy.getParty(p).set(0,user);StorageProxy.getParty(p).set(1,ally);StorageProxy.getParty(p).set(2,mon(1024));
        p.field_71071_by.field_70462_a.set(0,new ItemStack(Backport.REGISTERED.get("tera_orb").get()));if(kind.equals("bagbattle")){
            bag(p);Tera.changeType(user,Element.WATER);
        }
        p.func_70012_b(.5,65,7.5,180,12);p.func_70634_a(.5,65,7.5);
        Entity a=user.getOrSpawnPixelmon(p),c=ally.getOrSpawnPixelmon(p),e=enemy.getOrSpawnPixelmon(p.field_70170_p,.5,65,-3.5),f=other.getOrSpawnPixelmon(p.field_70170_p,3.5,65,-3.5);Collections.addAll(testEntities,a,c,e,f);for(Entity entity:testEntities)if(entity!=null)entity.func_184211_a("backport_qa_fixture");
        BattleRules rules=new BattleRules(doub?BattleType.DOUBLE:BattleType.SINGLE);rules.set(BattleRuleRegistry.GEN_MODE,EnumOldGenMode.Mega);
        // Native Battle Bond checks that a target participant still has an able
        // Pokemon after the KO; provide a reserve for the Ash unlock fixture.
        BattleController b=BattleRegistry.startBattle(new BattleParticipant[]{new PlayerParticipant(p,doub?new Pokemon[]{user,ally}:new Pokemon[]{user})},new BattleParticipant[]{new WildPixelmonParticipant(doub || kind.equals("ash")?new Pokemon[]{enemy,other}:new Pokemon[]{enemy})},rules);
        if(b==null)throw new AssertionError("Map battle rejected "+kind);System.out.println("BACKPORT_QA_SCENARIO_START "+kind);
    }
    @SubscribeEvent public static void click(PlayerInteractEvent.RightClickBlock event){
        if(event.getWorld().field_72995_K || !(event.getPlayer() instanceof ServerPlayerEntity) || event.getHand()!=Hand.MAIN_HAND)return;
        ServerPlayerEntity p=(ServerPlayerEntity)event.getPlayer();if(!isMap(p))return;String action=stations.get(event.getPos());if(action==null)return;
        event.setCanceled(true);action(p,action);
    }
    @SubscribeEvent public static void stopped(net.minecraftforge.fml.event.server.FMLServerStoppedEvent event){prepared=false;mode=null;stations.clear();testEntities.clear();observed.clear();}
}
