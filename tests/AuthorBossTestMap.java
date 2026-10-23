import java.util.*;
import com.google.gson.*;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.battles.BattleRegistry;
import com.pixelmonmod.pixelmon.client.ServerStorageDisplay;
import com.pixelmonmod.pixelmon.client.gui.starter.ChooseStarterScreen;
import local.pixelmon.backport.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ChestTileEntity;
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

/** Private inspection world only. No automatic game launch or edits to other saves. */
@Mod("backport_author_test")
@Mod.EventBusSubscriber(modid="backport_author_test")
public final class AuthorBossTestMap {
    static final String WORLD="神秘任务男与完整战利品 1.7.0",TAG="Author170Test";
    static ServerWorld prepared;
    static final Set<UUID> READY=new HashSet<>();
    static final List<String> THEMES=new ArrayList<>(AuthorBossPolicy.TYPES.keySet());
    static {THEMES.add("paradox");THEMES.add("ultra");}
    static boolean isMap(ServerPlayerEntity p){return WORLD.equals(p.func_71121_q().func_73046_m().func_240793_aU_().func_76065_j());}
    static void tell(ServerPlayerEntity p,String s){p.func_146105_b(new StringTextComponent(s),false);}
    static void cmd(ServerPlayerEntity p,String s){p.func_71121_q().func_73046_m().func_195571_aL().func_197059_a(p.func_195051_bN().func_197033_a(4).func_197031_a(),s);}
    static void block(ServerWorld w,BlockPos p,String id){w.func_180501_a(p,ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id)).func_176223_P(),3);}
    static void label(ServerWorld w,double x,double y,double z,String text){
        ArmorStandEntity a=new ArmorStandEntity(w,x,y,z);a.func_200203_b(new StringTextComponent(text));a.func_174805_g(true);a.func_82142_c(true);a.func_189654_d(true);a.func_184211_a(TAG);w.func_217376_c(a);
    }
    static BlockPos room(int i){return new BlockPos(-48+(i%5)*24,64,60+(i/5)*24);}
    static void spawn(ServerWorld w,int i){
        BlockPos home=room(i);AuthorBossFeature.build(w,home,false,AuthorBossFeature.material(w,home,THEMES.get(i-1)));
        Entity e=AuthorBossFeature.create(w,home,false,THEMES.get(i-1));e.func_184211_a(TAG);w.func_217376_c(e);
    }
    static void chest(ServerWorld w,BlockPos pos,ItemStack item){
        block(w,pos,"minecraft:chest");var tile=w.func_175625_s(pos);
        if(!(tile instanceof ChestTileEntity))throw new IllegalStateException("Missing test display chest "+pos);
        ChestTileEntity chest=(ChestTileEntity)tile;
        for(int slot=0;slot<27;slot++)chest.func_70299_a(slot,ItemStack.field_190927_a);
        if(!item.func_190926_b())chest.func_70299_a(13,item);chest.func_70296_d();
    }
    static void setup(ServerPlayerEntity p){
        ServerWorld w=p.func_71121_q();
        // Explicit force-loading is confined to this private map. No live world is copied.
        cmd(p,"forceload add -64 -32 63 127");
        if(!(w.func_175625_s(new BlockPos(0,65,-23)) instanceof ChestTileEntity)){
            for(int x=-62;x<=62;x++)for(int z=-28;z<=124;z++){
                block(w,new BlockPos(x,64,z),z<32?"minecraft:smooth_quartz":"minecraft:polished_andesite");
                if(x%12==0&&z%12==0)block(w,new BlockPos(x,64,z),"minecraft:sea_lantern");
            }
            label(w,.5,71,-24,"1.7.0 神秘任务男 · 78 项完整奖池核对");
            int n=0,missing=0;
            for(JsonElement value:AuthorBossRewards.POOL){
                JsonObject e=value.getAsJsonObject();int x=-32+(n%9)*8,z=-15+(n/9)*5;
                ItemStack item=AuthorBossRewards.stack(e);if(item.func_190926_b())missing++;
                chest(w,new BlockPos(x,65,z),item);
                String title=String.format("%02d 图%d-%d-%d ",n+1,e.get("image").getAsInt(),e.get("row").getAsInt(),e.get("column").getAsInt())+e.get("display_zh").getAsString()+" ×"+e.get("count").getAsInt();
                label(w,x+.5,67.8,z+.5,title);
                label(w,x+.5,67.5,z+.5,item.func_190926_b()?"缺少对应扩展模组，当前不参与抽取":(e.has("ball")?"pixelmon:poke_ball {PokeBallID:"+e.get("ball").getAsString()+"}":e.get("item").getAsString()));
                n++;
            }
            JsonObject fixed=new JsonObject();fixed.addProperty("ball","master_ball");fixed.addProperty("count",1);
            chest(w,new BlockPos(0,65,-23),AuthorBossRewards.stack(fixed));
            label(w,.5,68,-22.5,"固定奖励：大师球 ×1；宝可梦币 50～100 万；另外抽取五项不重复奖品");
            for(int i=1;i<=THEMES.size();i++){
                spawn(w,i);BlockPos c=room(i);
                label(w,c.func_177958_n()+.5,73,c.func_177952_p()+.5,"作者道馆："+THEMES.get(i-1)+"·原生实力相当");
            }
            for(int i=0;i<5;i++){int x=-16+i*8;block(w,new BlockPos(x,65,38),"minecraft:emerald_block");label(w,x+.5,67.2,38.5,List.of("恢复玩家状态","宝可梦对战队伍","创造模式核对","中止挑战并治疗","仅重置已击败Boss").get(i));}
            label(w,.5,70,34,"右键绿宝石台操作；作者右键挑战；玩家禁药，作者有2个全满伤药和1个活力块");
            p.getPersistentData().func_74757_a("Author170MapBuilt",true);
            tell(p,"已陈列全部 78 项。缺少依赖物品 "+missing+" 项会保留空箱和标签，请配合原整合包核对。");
        }
        if(!p.getPersistentData().func_74767_n("Author170PvpRetired")){
            for(Entity e:w.func_175647_a(Entity.class,new AxisAlignedBB(-64,60,45,64,85,124),e->e.func_184216_O().contains(TAG)&&e instanceof ArmorStandEntity)){
                String name=e.func_200200_C_().getString();if(name.contains("近战")||name.contains("PVP"))e.func_70106_y();
            }
            BlockPos old=room(0);for(int x=-6;x<=6;x++)for(int z=-6;z<=7;z++)for(int y=0;y<=7;y++)block(w,old.func_177982_a(x,y,z),y==0?"minecraft:polished_andesite":"minecraft:air");
            p.getPersistentData().func_74757_a("Author170PvpRetired",true);
        }
        // Upgrade already-created private test rooms without rebuilding them or respawning bosses.
        var bricks=ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft:stone_bricks"));
        for(int i=1;i<=THEMES.size();i++){
            AuthorBossFeature.upgrade(w,room(i),false);
            BlockPos ceiling=room(i).func_177982_a(0,7,0);
            if(w.func_180495_p(ceiling).func_177230_c()==bricks)block(w,ceiling,"minecraft:sea_lantern");
            for(int x:new int[]{-4,4})for(int z:new int[]{-4,4}){
                BlockPos old=room(i).func_177982_a(x,6,z);
                if(w.func_180495_p(old).func_177230_c()==net.minecraft.block.Blocks.field_180398_cJ){block(w,old,"minecraft:air");block(w,old.func_177982_a(0,1,0),"minecraft:sea_lantern");}
            }
            if(i>0)for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++){
                BlockPos floor=room(i).func_177982_a(x,0,z);
                if(w.func_180495_p(floor).func_177230_c()==ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft:chiseled_quartz_block")))block(w,floor,"minecraft:white_wool");
            }
        }
        prepared=w;
    }
    static void equipment(ServerPlayerEntity p){
        cmd(p,"gamemode survival");
        p.func_70606_j(p.func_110138_aP());cmd(p,"spawnpoint @s 0 65 34");
        tell(p,"状态已恢复，右键作者开始宝可梦对战。");
    }
    static void team(ServerPlayerEntity p){
        var storage=StorageProxy.getParty(p);var team=AuthorBossTeams.roster("plains",new Random(170));
        for(int i=0;i<6;i++){var mon=team.get(i);mon.setLevelNum(100);mon.heal();storage.set(i,mon);}
        cmd(p,"give @s pixelmon:full_restore 32");tell(p,"仅本测试地图：已替换为六只满配100级测试宝可梦；Boss 随原生实力相当规则调整等级。");
    }
    static void abort(ServerPlayerEntity p){
        var battle=BattleRegistry.getBattle(p);if(battle!=null)battle.endBattleWithoutXP();
        StorageProxy.getParty(p).heal();p.func_70606_j(p.func_110138_aP());cmd(p,"tp @s 0.5 65 34.5 180 8");
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||ServerLifecycleHooks.getCurrentServer()==null)return;
        for(ServerPlayerEntity p:ServerLifecycleHooks.getCurrentServer().func_184103_al().func_181057_v())if(isMap(p)){
            if(prepared!=p.func_71121_q())setup(p);
            if(READY.add(p.func_110124_au())){
                StorageProxy.getParty(p).starterPicked=true;
                if(Arrays.stream(StorageProxy.getParty(p).getAll()).allMatch(Objects::isNull)){
                    team(p);tell(p,"空队伍已补齐六只测试宝可梦，左侧队伍栏可正常显示；已有队伍不会被自动覆盖。");
                }
                if(!p.getPersistentData().func_74767_n("Author170Compass")&&ForgeRegistries.ITEMS.containsKey(new ResourceLocation("explorerscompass:explorerscompass"))){
                    cmd(p,"give @s explorerscompass:explorerscompass");p.getPersistentData().func_74757_a("Author170Compass",true);
                    tell(p,"已发探险家指南针：搜索“作者道馆”，也可使用/locate pixelmon_backport:author_gym。");
                }
                if(!p.getPersistentData().func_74767_n("Author170Visited")){cmd(p,"tp @s 0.5 65 34.5 180 8");cmd(p,"spawnpoint @s 0 65 34");p.getPersistentData().func_74757_a("Author170Visited",true);}
                tell(p,"出生点前方为战利品陈列，后方十四座宝可梦对战测试房。图号、行列、份数与截图对应。末地/究极等主题在此平地复现，天然生成仍需另行实机核对。");
            }
        }
    }
    @SubscribeEvent public static void click(PlayerInteractEvent.RightClickBlock event){
        if(event.getWorld().field_72995_K||event.getHand()!=Hand.MAIN_HAND||!(event.getPlayer() instanceof ServerPlayerEntity))return;
        ServerPlayerEntity p=(ServerPlayerEntity)event.getPlayer();BlockPos pos=event.getPos();
        if(!isMap(p)||pos.func_177956_o()!=65||pos.func_177952_p()!=38)return;
        int x=pos.func_177958_n();if(x < -16||x>16||(x+16)%8!=0)return;event.setCanceled(true);
        switch((x+16)/8){
            case 0:equipment(p);break;case 1:team(p);break;case 2:cmd(p,"gamemode creative");break;case 3:abort(p);break;
            case 4:
                for(int i=1;i<=THEMES.size();i++){
                    BlockPos home=room(i);boolean exists=!p.func_71121_q().func_175647_a(Entity.class,new AxisAlignedBB(home).func_186662_g(8),e->AuthorBossArenas.authored(e)).isEmpty();
                    if(!exists)spawn(p.func_71121_q(),i);
                }
                tell(p,"只补回已被击败、移除的测试 Boss。实际掉落仍通过生产胜利逻辑发放。");break;
        }
    }
    @SubscribeEvent public static void stopped(net.minecraftforge.fml.event.server.FMLServerStoppedEvent event){prepared=null;READY.clear();}
    @Mod.EventBusSubscriber(modid="backport_author_test",value=net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static final class Client {
        @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
            if(event.phase!=TickEvent.Phase.END)return;
            var mc=net.minecraft.client.Minecraft.func_71410_x();var server=mc.func_71401_C();
            if(mc.field_71439_g==null||mc.field_71441_e==null||server==null||!WORLD.equals(server.func_240793_aU_().func_76065_j()))return;
            // The server-side fixture skips starter selection. Its already-queued native
            // StarterListPacket can still open the client screen; clear the actual UI flag.
            ServerStorageDisplay.showStarterScreen=false;
            if(mc.field_71462_r instanceof ChooseStarterScreen)mc.func_147108_a(null);
        }
    }
}
