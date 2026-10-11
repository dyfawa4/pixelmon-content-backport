import java.util.*;
import java.math.BigDecimal;
import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.entities.npcs.NPCTrainer;
import com.pixelmonmod.pixelmon.enums.EnumMegaItem;
import com.pixelmonmod.pixelmon.world.generation.features.GymVillagePiece;
import com.pixelmonmod.pixelmon.world.structure.type.GymConfig;
import com.pixelmonmod.pixelmon.world.structure.PixelmonStructureFeatures;
import local.pixelmon.backport.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.*;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraft.world.gen.feature.jigsaw.*;
import net.minecraft.world.gen.feature.structure.*;

/** Distributable controls for eighteen separate gym city worlds; no automatic QA runner. */
public final class GymTestMaps {
    public static final String PREFIX="回迁道馆 ";
    public static final String[] TYPES={"normal","fire","water","electric","grass","ice","fighting","poison","ground","flying","psychic","bug","rock","ghost","dragon","dark","steel","fairy"};
    public static final String[] LABELS={"一般","火","水","电","草","冰","格斗","毒","地面","飞行","超能力","虫","岩石","幽灵","龙","恶","钢","妖精"};
    static final Map<ServerWorld,Plan> PLANS=new WeakHashMap<>();
    public static final class Data extends WorldSavedData {
        int cursor;boolean ready;int gymX,gymY,gymZ,navVersion;
        public Data(){super("backport_gym_test");}
        public void func_76184_a(CompoundNBT n){cursor=n.func_74762_e("Cursor");ready=n.func_74767_n("Ready");gymX=n.func_74762_e("GymX");gymY=n.func_74762_e("GymY");gymZ=n.func_74762_e("GymZ");navVersion=n.func_74762_e("NavVersion");}
        public CompoundNBT func_189551_b(CompoundNBT n){n.func_74768_a("Cursor",cursor);n.func_74757_a("Ready",ready);n.func_74768_a("GymX",gymX);n.func_74768_a("GymY",gymY);n.func_74768_a("GymZ",gymZ);n.func_74768_a("NavVersion",navVersion);return n;}
    }
    static final class Plan {List<AbstractVillagePiece> pieces;String type;Data data;Plan(String t,List<AbstractVillagePiece> p,Data d){type=t;pieces=p;data=d;}}
    static Data data(ServerWorld w){return w.func_217481_x().func_215752_a(Data::new,"backport_gym_test");}
    public static String type(ServerWorld w){
        String title=w.func_73046_m().func_240793_aU_().func_76065_j();if(!title.startsWith(PREFIX))return null;
        for(String type:TYPES)if(title.endsWith("["+type+"]"))return type;return null;
    }
    public static boolean isMap(ServerPlayerEntity p){return type(p.func_71121_q())!=null || BadgeGallery.isMap(p);}
    public static List<AbstractVillagePiece> assemble(ServerWorld w,String type,BlockPos pos){
        String ns=GymCities.DATA.getAsJsonObject("newThemes").has(type)?"pixelmon_backport":"pixelmon";
        JigsawPattern original=w.func_241828_r().func_243612_b(Registry.field_243555_ax).func_82594_a(new ResourceLocation(ns,"gyms/"+type+"/start_pool"));
        if(type.equals("water"))original=((GymConfig)PixelmonStructureFeatures.WATER_GYM.field_236269_c_).func_242810_c().get();
        if(original==null || original.func_214946_c()==0)throw new IllegalStateException("Missing gym pool "+type);
        // Exclude added variants when inspecting one of the ten original types.
        List<Pair<JigsawPiece,Integer>> entries=new ArrayList<>();
        for(JigsawPiece p:original.func_214943_b(new Random(0)))if(p.toString().contains(ns+":gyms/"+type+"/"))entries.add(Pair.of(p,1));
        JigsawPattern pool=new JigsawPattern(new ResourceLocation("pixelmon_backport","test_"+type),new ResourceLocation("minecraft:empty"),entries);
        List<AbstractVillagePiece> best=Collections.emptyList();
        List<AbstractVillagePiece> complete=Collections.emptyList();
        int completeScore=-1;
        boolean authored=ns.equals("pixelmon_backport");
        for(int seed=1;seed<=64;seed++){
            List<AbstractVillagePiece> pieces=new ArrayList<>();
            JigsawManager.func_242837_a(w.func_241828_r(),new VillageConfig(()->pool,60),(a,b,c,d,e,f)->new GymVillagePiece(a,b,c,d,e,f,true),
                w.func_72863_F().func_201711_g(),w.func_184163_y(),pos,pieces,new Random(seed),true,true);
            if(pieces.size()>best.size())best=pieces;
            int houses=(int)pieces.stream().filter(p->p.toString().contains("_house_")).count();
            boolean withinPreview=pieces.stream().allMatch(p->{MutableBoundingBox b=p.func_74874_b();return b.field_78897_a>=-256 && b.field_78893_d<=256 && b.field_78896_c>=-256 && b.field_78892_f<=256;});
            if(pieces.size()>=40 && (!authored || houses>=8 && withinPreview) && pieces.stream().anyMatch(p->p.toString().contains("/gym_"+type+"]"))
                && pieces.stream().anyMatch(p->p.toString().contains("_pokecenter]")) && pieces.stream().anyMatch(p->p.toString().contains("_pokemart]"))){
                if(!authored)return pieces;
                int publicBuildings=(int)pieces.stream().filter(p->p.toString().matches(".*_(pokecenter|pokemart|daycare|church|blacksmith|tcg_shop)\\].*")).count();
                // Trees, NPC markers and street clutter cannot stand in for houses.
                int score=houses*32+publicBuildings*12+Math.min(100,pieces.size());
                if(score>completeScore){complete=pieces;completeScore=score;}
            }
            // Compare several complete layouts, rather than accepting the first
            // small city. The deterministic plan is reused by navigation.
            if(authored && seed>=12 && !complete.isEmpty())return complete;
        }
        if(!complete.isEmpty())return complete;
        throw new IllegalStateException("No complete city assembled "+type+" pieces="+best.size());
    }
    public static void tick(ServerPlayerEntity p){
        if(BadgeGallery.isMap(p)){BadgeGallery.tick(p);return;}
        ServerWorld w=p.func_71121_q();String type=type(w);if(type==null)return;Data data=data(w);
        if(data.navVersion<1)navigation(w,data);
        if(!type.equals(p.getPersistentData().func_74779_i("BackportGymHubTheme"))){p.getPersistentData().func_74778_a("BackportGymHubTheme",type);hub(p);}
        if(data.ready){
            if(!p.getPersistentData().func_74767_n("BackportGymWelcome")){
                p.getPersistentData().func_74757_a("BackportGymWelcome",true);team(p);hub(p);
                p.func_146105_b(new StringTextComponent("完整城市已就绪。右键出生点传送台前往城市/道馆；聊天可点击返回传送台。"),false);
            }
            return;
        }
        Plan plan=PLANS.get(w);
        if(plan==null){
            TestMaps.cmd(p,"gamerule doDaylightCycle false");TestMaps.cmd(p,"gamerule doWeatherCycle false");TestMaps.cmd(p,"gamerule doFireTick false");
            TestMaps.cmd(p,"gamerule doPokemonSpawning false");TestMaps.cmd(p,"gamerule doTrainerSpawning false");TestMaps.cmd(p,"time set day");
            plan=new Plan(type,assemble(w,type,BlockPos.field_177992_a),data);PLANS.put(w,plan);
            for(AbstractVillagePiece piece:plan.pieces)if(piece.toString().contains("/gym_"+type+"]")){
                MutableBoundingBox b=piece.func_74874_b();data.gymX=(b.field_78897_a+b.field_78893_d)/2;data.gymY=b.field_78895_b+3;data.gymZ=(b.field_78896_c+b.field_78892_f)/2;
            }
        }
        // Work in small batches so an entire city does not monopolize one game tick.
        int end=Math.min(plan.pieces.size(),data.cursor+4);
        for(;data.cursor<end;data.cursor++){
            AbstractVillagePiece piece=plan.pieces.get(data.cursor);
            if(!piece.func_237001_a_(w,w.func_241112_a_(),w.func_72863_F().func_201711_g(),new Random(data.cursor),
                new MutableBoundingBox(-256,0,-256,256,255,256),BlockPos.field_177992_a,false)){
                // Native optional tree features return false when conditions
                // disallow placement. A failed building remains a real error.
                if(piece.func_214826_b() instanceof FeatureJigsawPiece)System.out.println("BACKPORT_GYM_OPTIONAL_FEATURE_SKIPPED "+piece);
                else throw new IllegalStateException("City piece placement failed "+piece);
            }
        }
        data.func_76185_a();p.func_146105_b(new StringTextComponent("正在生成完整城市："+data.cursor+" / "+plan.pieces.size()),true);
        if(data.cursor==plan.pieces.size()){
            data.ready=true;data.func_76185_a();PLANS.remove(w);
            hub(p);
            System.out.println("BACKPORT_GYM_MAP_READY "+type+" pieces="+data.cursor);
        }
    }
    static final String[] NAV={"leader","city","overview","center","shop","team","end"};
    static final String[] NAV_LABEL={"主道馆 / 馆主","城市广场","城市鸟瞰","宝可梦中心","友好商店","准备测试队伍","结束当前对战"};
    static final String[] NAV_COLORS={"blue","cyan","white","pink","orange","lime","red"};
    static void navigation(ServerWorld w,Data d){
        for(int x=-9;x<=9;x++)for(int z=114;z<=126;z++)TestMaps.block(w,x,148,z,"minecraft:light_gray_stained_glass");
        for(int i=0;i<NAV.length;i++){
            int x=-6+(i%5)*3,z=i<5?117:123;TestMaps.block(w,x,149,z,"minecraft:"+NAV_COLORS[i]+"_concrete");
            FeatureStations.label(w,x+.5,150.4,z+.5,"右键："+NAV_LABEL[i]);
        }
        FeatureStations.label(w,.5,152,120.5,"道馆城市传送台 · 城市生成完成后可使用");
        d.navVersion=1;d.func_76185_a();
    }
    static String navigationAction(BlockPos pos){
        if(pos.func_177956_o()!=149)return null;
        for(int i=0;i<NAV.length;i++)if(pos.func_177958_n()==-6+(i%5)*3 && pos.func_177952_p()==(i<5?117:123))return NAV[i];return null;
    }
    static void hub(ServerPlayerEntity p){TestMaps.cmd(p,"gamemode creative");TestMaps.cmd(p,"tp @s 0.5 149 120.5 180 25");returnLink(p);}
    static void returnLink(ServerPlayerEntity p){p.func_146105_b(new StringTextComponent("[点击返回出生传送台]").func_230530_a_(net.minecraft.util.text.Style.field_240709_b_.func_240715_a_(new net.minecraft.util.text.event.ClickEvent(net.minecraft.util.text.event.ClickEvent.Action.RUN_COMMAND,"/backportqa gym hub"))),false);}
    @net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="backport_client_test")
    public static final class Navigation {
        @net.minecraftforge.eventbus.api.SubscribeEvent public static void click(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock event){
            if(event.getWorld().field_72995_K || event.getHand()!=net.minecraft.util.Hand.MAIN_HAND || !(event.getPlayer() instanceof ServerPlayerEntity))return;
            ServerPlayerEntity p=(ServerPlayerEntity)event.getPlayer();if(type(p.func_71121_q())==null)return;String action=navigationAction(event.getPos());if(action!=null){event.setCanceled(true);GymTestMaps.action(p,action);returnLink(p);}
        }
    }
    public static void overview(ServerPlayerEntity p){TestMaps.cmd(p,"gamemode creative");p.field_71075_bZ.field_75100_b=true;p.func_71016_p();TestMaps.cmd(p,"tp @s 0 150 120 180 35");returnLink(p);}
    public static void team(ServerPlayerEntity p){
        TestMaps.end(p);var party=StorageProxy.getParty(p);party.starterPicked=true;party.setBalance(new BigDecimal("1000000"));party.unlockMega(true);party.setMegaItem(EnumMegaItem.BraceletORAS,true);
        int[] dex={6,9,3,248,376,700};for(int i=0;i<dex.length;i++){
            Pokemon pokemon=TestMaps.mon(dex[i]);pokemon.getPersistentData().func_74757_a(NpcMoves.ACE,true);NpcMoves.configure(pokemon,p.func_110124_au());party.set(i,pokemon);
        }
        p.field_71071_by.func_70441_a(DedicatedTestMaps.stack("pixelmon_backport:tera_orb",1));
        p.field_71071_by.func_70441_a(DedicatedTestMaps.stack("pixelmon:max_potion",16));p.field_71071_by.func_70441_a(DedicatedTestMaps.stack("pixelmon:revive",16));
        p.field_71071_by.func_70441_a(DedicatedTestMaps.stack("pixelmon:charizardite_x",1));p.field_71071_by.func_70441_a(DedicatedTestMaps.stack("pixelmon:firium_z",1));
    }
    public static int action(ServerPlayerEntity p,String action){
        if(BadgeGallery.isMap(p)){if(action.equals("end"))TestMaps.end(p);else TestMaps.cmd(p,"tp @s 0 65 0 0 0");return 1;}
        if(!isMap(p))return 0;
        if(action.equals("hub")){hub(p);return 1;}
        if(action.equals("end")){TestMaps.end(p);return 1;}if(action.equals("team")){team(p);return 1;}
        if(!data(p.func_71121_q()).ready){p.func_146105_b(new StringTextComponent("城市尚在生成，请等待完成。"),false);return 0;}
        if(action.equals("overview")){overview(p);return 1;}
        if(action.equals("city")){int y=p.func_71121_q().func_201676_a(net.minecraft.world.gen.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,0,0);TestMaps.cmd(p,"tp @s 0.5 "+(y+1)+" 0.5");returnLink(p);return 1;}
        if(action.equals("center") || action.equals("shop")){
            String template=action.equals("center")?"_pokecenter]":"_pokemart]";
            for(AbstractVillagePiece piece:assemble(p.func_71121_q(),type(p.func_71121_q()),BlockPos.field_177992_a))if(piece.toString().contains(template)){
                MutableBoundingBox b=piece.func_74874_b();int x=(b.field_78897_a+b.field_78893_d)/2,z=b.field_78896_c-2;
                int y=p.func_71121_q().func_201676_a(net.minecraft.world.gen.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,x,z);TestMaps.cmd(p,"tp @s "+(x+.5)+" "+(y+1)+" "+(z+.5));returnLink(p);return 1;
            }
            p.func_146105_b(new StringTextComponent("设施尚未加载；请先前往城市广场。"),false);return 0;
        }
        if(action.equals("leader")){
            Data d=data(p.func_71121_q());
            TestMaps.cmd(p,"tp @s "+d.gymX+" "+d.gymY+" "+d.gymZ);returnLink(p);
            for(net.minecraft.entity.Entity entity:(Iterable<net.minecraft.entity.Entity>)p.func_71121_q().getEntities()::iterator)
                if(entity instanceof NPCTrainer && "leader".equals(entity.getPersistentData().func_74779_i("BackportGymRole"))){
                    TestMaps.cmd(p,"tp @s "+entity.func_226277_ct_()+" "+entity.func_226278_cu_()+" "+(entity.func_226281_cx_()+3));return 1;
                }
            p.func_146105_b(new StringTextComponent("已到达主道馆；区块加载后可再次使用命令定位馆主。"),false);return 1;
        }
        return 0;
    }
}

