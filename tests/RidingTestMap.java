import java.util.*;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import com.pixelmonmod.pixelmon.client.gui.starter.ChooseStarterScreen;
import com.pixelmonmod.pixelmon.client.ServerStorageDisplay;
import com.pixelmonmod.pixelmon.enums.EnumGrowth;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.network.play.server.SEntityTeleportPacket;
import net.minecraft.network.play.server.SSetPassengersPacket;
import net.minecraft.command.Commands;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

/** Separate helper, active only in the named disposable riding world. */
@Mod("backport_riding_test")
@Mod.EventBusSubscriber(modid="backport_riding_test")
public class RidingTestMap {
    static final String FOLDER="backport-riding-150",WORLD="轻快之旅 1.5.0 乘骑测试";
    static final int[] DEX={6,78,131,95,321,1026};
    static final String[] NAMES={"喷火龙：飞行","烈焰马：地面","拉普拉斯：水上","大岩蛇：长体型","吼鲸王：大体型","戴帽子的皮卡丘：资格对照"};
    static final Set<UUID> READY=new HashSet<>();static net.minecraft.world.server.ServerWorld prepared;
    static final Map<UUID,Resync> RESYNC=new HashMap<>();
    static class Resync {PixelmonEntity entity;int tick;Resync(PixelmonEntity e,int tick){entity=e;this.tick=tick;}}
    static boolean map(ServerPlayerEntity p){return WORLD.equals(p.func_184102_h().func_240793_aU_().func_76065_j());}
    static void cmd(ServerPlayerEntity p,String text){p.func_184102_h().func_195571_aL().func_197059_a(p.func_195051_bN().func_197033_a(4).func_197031_a(),text);}
    static void tell(ServerPlayerEntity p,String text){p.func_145747_a(new StringTextComponent(text),new UUID(0,0));}
    static void prepare(ServerPlayerEntity p){
        var w=p.func_71121_q();prepared=w;
        for(int x=-48;x<=48;x++)for(int z=-52;z<=52;z++)w.func_180501_a(new BlockPos(x,64,z),Blocks.field_150348_b.func_176223_P(),2);
        for(int x=-36;x<=-8;x++)for(int z=-42;z<=-16;z++)for(int y=60;y<=64;y++)w.func_180501_a(new BlockPos(x,y,z),(y==60?Blocks.field_150348_b:Blocks.field_150355_j).func_176223_P(),2);
        // Three-block clearance: collision still uses the original entity boxes.
        for(int x=30;x<=42;x++)for(int z=-8;z<=12;z++)w.func_180501_a(new BlockPos(x,68,z),Blocks.field_150348_b.func_176223_P(),2);
        for(int i=0;i<DEX.length;i++)w.func_180501_a(new BlockPos(-20+i*8,65,16),Blocks.field_150340_R.func_176223_P(),2);
        cmd(p,"time set day");cmd(p,"gamerule doDaylightCycle false");cmd(p,"gamerule doMobSpawning false");cmd(p,"gamerule keepInventory true");
    }
    static PixelmonEntity select(ServerPlayerEntity p,int index,boolean mount){
        if(p.func_184187_bx() instanceof PixelmonEntity)local.pixelmon.backport.RidingMotion.clear((PixelmonEntity)p.func_184187_bx());
        p.func_233575_bb_();var mon=StorageProxy.getParty(p).get(index);mon.heal();
        double x=index==2?-22:0,z=index==2?-28:0,y=index==2?64:65;
        cmd(p,"tp @s "+x+" 65 "+(z+3)+" 0 10");
        var e=mon.getOrCreatePixelmon(p.func_71121_q(),x,y,z);e.func_70012_b(x,y,z,0,0);e.func_213293_j(0,0,0);e.setFlying(false);e.func_189654_d(false);e.func_94061_f(true);
        e.func_70606_j(e.func_110138_aP());
        if(p.func_71121_q().func_73045_a(e.func_145782_y())==null)p.func_71121_q().func_217376_c(e);
        local.pixelmon.backport.RidingMotion.clear(e);
        // A teleport packet must reach the client before its passenger packet;
        // otherwise the client attaches to the entity at its old location.
        p.field_71135_a.func_147359_a(new SEntityTeleportPacket(e));
        if(mount&&e.getForm().getMovement().isRideable()){
            if(!p.func_184205_a(e,true))tell(p,"坐骑切换未成功，请下骑后重试。");
            else{e.func_184232_k(p);p.field_71135_a.func_147359_a(new SSetPassengersPacket(e));RESYNC.put(p.func_110124_au(),new Resync(e,p.field_70173_aa+5));}
        }
        return e;
    }
    @SubscribeEvent public static void commands(RegisterCommandsEvent e){e.getDispatcher().register(Commands.func_197057_a("ridingtest").then(Commands.func_197056_a("number",IntegerArgumentType.integer(1,6)).executes(ctx->{var p=ctx.getSource().func_197035_h();if(!map(p))return 0;select(p,IntegerArgumentType.getInteger(ctx,"number")-1,true);return 1;})));}
    @SubscribeEvent public static void stopped(net.minecraftforge.fml.event.server.FMLServerStoppedEvent e){READY.clear();RESYNC.clear();prepared=null;}
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;var s=ServerLifecycleHooks.getCurrentServer();if(s==null)return;
        for(var p:s.func_184103_al().func_181057_v())if(map(p)){
            Resync sync=RESYNC.get(p.func_110124_au());if(sync!=null&&p.field_70173_aa>=sync.tick){RESYNC.remove(p.func_110124_au());if(p.func_184187_bx()==sync.entity&&sync.entity.func_70089_S()){p.field_71135_a.func_147359_a(new SEntityTeleportPacket(sync.entity));p.field_71135_a.func_147359_a(new SSetPassengersPacket(sync.entity));}}
            if(prepared!=p.func_71121_q())prepare(p);
            if(READY.add(p.func_110124_au())){
                var party=StorageProxy.getParty(p);party.starterPicked=true;
                for(int i=0;i<DEX.length;i++){var mon=PokemonFactory.create(PixelmonSpecies.fromNationalDex(DEX[i]));mon.setLevelNum(60);mon.initialize();mon.setGrowth(EnumGrowth.Ordinary);mon.heal();party.set(i,mon);
                    var e=mon.getOrCreatePixelmon(p.func_71121_q(),-20+i*8,65,10);e.func_94061_f(true);e.func_110163_bv();if(p.func_71121_q().func_73045_a(e.func_145782_y())==null)p.func_71121_q().func_217376_c(e);
                    tell(p,"金块 "+(i+1)+"："+NAMES[i]+"；可骑="+e.getForm().getMovement().isRideable()+"，可飞="+e.canFly());
                }
                cmd(p,"gamemode creative");cmd(p,"tp @s 0 65 22 180 12");tell(p,"右击金块或 /ridingtest 1~6 选择坐骑（4 为大岩蛇）。空格上升/起跳，原下降键下降，Shift 下骑。东侧低顶，西北水池。测试说明只在本地图。请给游戏窗口焦点后操作。");
            }
        }
    }
    @SubscribeEvent public static void click(PlayerInteractEvent.RightClickBlock event){
        if(!(event.getPlayer() instanceof ServerPlayerEntity))return;var p=(ServerPlayerEntity)event.getPlayer();if(!map(p)||event.getHand()!=net.minecraft.util.Hand.MAIN_HAND)return;
        BlockPos pos=event.getPos();if(pos.func_177956_o()!=65||pos.func_177952_p()!=16)return;
        int index=(pos.func_177958_n()+20)/8;if(index<0||index>=DEX.length||-20+index*8!=pos.func_177958_n())return;
        var e=select(p,index,true);tell(p,NAMES[index]+(e.getForm().getMovement().isRideable()?" 已准备。":" 原本不可乘骑，保持不变。"));event.setCanceled(true);
    }
    @Mod.EventBusSubscriber(modid="backport_riding_test",value=net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static class Client {
        static boolean opened;
        static boolean ready(){var mc=net.minecraft.client.Minecraft.func_71410_x();return mc.field_71439_g!=null&&READY.contains(mc.field_71439_g.func_110124_au());}
        @SubscribeEvent public static void starter(net.minecraftforge.client.event.GuiOpenEvent e){if(ready()&&e.getGui() instanceof ChooseStarterScreen){ServerStorageDisplay.showStarterScreen=false;e.setCanceled(true);}}
        @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){if(e.phase!=TickEvent.Phase.END)return;var mc=net.minecraft.client.Minecraft.func_71410_x();if(!opened&&FOLDER.equals(System.getProperty("backport.manual.world"))&&mc.field_71462_r instanceof net.minecraft.client.gui.screen.MainMenuScreen){opened=true;mc.func_238191_a_(FOLDER);}if(ready()){ServerStorageDisplay.showStarterScreen=false;if(mc.field_71462_r instanceof ChooseStarterScreen)mc.func_147108_a(null);}}
    }
}
