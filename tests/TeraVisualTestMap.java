import java.util.*;
import com.google.gson.JsonParser;
import local.pixelmon.backport.*;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.battles.BattleRegistry;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.client.gui.starter.ChooseStarterScreen;
import com.pixelmonmod.pixelmon.client.ServerStorageDisplay;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import com.pixelmonmod.pixelmon.enums.EnumMegaItem;
import com.pixelmonmod.pixelmon.enums.EnumGrowth;
import com.pixelmonmod.pixelmon.api.pokemon.species.gender.Gender;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.minecraftforge.registries.ForgeRegistries;

/** Separate, opt-in manual visual fixtures. Never installed into normal gameplay. */
@Mod("backport_tera_visual_test")
@Mod.EventBusSubscriber(modid="backport_tera_visual_test")
public final class TeraVisualTestMap {
    static final String WORLD="回迁太晶外观 1.4.5 手动验收",FOLDER="backport-tera-visuals-145",TAG="TeraVisualFixture";
    static final String[] TYPES={"一般","火","水","草","电","冰","格斗","毒","地面","飞行","超能力","虫","岩石","幽灵","龙","恶","钢","妖精","星晶"};
    static final int[] DEX={6,1,95,146,197,321};
    static final int[] TYPE={1,3,12,9,15,2};
    static ServerWorld prepared;static boolean show=true;static final Set<UUID> READY=java.util.concurrent.ConcurrentHashMap.newKeySet();
    static final Set<UUID> IN_BATTLE=new HashSet<>();
    static volatile String review="";
    // Developer screenshot control is restricted to an explicitly opened manual
    // map and a local request file. It never runs in a distributed normal world.
    static void review(ServerPlayerEntity p){
        if(!FOLDER.equals(System.getProperty("backport.manual.world")) || p.field_70173_aa<200 || p.field_70173_aa%20!=0)return;
        var request=java.nio.file.Paths.get("tera-review-request.json");if(!java.nio.file.Files.isRegularFile(request))return;
        try{
            var q=new JsonParser().parse(java.nio.file.Files.readString(request)).getAsJsonObject();
            String id=q.get("id").getAsString();if(id.equals(review) || !id.matches("[a-zA-Z0-9_-]+"))return;
            for(PixelmonEntity old:p.func_71121_q().func_175647_a(PixelmonEntity.class,new AxisAlignedBB(-20,60,16,20,90,25),v->v.getPersistentData().func_74767_n("TeraVisualReview")))old.func_70106_y();
            int dex=q.get("dex").getAsInt(),type=q.get("type").getAsInt();boolean shiny=q.has("shiny") && q.get("shiny").getAsBoolean();
            double gap=dex==6?3:1.3,distance=dex==6?8:4;
            for(int i=0;i<2;i++){
                PixelmonEntity actor=display(p.func_71121_q(),dex,"",shiny,i==0?-1:type,i==0?-gap:gap,21.5);
                actor.getPersistentData().func_74757_a("TeraVisualReview",true);
                actor.func_70012_b(i==0?-gap:gap,65,21.5,180,0);
            }
            cmd(p,"tp @s 0 "+(dex==6?66:65)+" "+(21.5-distance)+" 0 "+(dex==6?8:5));
            review=id;System.out.println("TERA_VISUAL_REVIEW_READY "+id);
        }catch(Exception failure){failure.printStackTrace();}
    }
    static boolean map(ServerPlayerEntity p){return WORLD.equals(p.func_184102_h().func_240793_aU_().func_76065_j());}
    static void cmd(ServerPlayerEntity p,String command){p.func_184102_h().func_195571_aL().func_197059_a(p.func_195051_bN().func_197033_a(4).func_197031_a(),command);}
    static void tell(ServerPlayerEntity p,String text){p.func_145747_a(new StringTextComponent(text),new UUID(0,0));}
    static Pokemon pokemon(int dex,String form,boolean shiny){
        Pokemon mon=PokemonFactory.create(PixelmonSpecies.fromNationalDex(dex));if(!form.isEmpty())mon.setForm(form);
        mon.setLevelNum(60);mon.initialize();mon.setGrowth(EnumGrowth.Ordinary);if(dex==25 || dex==CapSpecies.DEX)mon.setGender(Gender.MALE);mon.setShiny(shiny);mon.heal();return mon;
    }
    static PixelmonEntity display(ServerWorld world,int dex,String form,boolean shiny,int type,double x,double z){
        PixelmonEntity entity=new PixelmonEntity(world,pokemon(dex,form,shiny));
        entity.func_70012_b(x,65,z,0,0);entity.func_94061_f(true);entity.func_184224_h(true);entity.func_189654_d(true);entity.func_110163_bv();
        entity.getPersistentData().func_74757_a(TAG,true);entity.getPersistentData().func_74768_a("TeraVisualFixtureType",type);
        world.func_217376_c(entity);return entity;
    }
    static void label(ServerWorld w,double x,double y,double z,String text){
        ArmorStandEntity a=new ArmorStandEntity(w,x,y,z);a.func_82142_c(true);a.func_189654_d(true);a.func_184224_h(true);a.func_200203_b(new StringTextComponent(text));a.func_174805_g(true);w.func_217376_c(a);
    }
    static void block(ServerWorld w,int x,int y,int z,String id){w.func_180501_a(new BlockPos(x,y,z),ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id)).func_176223_P(),3);}
    static void station(ServerWorld w,int x,String color,String name){block(w,x,65,27,"minecraft:"+color+"_concrete");label(w,x+.5,67,27.5,name);}
    static void controls(ServerWorld w,int z){
        int[] xs={-16,-8,0,8,16,24};String[] colors={"magenta","cyan","lime","red","yellow","orange"};
        String[] names={"右键：返回展示区入口","右键：准备实战队伍","右键：准备野生对手","右键：结束实战并充能","右键：切换展示外观","右键：重播帽皮变身演示"};
        for(int i=0;i<xs.length;i++){
            String id="minecraft:"+colors[i]+"_concrete";BlockPos pos=new BlockPos(xs[i],65,z);
            if(w.func_180495_p(pos).func_177230_c()!=ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id))){block(w,xs[i],65,z,id);label(w,xs[i]+.5,67,z+.5,names[i]);}
        }
    }
    static void entrance(ServerPlayerEntity p){cmd(p,"tp @s 0.5 65 33.5 180 5");}
    static void setup(ServerPlayerEntity p){
        ServerWorld w=p.func_71121_q();
        if(!p.func_184102_h().func_240793_aU_().func_76065_j().equals(WORLD))return;
        // Clean initial world has no chunks. A world-level saved flag lives in a
        // dedicated fixture marker, so rejoining does not duplicate displays.
        List<ArmorStandEntity> markers=w.func_175647_a(ArmorStandEntity.class,new AxisAlignedBB(-2,64,28,2,75,32),a->a.getPersistentData().func_74767_n("TeraVisualMapMarker"));
        if(markers.isEmpty()){
            cmd(p,"fill -40 64 -96 40 64 36 minecraft:smooth_quartz");
            cmd(p,"fill -2 64 -92 2 64 34 minecraft:polished_andesite");
            for(int x:new int[]{-39,39})cmd(p,"fill "+x+" 64 -95 "+x+" 64 35 minecraft:sea_lantern");
            label(w,.5,71,25,"1.4.5 太晶外观 · 19属性与原体型对照");
            label(w,.5,69.8,25,"前方冠冕展示；两侧同种对照；后方实战区。身体不放大。");
            for(int i=0;i<19;i++){
                int x=-12+(i%5)*6,z=17-(i/5)*9;
                display(w,25,"",i%2==1,i,x+.5,z+.5);label(w,x+.5,68,z+.5,TYPES[i]+"太晶"+(i%2==1?" · 异色":""));
            }
            for(int i=0;i<DEX.length;i++){
                double x=i%2==0?-26:26,z=12-(i/2)*28;
                display(w,DEX[i],"",i%2==1,-1,x-5,z);display(w,DEX[i],"",i%2==1,TYPE[i],x+5,z);
                label(w,x,72,z,"全国编号 "+DEX[i]+"：左原型 / 右太晶，同一体型");
            }
            for(int i=0;i<4;i++)display(w,1017,new String[]{"terateal","terawellspring","terahearthflame","teracornerstone"}[i],false,new int[]{3,2,1,12}[i],-12+i*6,-25);
            label(w,-3,70,-25,"厄诡椪四种特殊太晶形态（保留自身面具）");
            display(w,1024,"stellar",false,18,12,-31);label(w,12,72,-31,"太乐巴戈斯星晶形态（保留专属模型）");
            for(int shiny=0;shiny<2;shiny++){
                int[] types={-1,4,1,18};
                for(int i=0;i<types.length;i++)display(w,CapSpecies.DEX,"",shiny==1,types[i],-9+i*6,-40-shiny*9);
                label(w,0,70,-40-shiny*9,"自制戴帽皮卡丘"+(shiny==1?"（异色）":"（正常）")+"：原型 / 电太晶 / 火太晶 / 星晶");
            }
            station(w,-8,"cyan","右键：准备实战队伍与太晶珠");station(w,0,"lime","右键：在后方准备野生对手");station(w,8,"red","右键：结束实战 / 恢复充能");station(w,16,"yellow","右键：切换展示外观");
            label(w,.5,69,-60,"实战区：自己抛出队伍宝可梦进入对战，点太晶按钮，检查换人/结束清理");
            ArmorStandEntity marker=new ArmorStandEntity(w,.5,65,30.5);marker.func_82142_c(true);marker.func_189654_d(true);marker.getPersistentData().func_74757_a("TeraVisualMapMarker",true);w.func_217376_c(marker);
        }
        controls(w,27);controls(w,-53);
        if(w.func_175647_a(PixelmonEntity.class,new AxisAlignedBB(-1,64,19,8,73,25),v->v.getPersistentData().func_74767_n("TeraVisualDemo")).isEmpty()){
            PixelmonEntity demo=display(w,CapSpecies.DEX,"",false,-1,4.5,21.5);demo.getPersistentData().func_74757_a("TeraVisualDemo",true);label(w,4.5,68,21.5,"帽皮变身演示：橙台可反复重播");
        }
        prepared=w;System.out.println("TERA_VISUAL_MAP_LAYOUT_READY controls=entrance+arena");
    }
    static void team(ServerPlayerEntity p){
        if(BattleRegistry.getBattle(p)!=null){tell(p,"先结束当前对战，再准备队伍。");return;}
        var party=StorageProxy.getParty(p);party.unlockMega(true);party.setMegaItem(EnumMegaItem.BraceletORAS,true);
        int[] ids={CapSpecies.DEX,6,1,95,1017,1024};
        for(int i=0;i<6;i++){
            Pokemon old=party.get(i);if(old!=null)old.getPixelmonEntity().ifPresent(e->e.retrieve("TeraVisualMapReset"));
            Pokemon mon=pokemon(ids[i],i==4?"teal":"",i==2);if(i<4)Tera.changeType(mon,Tera.TYPES[i==0?4:TYPE[i]]);
            if(i==0){mon.getMoveset().clear();for(String move:new String[]{"Ten Million Volts","Tera Blast","Protect","Thunderbolt"})mon.getMoveset().add(new Attack(move));}
            party.set(i,mon);
        }
        if(!TeraItems.hasAnyOrb(p))p.field_71071_by.func_70441_a(new ItemStack(Backport.item("tera_orb")));
        TeraItems.recharge(p);tell(p,"已准备六只实战队友与太晶珠。绿台生成野生吉利蛋；到后方自行抛出宝可梦进入战斗并点击太晶化。红台结束战斗并充能。");
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){
        if(e.phase!=TickEvent.Phase.END || ServerLifecycleHooks.getCurrentServer()==null)return;
        for(ServerPlayerEntity p:ServerLifecycleHooks.getCurrentServer().func_184103_al().func_181057_v())if(map(p)){
            if(prepared!=p.func_71121_q())setup(p);
            StorageProxy.getParty(p).starterPicked=true;
            if(READY.add(p.func_110124_au())){cmd(p,"gamemode creative");cmd(p,"tp @s 0.5 65 33.5 180 5");tell(p,"太晶外观测试地图已就绪。展示对象由独立地图模组控制；实战队伍仍走真实太晶机制。请检查体型、冠冕位置、异色和结束后的清理。");}
            if(BattleRegistry.getBattle(p)!=null)IN_BATTLE.add(p.func_110124_au());
            else if(IN_BATTLE.remove(p.func_110124_au())){entrance(p);tell(p,"实战结束，已返回控制台；实战区也有完整控制台，紫台随时返回入口。");}
            if(p.field_70173_aa%100==0){controls(p.func_71121_q(),27);controls(p.func_71121_q(),-53);}
            review(p);
        }
        if(prepared!=null)for(PixelmonEntity entity:prepared.func_175647_a(PixelmonEntity.class,new AxisAlignedBB(-40,60,-96,40,100,36),v->v.getPersistentData().func_74767_n(TAG)))
            ((TeraVisualState)entity).backport$setTeraVisualType(show?entity.getPersistentData().func_74762_e("TeraVisualFixtureType"):-1);
    }
    @SubscribeEvent public static void click(PlayerInteractEvent.RightClickBlock e){
        if(e.getWorld().field_72995_K || e.getHand()!=Hand.MAIN_HAND || !(e.getPlayer() instanceof ServerPlayerEntity))return;
        ServerPlayerEntity p=(ServerPlayerEntity)e.getPlayer();if(!map(p) || e.getPos().func_177956_o()!=65 || (e.getPos().func_177952_p()!=27 && e.getPos().func_177952_p()!=-53))return;
        int x=e.getPos().func_177958_n();if(x==-8){e.setCanceled(true);team(p);}else if(x==0){
            e.setCanceled(true);if(BattleRegistry.getBattle(p)!=null)return;
            for(PixelmonEntity old:p.func_71121_q().func_175647_a(PixelmonEntity.class,new AxisAlignedBB(-10,60,-75,10,80,-55),v->v.getPersistentData().func_74767_n("TeraVisualOpponent")))old.func_70106_y();
            Pokemon mon=pokemon(242,"",false);mon.getMoveset().clear();mon.getMoveset().add(new Attack("Splash"));
            PixelmonEntity target=mon.getOrCreatePixelmon(p.field_70170_p,.5,65,-65.5);target.func_94061_f(true);target.getPersistentData().func_74757_a("TeraVisualOpponent",true);p.field_70170_p.func_217376_c(target);cmd(p,"tp @s 0.5 65 -57.5 180 5");tell(p,"野生吉利蛋已在前方准备，请自己抛出宝可梦开始实战。背后为实战控制台；实战结束后自动回到入口。");
        }else if(x==8){e.setCanceled(true);var battle=BattleRegistry.getBattle(p);if(battle!=null)battle.endBattleWithoutXP();TeraItems.recharge(p);tell(p,"已结束实战并充能，请确认太晶外观消失。");}
        else if(x==16){e.setCanceled(true);show=!show;tell(p,show?"展示外观已开启":"展示外观已关闭，可比较原始体型；实战太晶状态不受影响");}
        else if(x==-16){e.setCanceled(true);if(BattleRegistry.getBattle(p)==null)entrance(p);}
        else if(x==24){e.setCanceled(true);if(BattleRegistry.getBattle(p)!=null)return;show=true;
            for(PixelmonEntity demo:p.func_71121_q().func_175647_a(PixelmonEntity.class,new AxisAlignedBB(-1,64,19,8,73,25),v->v.getPersistentData().func_74767_n("TeraVisualDemo"))){
                demo.getPersistentData().func_74768_a("TeraVisualFixtureType",4);((TeraVisualState)demo).backport$setTeraVisualType(4);((TeraVisualState)demo).backport$setTeraVisualStart(p.field_70170_p.func_82737_E());
            }cmd(p,"tp @s -1.5 65 21.5 -90 8");tell(p,"重播帽皮变身：原版进化光束、聚光、结晶与冠冕。此为单独的外观演示，不冒充真实战斗。");
        }
    }
    @SubscribeEvent public static void stopped(net.minecraftforge.fml.event.server.FMLServerStoppedEvent e){prepared=null;show=true;READY.clear();IN_BATTLE.clear();}
    @Mod.EventBusSubscriber(modid="backport_tera_visual_test",value=net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static final class Client {
        static boolean opened,captured,privateSized;static int previewTicks;static String reviewSeen="";static int reviewFrames;
        static boolean ready(){var mc=net.minecraft.client.Minecraft.func_71410_x();return mc.field_71439_g!=null && READY.contains(mc.field_71439_g.func_110124_au());}
        @SubscribeEvent public static void key(net.minecraftforge.client.event.InputEvent.KeyInputEvent e){
            var mc=net.minecraft.client.Minecraft.func_71410_x();if(ready() && e.getKey()==297 && e.getAction()==1 && mc.field_71462_r==null)mc.func_147108_a(new TeraVisualPreviewScreen());
        }
        @SubscribeEvent public static void chat(net.minecraftforge.client.event.ClientChatEvent e){
            if(ready() && (e.getMessage().equalsIgnoreCase("/terapreview") || e.getMessage().equals("/太晶预览"))){e.setCanceled(true);net.minecraft.client.Minecraft.func_71410_x().func_147108_a(new TeraVisualPreviewScreen());}
        }
        @SubscribeEvent public static void starter(net.minecraftforge.client.event.GuiOpenEvent e){
            if(ready() && e.getGui() instanceof ChooseStarterScreen){ServerStorageDisplay.showStarterScreen=false;e.setCanceled(true);}
        }
        @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
            if(e.phase!=TickEvent.Phase.END)return;var mc=net.minecraft.client.Minecraft.func_71410_x();
            if(java.nio.file.Files.isRegularFile(java.nio.file.Paths.get("tera-review-private"))){
                org.lwjgl.glfw.GLFW.glfwHideWindow(mc.func_228018_at_().func_198092_i());
                if(!privateSized){org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.func_228018_at_().func_198092_i(),1280,800);privateSized=true;}
            }
            if(!opened && FOLDER.equals(System.getProperty("backport.manual.world")) && mc.field_71462_r instanceof net.minecraft.client.gui.screen.MainMenuScreen){opened=true;mc.func_238191_a_(FOLDER);return;}
            if(ready()){
                if(java.nio.file.Files.isRegularFile(java.nio.file.Paths.get("tera-review-private"))){
                    org.lwjgl.glfw.GLFW.glfwHideWindow(mc.func_228018_at_().func_198092_i());
                    mc.field_71474_y.field_74319_N=true;
                }
                if(previewTicks%100==0)mc.func_228018_at_().func_230148_b_("宝可梦：轻快之旅 | 太晶外观图鉴 r10 · F8");
                if(previewTicks==180 && java.nio.file.Files.isRegularFile(java.nio.file.Paths.get("tera-preview-auto")))mc.func_147108_a(new TeraVisualPreviewScreen());
                if(previewTicks<200)previewTicks++;
                ServerStorageDisplay.showStarterScreen=false;
                if(mc.field_71462_r instanceof ChooseStarterScreen)mc.func_147108_a(null);
                if(!review.equals(reviewSeen)){reviewSeen=review;reviewFrames=0;}
                if(!reviewSeen.isEmpty() && (reviewFrames==40 || reviewFrames==100)){
                    try(var image=net.minecraft.util.ScreenShotHelper.func_198052_a(mc.func_228018_at_().func_198105_m(),mc.func_228018_at_().func_198083_n(),mc.func_147110_a())){
                        image.func_209270_a(java.nio.file.Paths.get("tera-review-"+reviewSeen+"-"+reviewFrames+".png"));System.out.println("TERA_VISUAL_REVIEW_CAPTURE "+reviewSeen+" "+reviewFrames);
                    }catch(Exception failure){failure.printStackTrace();}
                }
                if(!reviewSeen.isEmpty())reviewFrames++;
                if(!captured && FOLDER.equals(System.getProperty("backport.manual.world")) && mc.field_71462_r==null && ++previewTicks>=140){
                    captured=true;
                    try(var image=net.minecraft.util.ScreenShotHelper.func_198052_a(mc.func_228018_at_().func_198105_m(),mc.func_228018_at_().func_198083_n(),mc.func_147110_a())){
                        image.func_209270_a(java.nio.file.Paths.get("tera-visual-r4-preview.png"));System.out.println("TERA_VISUAL_CLIENT_PREVIEW_READY");
                    }catch(Exception failure){failure.printStackTrace();}
                }
            }
        }
    }
}
