import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.api.pokemon.PokemonFactory;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.client.gui.starter.ChooseStarterScreen;
import com.pixelmonmod.pixelmon.entities.npcs.NPCQuestGiver;
import com.pixelmonmod.pixelmon.entities.npcs.registry.ServerNPCRegistry;
import com.pixelmonmod.pixelmon.quests.*;
import com.pixelmonmod.pixelmon.quests.quest.Quest;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.*;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.minecraftforge.registries.ForgeRegistries;

/** Private interactive world. All submissions run through real native NPC dialogue/quest methods. */
@Mod("backport_npc_quest_test")
@Mod.EventBusSubscriber(modid="backport_npc_quest_test")
public final class NpcQuestTestMap {
    static final String WORLD="回迁NPC任务 1.4.5 手动验收",FOLDER="backport-npc-quests-145",PREFIX="npc145_case_";
    static final String[] TITLES={"01 整组：64交8","02 分栈：3＋5交8","03 不足：7不能交8","04 错球：超级球不能代替高级球","05 展示：只看不扣","06 双目标：高级8＋超级5","07 治疗仪：任意颜色","08 PC：任意颜色","09 批量制作：5个球","10 持有：合计8个球"};
    static final String[] COLORS={"white","red","blue","green","black","yellow","pink","purple"};
    static final String[] COLOR_NAMES={"白","红","蓝","绿","黑","黄","粉","紫"};
    static final Set<UUID> READY=java.util.concurrent.ConcurrentHashMap.newKeySet();
    static final Map<Integer,UUID> NPCS=new HashMap<>();
    static ServerWorld prepared;
    static boolean registered;

