package qa;
import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import com.pixelmonmod.pixelmon.api.storage.*;
import com.pixelmonmod.pixelmon.enums.EnumFeatureState;
import com.pixelmonmod.pixelmon.enums.items.EnumCharms;
import com.pixelmonmod.pixelmon.listener.EntityPlayerExtension;
import com.pixelmonmod.pixelmon.client.gui.inventory.*;
import com.pixelmonmod.pixelmon.comm.packetHandlers.SetCharmPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.play.server.*;
import net.minecraft.world.GameType;

/** Private end-to-end native drawer, charm packets and actual world restart check. */
public final class Charm190Cases {
    static volatile int step=-1,opened=-1,clientDone=-1,observed=-1,supplied=-1;
    static int started=-2,age;static boolean initialized,relog;static Screen screen;
    static final JsonArray rows=new JsonArray();static final int[][] SIZES={{320,240},{427,267},{640,400},{1280,800}};
    static volatile boolean itemStarted,itemClientDone;static int itemAge,itemBefore;static JsonObject itemReport;
    static boolean baseline(){return Files.exists(Path.of("charm-baseline"));}
    static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
    static ItemStack marker(){var s=local.pixelmon.backport.AuthorBossTeams.item("minecraft:diamond");s.func_190920_e(7);s.func_196082_o().func_74757_a("BackportCharm190",true);return s;}
    static void all(PlayerPartyStorage p,EnumFeatureState state){p.setExpCharm(state);p.setShinyCharm(state);p.setOvalCharm(state);p.setCatchingCharm(state);p.setMarkCharm(state);}
    static boolean allAre(PlayerPartyStorage p,EnumFeatureState state){return p.getExpCharm()==state&&p.getShinyCharm()==state&&p.getOvalCharm()==state&&p.getCatchingCharm()==state&&p.getMarkCharm()==state;}
    static void metadata(ServerPlayerEntity p){var s=StorageProxy.getParty(p);EntityPlayerExtension.updatePlayerExpCharm(p,s.getExpCharm());EntityPlayerExtension.updatePlayerShinyCharm(p,s.getShinyCharm());EntityPlayerExtension.updatePlayerOvalCharm(p,s.getOvalCharm());EntityPlayerExtension.updatePlayerCatchingCharm(p,s.getCatchingCharm());EntityPlayerExtension.updatePlayerMarkCharm(p,s.getMarkCharm());p.field_71135_a.func_147359_a(new SEntityMetadataPacket(p.func_145782_y(),p.func_184212_Q(),true));}
    static void cursor(ServerPlayerEntity p,ItemStack stack){p.field_71071_by.func_70437_b(stack);p.field_71135_a.func_147359_a(new SSetSlotPacket(-1,-1,stack));}
    static InventoryPixelmon<?> inventory(Screen s)throws Exception{var f=s.getClass().getDeclaredField("inventory");f.setAccessible(true);return (InventoryPixelmon<?>)f.get(s);}
    static void cmd(ServerPlayerEntity p,String command){p.func_184102_h().func_195571_aL().func_197059_a(p.func_195051_bN().func_197033_a(4).func_197031_a(),command);}
    static void next(ServerPlayerEntity p,int value){cursor(p,ItemStack.field_190927_a);if(value<16)p.func_71033_a(value>=8?GameType.CREATIVE:GameType.SURVIVAL);else p.func_71033_a(GameType.SURVIVAL);var party=StorageProxy.getParty(p);all(party,value==18?EnumFeatureState.Disabled:EnumFeatureState.Active);metadata(p);supplied=-1;step=value;}
    static void row(String kind,boolean passed,String detail){var r=new JsonObject();r.addProperty("case",kind);r.addProperty("passed",passed);r.addProperty("detail",detail);rows.add(r);BossBalance190Probe.log("CHARM190_CASE "+kind+" "+passed+" "+detail);}
    public static void item(ServerPlayerEntity player)throws Exception{
        if(itemClientDone){check(StorageProxy.getParty(player).getExpCharm()==EnumFeatureState.Active,"Native physical item ownership retained");itemReport.addProperty("client_before_inventory",itemBefore);itemReport.addProperty("client_after_inventory",EnumFeatureState.Active.ordinal());Files.writeString(Path.of("charm190-item-results.json"),new GsonBuilder().setPrettyPrinting().create().toJson(itemReport));BossBalance190Probe.log("CHARM190_ITEM_PASS native item consumption, duplicate preservation and actual client inventory icon");BossBalance190Probe.done=true;return;}
        if(itemStarted)return;player.func_71033_a(GameType.SURVIVAL);var party=StorageProxy.getParty(player);party.setExpCharm(EnumFeatureState.Disabled);metadata(player);var saved=player.func_184586_b(net.minecraft.util.Hand.MAIN_HAND).func_77946_l();
        var token=new ItemStack(com.pixelmonmod.pixelmon.api.registries.PixelmonItems.exp_charm);player.func_184611_a(net.minecraft.util.Hand.MAIN_HAND,token);
        var used=token.func_77973_b().func_77659_a(player.field_70170_p,player,net.minecraft.util.Hand.MAIN_HAND);check(used.func_188397_a()==net.minecraft.util.ActionResultType.SUCCESS&&token.func_190926_b()&&party.getExpCharm()==EnumFeatureState.Active,"Quest physical Exp Charm consumes exactly once and unlocks");
        var duplicate=new ItemStack(com.pixelmonmod.pixelmon.api.registries.PixelmonItems.exp_charm);player.func_184611_a(net.minecraft.util.Hand.MAIN_HAND,duplicate);var repeated=duplicate.func_77973_b().func_77659_a(player.field_70170_p,player,net.minecraft.util.Hand.MAIN_HAND);check(repeated.func_188397_a()==net.minecraft.util.ActionResultType.FAIL&&duplicate.func_190916_E()==1&&party.getExpCharm()==EnumFeatureState.Active,"Duplicate owned charm is not consumed");player.func_184611_a(net.minecraft.util.Hand.MAIN_HAND,saved);player.func_71120_a(player.field_71069_bz);
        itemReport=new JsonObject();itemReport.addProperty("item","pixelmon:exp_charm");itemReport.addProperty("first_consumed",1);itemReport.addProperty("duplicate_consumed",0);itemReport.addProperty("server_owned",true);itemStarted=true;
    }
    public static void itemClient()throws Exception{
        if(!itemStarted||itemClientDone)return;var mc=Minecraft.func_71410_x();if(mc.field_71439_g==null)return;
        if(++itemAge==8){itemBefore=EntityPlayerExtension.getPlayerExpCharm(mc.field_71439_g).ordinal();mc.func_147108_a(new InventoryPixelmonExtendedScreen(mc.field_71439_g));}
        if(itemAge==28){check(EntityPlayerExtension.getPlayerExpCharm(mc.field_71439_g)==EnumFeatureState.Active,"Physical unlock visible in actual inventory");itemClientDone=true;}
    }
    public static void server(ServerPlayerEntity player)throws Exception{
        var party=StorageProxy.getParty(player);
        if(!initialized){initialized=true;relog=Files.exists(Path.of("charm190-relog-ready"));
            if(relog){check(party.getExpCharm()==EnumFeatureState.Active,"Owned charm survives actual restart");boolean item=false;for(int i=0;i<36;i++){var s=player.field_71071_by.func_70301_a(i);if(s.func_77978_p()!=null&&s.func_77978_p().func_74767_n("BackportCharm190")){check(s.func_190916_E()==7,"Saved marker count7");item=true;}}check(item,"Inventory marker survives actual restart");row("actual-relog",true,"native saved charm ownership and inventory count7");step=20;return;}
            next(player,0);return;
        }
        if(step<16&&opened==step&&supplied!=step){cursor(player,marker());supplied=step;}
        if(clientDone!=step)return;
        if(relog){
            if(step==20){row("relog-client",observed==EnumFeatureState.Active.ordinal(),"client state="+observed);check(observed==EnumFeatureState.Active.ordinal(),"Relog client receives authoritative charm");next(player,21);party.setExpCharm(EnumFeatureState.Available);metadata(player);return;}
            row("owned-inactive-repair",observed==EnumFeatureState.Available.ordinal()&&party.getExpCharm()==EnumFeatureState.Available,"client="+observed+" server="+party.getExpCharm());check(observed==EnumFeatureState.Available.ordinal()&&party.getExpCharm()==EnumFeatureState.Available,"Refresh preserves intentional inactive owned state");Files.writeString(Path.of("charm190-relog-results.json"),new GsonBuilder().setPrettyPrinting().create().toJson(rows));BossBalance190Probe.log("CHARM190_RELOG_PASS");BossBalance190Probe.done=true;return;
        }
        if(step<16){
            int count=player.field_71071_by.func_70445_o().func_190916_E();boolean owned=party.getExpCharm().isAvailable();row("click-"+step,count==7&&owned,"cursor="+count+" owned="+party.getExpCharm());check(count==7&&owned,"Drawer/old arrow preserves cursor and ownership");next(player,step+1);return;
        }
        if(step==16){boolean ok=observed==EnumFeatureState.Active.ordinal()&&party.getExpCharm()==EnumFeatureState.Active;row("stale-client-repair",ok,"client="+observed+" server="+party.getExpCharm());if(!baseline())check(ok,"Opening drawer repairs stale client metadata");next(player,17);return;}
        if(step==17){boolean ok=allAre(party,EnumFeatureState.Active);row("client-cannot-revoke",ok,"all five charms; exp="+party.getExpCharm());if(!baseline())check(ok,"Client toggle cannot remove ownership");next(player,18);return;}
        if(step==18){boolean ok=allAre(party,EnumFeatureState.Disabled);row("client-cannot-grant",ok,"all five charms; exp="+party.getExpCharm());check(ok,"Unowned cannot be granted by toggle");
            for(var state:EnumFeatureState.values()){
                all(party,state);var nbt=party.writeToNBT(new CompoundNBT());var restored=new PlayerPartyStorage(UUID.randomUUID(),false);restored.readFromNBT(nbt);check(restored.getExpCharm()==state&&restored.getShinyCharm()==state&&restored.getOvalCharm()==state&&restored.getCatchingCharm()==state&&restored.getMarkCharm()==state,"Five native charm states serialize "+state);
            }row("native-nbt",true,"all five charms in Disabled/Available/Active states");
            if(!baseline()){all(party,EnumFeatureState.Active);EntityPlayerExtension.updatePlayerExpCharm(player,EnumFeatureState.Available);check(player.func_184212_Q().func_187223_a(),"Pending live metadata update");local.pixelmon.backport.CharmStateSync.refresh(player);check(player.func_184212_Q().func_187223_a(),"Owner resync preserves updates pending for other tracking players");row("tracking-dirty",true,"owner snapshot does not clear live dirty flags");}
            cmd(player,"expcharm "+player.func_200200_C_().getString());check(party.getExpCharm()==EnumFeatureState.Active,"Native grant command");cmd(player,"expcharm "+player.func_200200_C_().getString()+" remove");check(party.getExpCharm()==EnumFeatureState.Disabled,"Admin remove remains functional");row("admin-command",true,"native grant/remove unaffected");
            all(party,EnumFeatureState.Active);metadata(player);cursor(player,ItemStack.field_190927_a);check(player.field_71071_by.func_70441_a(marker()),"Private fixture inventory has room");player.func_71120_a(player.field_71069_bz);
            Files.writeString(Path.of("charm190-results.json"),new GsonBuilder().setPrettyPrinting().create().toJson(rows));Files.writeString(Path.of("charm190-relog-ready"),"owned Active, marker count7\n");BossBalance190Probe.log("CHARM190_FIRST_PASS baseline="+baseline());step=20;return;
        }
        if(step==20)BossBalance190Probe.done=true;
    }
    public static void client()throws Exception{
        if(step<0)return;var mc=Minecraft.func_71410_x();if(mc.field_71439_g==null)return;
        if(started!=step){started=step;age=0;return;}age++;
        if(age==3){
            mc.field_71439_g.field_71071_by.func_70437_b(ItemStack.field_190927_a);
            if(step==16||step==21){var f=EntityPlayerExtension.class.getDeclaredField("dwExpCharm");f.setAccessible(true);@SuppressWarnings("unchecked") var key=(net.minecraft.network.datasync.DataParameter<Byte>)f.get(null);mc.field_71439_g.func_184212_Q().func_187227_b(key,(byte)EnumFeatureState.Disabled.ordinal());check(EntityPlayerExtension.getPlayerExpCharm(mc.field_71439_g)==EnumFeatureState.Disabled,"Controlled stale metadata reproduced");}
            screen=step<16&&step>=8?new CreativeInventoryExtendedScreen(mc.field_71439_g):new InventoryPixelmonExtendedScreen(mc.field_71439_g);mc.func_147108_a(screen);
            if(step<16){var size=SIZES[(step/2)%4];screen.func_231158_b_(mc,size[0],size[1]);}
            var inv=inventory(screen);check(inv.isDrawerOut()&&inv.getDrawerOffset()==42,"Pinned drawer all scales and modes");opened=step;
        }
        if(age==10){
            if(step<16){check(supplied==step,"Real server cursor supplied after screen opening");var inv=inventory(screen);int left=((InventoryPixelmonBase)screen).getGUILeft()-inv.getPartyWidth(),middle=screen.field_230709_l_/2;screen.func_231044_a_(step%2==0?left-13:left-33,step%2==0?middle+21:middle,0);}
            if(step==17)for(var charm:EnumCharms.values())com.pixelmonmod.pixelmon.api.util.helpers.NetworkHelper.sendToServer(new SetCharmPacket(charm,EnumFeatureState.Disabled));
            if(step==18)for(var charm:EnumCharms.values())com.pixelmonmod.pixelmon.api.util.helpers.NetworkHelper.sendToServer(new SetCharmPacket(charm,EnumFeatureState.Active));
        }
        if(step==21&&age==10){var f=EntityPlayerExtension.class.getDeclaredField("dwExpCharm");f.setAccessible(true);@SuppressWarnings("unchecked") var key=(net.minecraft.network.datasync.DataParameter<Byte>)f.get(null);mc.field_71439_g.func_184212_Q().func_187227_b(key,(byte)EnumFeatureState.Disabled.ordinal());}
        if(age==(step==21?28:17)){observed=EntityPlayerExtension.getPlayerExpCharm(mc.field_71439_g).ordinal();if(step==20||step==21)net.minecraft.util.ScreenShotHelper.func_148259_a(mc.field_71412_D,step==20?"charm190-owned.png":"charm190-owned-inactive.png",mc.func_228018_at_().func_198107_o(),mc.func_228018_at_().func_198087_p(),mc.func_147110_a(),message->{});clientDone=step;}
    }
}
