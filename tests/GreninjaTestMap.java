import java.util.*;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.battles.BattleType;
import com.pixelmonmod.pixelmon.client.gui.starter.ChooseStarterScreen;
import com.pixelmonmod.pixelmon.client.ServerStorageDisplay;
import com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.BattleBond;
import com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.battles.BattleRegistry;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.entities.npcs.NPCRelearner;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import local.pixelmon.backport.*;

/** Optional helper; fixtures and commands are restricted to this disposable world. */
@Mod("backport_greninja_test")
@Mod.EventBusSubscriber(modid="backport_greninja_test")
public final class GreninjaTestMap {
    public GreninjaTestMap(){Pixelmon.EVENT_BUS.register(new GreninjaNativeAudit());}
    static final String WORLD="轻快之旅 1.6.0 忍蛙与配置测试";
    static final Set<UUID> READY=new HashSet<>();
    static boolean map(ServerPlayerEntity p){return WORLD.equals(p.func_184102_h().func_240793_aU_().func_76065_j());}
    static void tell(ServerPlayerEntity p,String s){p.func_145747_a(new StringTextComponent(s),new UUID(0,0));}
    static void cmd(ServerPlayerEntity p,String s){p.func_184102_h().func_195571_aL().func_197059_a(p.func_195051_bN().func_197033_a(4).func_197031_a(),s);}
    static Pokemon mon(int dex,boolean ash,String... moves){
        Pokemon p=PokemonFactory.create(PixelmonSpecies.fromNationalDex(dex));p.setLevelNum(dex==658?60:100);p.initialize();
        if(dex==658){p.setAbility(new BattleBond());if(ash)p.setForm(PermanentAsh.FORM);}
        for(int i=0;i<4;i++)p.getMoveset().set(i,new Attack(moves[Math.min(i,moves.length-1)]));p.heal();return p;
    }
    static void prepare(ServerPlayerEntity p){
        var world=p.func_71121_q();for(int x=-20;x<=20;x++)for(int z=-20;z<=20;z++)world.func_180501_a(new BlockPos(x,64,z),net.minecraft.block.Blocks.field_150348_b.func_176223_P(),2);
        var party=StorageProxy.getParty(p);party.starterPicked=true;
        party.set(0,mon(658,true,"Water Shuriken",NinjaTraining.MOVE,"Protect","Splash"));
        party.set(1,mon(658,true,"Water Shuriken","Protect","Splash","Dark Pulse"));
        party.set(2,mon(658,false,"Water Shuriken","Dark Pulse","Protect","Splash"));
        party.set(3,mon(143,false,"Splash","Protect"));
        NPCRelearner npc=new NPCRelearner(world);npc.func_70012_b(5,65,3,180,0);npc.setCost(net.minecraft.item.ItemStack.field_190927_a);npc.func_110163_bv();world.func_217376_c(npc);
        cmd(p,"gamemode creative");cmd(p,"time set day");cmd(p,"gamerule doDaylightCycle false");cmd(p,"gamerule doMobSpawning false");cmd(p,"tp @s 0 65 8 180 10");
        tell(p,"1.6.0 忍蛙测试：/ninjatest single 单打；double 双打；unlock 普通忍蛙触发羁绊；stats 查看当前全场与个人计时。右侧 NPC 免费回忆技能，P 打开配置。");
        tell(p,"第二只小智忍蛙稍后会提示学习，可拒绝后找回忆师；第一只已学会。仅本地图准备队伍，不会处理普通存档。");
    }
    static int target(ServerPlayerEntity p){
        if(!map(p)||BattleRegistry.getBattle(p)!=null)return 0;
        var world=p.func_71121_q();
        for(var old:world.func_175647_a(PixelmonEntity.class,new AxisAlignedBB(-20,60,-20,20,90,20),v->v.getPersistentData().func_74767_n("GreninjaTestOpponent")))old.func_70106_y();
        Pokemon mon=mon(242,false,"Splash");mon.getMoveset().clear();mon.getMoveset().add(new Attack("Splash"));mon.heal();
        var entity=mon.getOrCreatePixelmon(world,0.5,65,-5.5);entity.func_94061_f(true);entity.func_110163_bv();entity.getPersistentData().func_74757_a("GreninjaTestOpponent",true);world.func_217376_c(entity);
        tell(p,"前方已准备 100 级幸福蛋，只会跃起。直接向它抛出忍蛙开始实战；/ninjatest target 可重新生成。");return 1;
    }
    static int fight(ServerPlayerEntity p,boolean doubles,boolean unlock){
        return fight(p,doubles,unlock,242);
    }
    static int fight(ServerPlayerEntity p,boolean doubles,boolean unlock,int opponentDex){
        if(!map(p)||BattleRegistry.getBattle(p)!=null)return 0;
        var party=StorageProxy.getParty(p);for(Pokemon mon:party.getAll())if(mon!=null)mon.heal();
        int selected=unlock?2:0;List<Pokemon> members=new ArrayList<>();for(Pokemon member:party.getAll())if(member!=null)members.add(member);var user=new PlayerParticipant(p,members,selected);
        Pokemon one=mon(unlock?10:opponentDex,false,"Splash");if(unlock){one.setLevelNum(5);one.heal();}
        var world=p.func_71121_q();var first=one.getOrCreatePixelmon(world,-2,65,-4);world.func_217376_c(first);
        WildPixelmonParticipant enemy;
        if(doubles){var second=mon(242,false,"Splash").getOrCreatePixelmon(world,2,65,-4);world.func_217376_c(second);enemy=new WildPixelmonParticipant(first,second);}
        else enemy=new WildPixelmonParticipant(first);
        BattleRegistry.startBattle(new BattleParticipant[]{user},new BattleParticipant[]{enemy},doubles?BattleType.DOUBLE:BattleType.SINGLE);
        return 1;
    }
    @SubscribeEvent public static void commands(RegisterCommandsEvent e){
        var root=Commands.func_197057_a("ninjatest");
        root.then(Commands.func_197057_a("target").executes(c->target(c.getSource().func_197035_h())));
        root.then(Commands.func_197057_a("single").executes(c->fight(c.getSource().func_197035_h(),false,false)));
        root.then(Commands.func_197057_a("double").executes(c->fight(c.getSource().func_197035_h(),true,false)));
        root.then(Commands.func_197057_a("unlock").executes(c->fight(c.getSource().func_197035_h(),false,true)));
        root.then(Commands.func_197057_a("stats").executes(c->{var p=c.getSource().func_197035_h();if(!map(p))return 0;var b=BattleRegistry.getBattle(p);if(b==null){tell(p,"当前未在战斗中。");return 0;}tell(p,"全场回合 "+b.battleTurn);
            for(var part:b.participants)for(var mon:part.allPokemon){StringBuilder s=new StringBuilder(mon.getNickname()).append(mon.onBattlefield?" [场上]":" [后备]").append(" HP ").append(mon.getHealth()).append('/').append(mon.getMaxHealth()).append(" 蚀 ").append(Erosion.remaining(mon)).append(" 水强化 ").append(AshWaterBoost.remaining(mon)).append(" 追加行动 ").append(NinjaTraining.bonus(mon));
                for(var stat:new BattleStatsType[]{BattleStatsType.ATTACK,BattleStatsType.DEFENSE,BattleStatsType.SPECIAL_ATTACK,BattleStatsType.SPECIAL_DEFENSE,BattleStatsType.SPEED})s.append(' ').append(stat).append('=').append(mon.getBattleStats().getStatFromEnum(stat));tell(p,s.toString());}
            return 1;}));
        e.getDispatcher().register(root);
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e){if(e.phase==TickEvent.Phase.END&&e.player instanceof ServerPlayerEntity){var p=(ServerPlayerEntity)e.player;if(map(p)&&READY.add(p.func_110124_au())){
        if(!p.getPersistentData().func_74767_n("greninja_test_prepared_160")){prepare(p);p.getPersistentData().func_74757_a("greninja_test_prepared_160",true);}
        else tell(p,"忍蛙测试地图已恢复，保留上次的学习和队伍记录。/ninjatest single、double、unlock、stats。");
        target(p);
    }}}
    @SubscribeEvent public static void stop(net.minecraftforge.fml.event.server.FMLServerStoppedEvent e){READY.clear();}
    @Mod.EventBusSubscriber(modid="backport_greninja_test",value=net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static final class Client {
        static boolean opened;
        @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
            if(e.phase!=TickEvent.Phase.END)return;
            var mc=net.minecraft.client.Minecraft.func_71410_x();
            if(!opened&&"backport-greninja-160".equals(System.getProperty("backport.manual.world"))&&mc.field_71462_r instanceof net.minecraft.client.gui.screen.MainMenuScreen){opened=true;mc.func_238191_a_("backport-greninja-160");}
            if(mc.field_71439_g!=null&&READY.contains(mc.field_71439_g.func_110124_au())){ServerStorageDisplay.showStarterScreen=false;if(mc.field_71462_r instanceof ChooseStarterScreen)mc.func_147108_a(null);}
        }
        @SubscribeEvent public static void starter(net.minecraftforge.client.event.GuiOpenEvent e){var mc=net.minecraft.client.Minecraft.func_71410_x();if(mc.field_71439_g!=null&&READY.contains(mc.field_71439_g.func_110124_au())&&e.getGui() instanceof ChooseStarterScreen){ServerStorageDisplay.showStarterScreen=false;e.setCanceled(true);}}
    }
}