    static boolean isMap(ServerPlayerEntity p){return WORLD.equals(p.func_71121_q().func_73046_m().func_240793_aU_().func_76065_j());}
    static void tell(ServerPlayerEntity p,String s){p.func_146105_b(new StringTextComponent(s),false);}
    static void cmd(ServerPlayerEntity p,String s){p.func_71121_q().func_73046_m().func_195571_aL().func_197059_a(p.func_195051_bN().func_197033_a(4).func_197031_a(),s);}
    static void block(ServerWorld w,int x,int y,int z,String id){var b=ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id));if(b==null)throw new IllegalStateException("Missing map block "+id);w.func_180501_a(new BlockPos(x,y,z),b.func_176223_P(),3);}
    static void label(ServerWorld w,double x,double y,double z,String text){ArmorStandEntity a=new ArmorStandEntity(w,x,y,z);a.func_200203_b(new StringTextComponent(text));a.func_174805_g(true);a.func_82142_c(true);a.func_189654_d(true);a.func_184211_a("npc145_label");w.func_217376_c(a);}
    static JsonArray array(String... strings){JsonArray a=new JsonArray();for(String s:strings)a.add(s);return a;}
    static JsonObject stage(int id,int next,JsonArray objectives,JsonArray actions){JsonObject s=new JsonObject();s.addProperty("stage",id);s.addProperty("nextStage",next);s.add("objectives",objectives);s.add("actions",actions);return s;}
    static String ballArg(String type,int count){return "pixelmon:poke_ball "+count+" - - {\"PokeBallID\":\""+type+"\"}";}
    static String filename(int i){return PREFIX+i+".json";}

    static void register(){
        if(registered)return;
        for(int i=0;i<TITLES.length;i++){
            JsonObject q=new JsonObject();q.addProperty("weight",0);q.addProperty("repeatable",false);q.addProperty("abandonable",true);q.addProperty("activeStage",10);
            JsonArray stages=new JsonArray();stages.add(stage(0,10,array("BLOCKER"),array()));
            JsonArray objectives=new JsonArray(),actions=new JsonArray();
            String detail=i==1?"请提交8个高级球。3个和5个可以跨格合计。":i==2?"需要8个高级球；只有7个时不应扣物品。":i==3?"需要高级球。超级球属于错误球种，不应被扣除。":i==4?"请展示8个高级球。完成后物品数量不应减少。":i==5?"请分两次点击提交：高级球8个、超级球5个；完成的目标不会重复扣。":"请提交8个高级球。携带64个时只扣8个，保留56个。";
            if(i<6){
                objectives.add("DIALOGUE ?NPC? npcName request submit - - - - - - -");
                String type=i==4?"NPC_SHOW":"NPC_GIVE";
                objectives.add(type+" ?NPC? success "+ballArg("ultra_ball",8));
                if(i==5){objectives.add("NPC_GIVE ?NPC? success "+ballArg("great_ball",5));actions.add("+1,2 SET_STAGE 20 -");}
                else {objectives.add(type+" ?NPC? failure "+ballArg("ultra_ball",8));actions.add("1 SET_STAGE 20 -");actions.add("2 END_DIALOGUE");}
            }else if(i==6)objectives.add("BLOCK_USE pixelmon:red_healer 1 - - -");
            else if(i==7)objectives.add("BLOCK_USE pixelmon:red_pc 1 - - -");
            else if(i==8)objectives.add("ITEM_CRAFT "+ballArg("ultra_ball",5));
            else objectives.add("ITEM_HAS "+ballArg("ultra_ball",8));
            stages.add(stage(10,20,objectives,actions));
            stages.add(stage(20,-1,array("FOLLOWTHROUGH"),array("0 ITEM_GIVE minecraft:emerald 1 - - - - - - -","0 END_DIALOGUE")));
            q.add("stages",stages);JsonObject strings=new JsonObject();strings.addProperty("name",TITLES[i]);strings.addProperty("npcName",TITLES[i]);strings.addProperty("request",detail);strings.addProperty("submit",i==4?"（展示物品，不扣除）":"（提交所需物品）");strings.addProperty("desc-10",i<6?detail:i==6?"右键任意颜色的真实治疗仪。":i==7?"右键任意颜色的真实PC。":i==8?"在工作台制作高级球，按产出的球数累计到5个。":"随身持有至少8个高级球，多个格子和副手可以合计。数量不足不累计。" );strings.addProperty("desc-X","测试完成！获得一颗绿宝石。右键准备台可以重新测试。");strings.addProperty("desc-x",strings.get("desc-X").getAsString());strings.addProperty("stage-10-0",i<6?"右键对应NPC并选择提交":i==6?"使用治疗仪（任意颜色）":i==7?"使用PC（任意颜色）":i==8?"制作#个高级球":"持有#个高级球");strings.addProperty("stage-10-1",i==5?"提交8个高级球":"完成提交或展示");strings.addProperty("stage-10-2",i==5?"提交5个超级球":"数量不足时保留物品");q.add("strings",strings);
            Quest quest=QuestRegistry.GSON.fromJson(q,Quest.class);quest.setFilename(filename(i));QuestRegistry.getInstance().addQuest(quest);
        }
        registered=true;
    }

    @SubscribeEvent public static void start(net.minecraftforge.fml.event.server.FMLServerStartingEvent e){if(WORLD.equals(e.getServer().func_240793_aU_().func_76065_j()))register();}
    @SubscribeEvent public static void stopped(net.minecraftforge.fml.event.server.FMLServerStoppedEvent e){registered=false;prepared=null;NPCS.clear();READY.clear();}
    static void setup(ServerPlayerEntity p){
        ServerWorld w=p.func_71121_q();NPCS.clear();
        List<NPCQuestGiver> existing=w.func_175647_a(NPCQuestGiver.class,new AxisAlignedBB(-32,60,-28,32,80,28),n->n.getPersistentData().func_74767_n("Npc145Fixture"));
        for(NPCQuestGiver n:existing){n.setTextureIndex(-1);n.setCustomSteveTexture("scientist_m.png");NPCS.put(n.getPersistentData().func_74762_e("Npc145Case"),n.func_110124_au());}
        if(NPCS.size()<6){
            cmd(p,"fill -28 64 -24 28 64 24 minecraft:smooth_quartz");cmd(p,"fill -28 65 -24 28 73 24 minecraft:air");
            cmd(p,"fill -27 64 -23 27 64 23 minecraft:light_gray_concrete");cmd(p,"fill -2 64 -22 2 64 22 minecraft:polished_andesite");
            for(int x:new int[]{-27,27}){cmd(p,"fill "+x+" 65 -23 "+x+" 65 23 minecraft:sea_lantern");}
            label(w,0.5,72,-22,"重铸内容回迁 1.4.5 · NPC任务验收广场");
            label(w,0.5,70.7,-22,"先右键彩色准备台，再走到对应的真实NPC面前提交");
            label(w,0.5,69.5,-22,"成功获得1颗绿宝石；准备台只重置本地图测试任务与测试用品");
            for(int i=0;i<6;i++){
                int x=-20+i*8;
                cmd(p,"fill "+(x-2)+" 64 -5 "+(x+2)+" 64 1 minecraft:blue_concrete");
                cmd(p,"fill "+(x-2)+" 69 -5 "+(x+2)+" 69 -1 minecraft:light_blue_stained_glass");
                NPCQuestGiver npc=new NPCQuestGiver(w);var definitions=ServerNPCRegistry.getEnglishNPCs();if(!definitions.isEmpty())npc.init(definitions.get(i%definitions.size()));npc.setTextureIndex(-1);npc.setCustomSteveTexture("scientist_m.png");npc.setName(TITLES[i]);npc.func_200203_b(new StringTextComponent(TITLES[i]));npc.func_174805_g(true);npc.func_94061_f(true);npc.func_184224_h(true);npc.func_70012_b(x+.5,65,-2.5,0,0);npc.getPersistentData().func_74757_a("Npc145Fixture",true);npc.getPersistentData().func_74768_a("Npc145Case",i);w.func_217376_c(npc);NPCS.put(i,npc.func_110124_au());
                label(w,x+.5,68,-2.5,TITLES[i]);station(w,x,8,TITLES[i],"minecraft:yellow_concrete");
            }
            for(int i=0;i<8;i++){
                int x=-21+i*6;block(w,x,65,-17,"pixelmon:"+COLORS[i]+"_healer");label(w,x+.5,67,-16.5,COLOR_NAMES[i]+"色治疗仪");block(w,x,65,-12,"pixelmon:"+COLORS[i]+"_pc");label(w,x+.5,67,-11.5,COLOR_NAMES[i]+"色PC");
            }
            for(int i=6;i<10;i++)station(w,-20+(i-6)*10,16,TITLES[i],"minecraft:lime_concrete");
            station(w,20,16,"返回入口 / 查看说明","minecraft:cyan_concrete");
            block(w,14,65,16,"minecraft:crafting_table");label(w,14.5,67,16.5,"批量制球用工作台");
        }
        prepared=w;
        System.out.println("NPC145_MAP_LAYOUT_READY npcs="+NPCS.size());
    }
    static void station(ServerWorld w,int x,int z,String title,String color){block(w,x,65,z,color);label(w,x+.5,66.8,z+.5,title);label(w,x+.5,66.4,z+.5,"空手右键此色块准备");}
    static ItemStack item(String id,int count){Item v=ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));if(v==null)throw new IllegalStateException(id);ItemStack s=new ItemStack(v,count);s.func_196082_o().func_74757_a("Npc145Supply",true);return s;}
    static ItemStack ball(String type,int count){ItemStack s=item("pixelmon:poke_ball",count);s.func_196082_o().func_74778_a("PokeBallID",type);return s;}
    static void supplies(ServerPlayerEntity p,ItemStack... items){
        for(List<ItemStack> list:Arrays.asList(p.field_71071_by.field_70462_a,p.field_71071_by.field_184439_c))for(int i=0;i<list.size();i++){ItemStack s=list.get(i);if(s.func_77942_o()&&s.func_77978_p().func_74767_n("Npc145Supply"))list.set(i,ItemStack.field_190927_a);}
        for(ItemStack stack:items){boolean placed=false;for(int i=0;i<p.field_71071_by.field_70462_a.size();i++)if(p.field_71071_by.field_70462_a.get(i).func_190926_b()){p.field_71071_by.field_70462_a.set(i,stack);placed=true;break;}if(!placed)throw new IllegalStateException("Test inventory full");}
        p.field_71071_by.func_70296_d();p.field_71069_bz.func_75142_b();
    }
    static QuestProgress reset(ServerPlayerEntity p,int i){
        var data=StorageProxy.getParty(p).getQuestData(true);data.getProgress().removeIf(q->q.getQuestFilename().equals(filename(i)));
        QuestProgress q=new QuestProgress(QuestRegistry.getInstance().getQuest(filename(i)),data);data.getProgress().add(q);q.start();if(i<6)q.setData("NPC",NPCS.get(i).toString());q.setStage((short)10);q.sendTo(p);data.setTracking(q);return q;
    }
    static void activate(ServerPlayerEntity p,int i){
        reset(p,i);
        if(i==0||i==4)supplies(p,ball("ultra_ball",64));
        else if(i==1)supplies(p,ball("ultra_ball",3),ball("ultra_ball",5));
        else if(i==2)supplies(p,ball("ultra_ball",7));
        else if(i==3)supplies(p,ball("great_ball",64));
        else if(i==5)supplies(p,ball("ultra_ball",64),ball("great_ball",64));
        else if(i==6||i==7)supplies(p);
        else if(i==8)supplies(p,item("pixelmon:cooked_black_apricorn",32),item("pixelmon:cooked_yellow_apricorn",16),item("pixelmon:platinum_base",16),item("minecraft:stone_button",16));
        else supplies(p,ball("ultra_ball",3),ball("ultra_ball",5));
        tell(p,"已准备："+TITLES[i]+"。"+(i<6?"前方相同编号NPC使用真实对话提交。":"北侧机器区或工作台可以完成此目标。"));
        if(i==8)tell(p,"原版配方：熟黑球果＋熟黄球果＋熟黑球果横排做高级球盖；球盖＋铂金底座＋石头按钮合成高级球，可Shift批量。用品已放入背包。");
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||ServerLifecycleHooks.getCurrentServer()==null)return;
        for(ServerPlayerEntity p:ServerLifecycleHooks.getCurrentServer().func_184103_al().func_181057_v())if(isMap(p)){
            register();if(prepared!=p.func_71121_q())setup(p);
            if(READY.add(p.func_110124_au())){
                StorageProxy.getParty(p).starterPicked=true;
                if(!p.getPersistentData().func_74767_n("Npc145Visited")){
                    cmd(p,"gamemode survival");cmd(p,"tp @s 0.5 65 21.5 180 8");
                    var mon=PokemonFactory.create(PixelmonSpecies.fromNationalDex(25));mon.setLevelNum(50);mon.heal();mon.setHealth(1);StorageProxy.getParty(p).set(0,mon);
                    activate(p,0);p.getPersistentData().func_74757_a("Npc145Visited",true);
                }
                tell(p,"欢迎来到1.4.5任务测试广场！黄色台：提交对照；绿色台：机器/计数。只准备对象，不替你点击提交。成功后获得绿宝石。");
                System.out.println("NPC145_MAP_PLAYER_READY "+p.func_110124_au());
            }
        }
    }
    @SubscribeEvent public static void click(PlayerInteractEvent.RightClickBlock e){
        if(e.getWorld().field_72995_K||e.getHand()!=Hand.MAIN_HAND||!(e.getPlayer() instanceof ServerPlayerEntity))return;
        ServerPlayerEntity p=(ServerPlayerEntity)e.getPlayer();if(!isMap(p)||e.getPos().func_177956_o()!=65)return;
        int x=e.getPos().func_177958_n(),z=e.getPos().func_177952_p();
        if(z==8)for(int i=0;i<6;i++)if(x==-20+i*8){e.setCanceled(true);activate(p,i);return;}
        if(z==16){for(int i=6;i<10;i++)if(x==-20+(i-6)*10){e.setCanceled(true);activate(p,i);return;}if(x==20){e.setCanceled(true);cmd(p,"tp @s 0.5 65 21.5 180 8");tell(p,"测试台只重置本地图对应测试任务和带测试标记的用品；正常任务与已获得绿宝石保留。按L打开任务日志。");}}
    }
    @Mod.EventBusSubscriber(modid="backport_npc_quest_test",value=net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static final class Client {
        static boolean opened,captured;static int ticks;
        @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
            if(e.phase!=TickEvent.Phase.END)return;
            var mc=net.minecraft.client.Minecraft.func_71410_x();
            if(!opened&&FOLDER.equals(System.getProperty("backport.manual.world"))&&mc.field_71462_r instanceof net.minecraft.client.gui.screen.MainMenuScreen){opened=true;mc.func_238191_a_(FOLDER);return;}
            if(mc.field_71439_g==null||!READY.contains(mc.field_71439_g.func_110124_au()))return;
            if(mc.field_71462_r instanceof ChooseStarterScreen)mc.func_147108_a(null);
            if(!captured&&mc.field_71462_r==null&&++ticks>=160){
                captured=true;try(var image=net.minecraft.util.ScreenShotHelper.func_198052_a(mc.func_228018_at_().func_198105_m(),mc.func_228018_at_().func_198083_n(),mc.func_147110_a())){
                    image.func_209270_a(Paths.get("npc-quest-145-preview.png"));System.out.println("NPC145_MAP_PREVIEW_READY");
                }catch(Exception failure){failure.printStackTrace();}
            }
        }
    }
}
