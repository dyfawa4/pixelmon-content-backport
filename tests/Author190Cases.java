package qa;
import java.util.*;
import java.nio.file.*;
import com.google.gson.*;
import local.pixelmon.backport.*;
import com.pixelmonmod.pixelmon.blocks.tileentity.EndTableTileEntity;
import com.pixelmonmod.pixelmon.entities.npcs.NPCTrainer;
import net.minecraft.block.*;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.feature.NoFeatureConfig;
import net.minecraftforge.registries.ForgeRegistries;
/** Controlled terrain in a disposable real world; invoke the production feature and native tile inventories. */
public final class Author190Cases {
    static BlockPos naturalHome,viewHome;static int naturalTicks,viewTicks,prizeSlot,prizeCount;
    public static void containers(ServerPlayerEntity player){
        if(viewHome==null)return;int tick=++viewTicks;if(tick!=80&&tick!=140)return;
        player.func_71053_j();var w=player.func_71121_q();var pos=viewHome.func_177982_a(-1,1,-5);var t=table(w,viewHome);empty(t);var visual=Update180Cases.stack(tick==80?"minecraft:diamond":"minecraft:netherite_scrap");visual.func_190920_e(tick==80?3:2);t.func_70299_a(tick==80?11:2,visual);var state=w.func_180495_p(pos);
        var hit=new BlockRayTraceResult(new net.minecraft.util.math.vector.Vector3d(pos.func_177958_n()+.5,pos.func_177956_o()+.5,pos.func_177952_p()+1),Direction.SOUTH,pos,false);
        check(state.func_177230_c().func_225533_a_(state,w,pos,player,Hand.MAIN_HAND,hit)==ActionResultType.SUCCESS&&player.field_71070_bA!=player.field_71069_bz,"Actual tea table block activation opens its native inventory even with workplace above");BossBalance190Probe.log("AUTHOR190_CONTAINER_PASS "+(tick==80?"diamond":"netherite_scrap")+" native activation; exact visual fixture after random reward checks");
    }
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static BlockState block(String id){return ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id)).func_176223_P();}
    static BlockPos candidate(ServerWorld w){
        for(int rx=0;rx<6;rx++)for(int x=rx*64;x<rx*64+40;x++)for(int z=0;z<40;z++)if(AuthorBossPolicy.candidate(w.func_72905_C(),x,z,false)){
            var p=new BlockPos(x*16+8,100,z*16+8);var b=w.func_226691_t_(p);var id=w.func_241828_r().func_243612_b(net.minecraft.util.registry.Registry.field_239720_u_).func_177774_c(b);var theme=AuthorBossPolicy.theme(w.func_234923_W_().func_240901_a_().toString(),b.func_201856_r().name(),id==null?"":id.toString());if(AuthorBossPolicy.candidate(w.func_72905_C(),x,z,false,theme)){w.func_175726_f(p);return p;}
        }throw new AssertionError("Eligible native candidate");
    }
    static void terrain(ServerWorld w,BlockPos h){
        for(var npc:w.func_217357_a(NPCTrainer.class,new AxisAlignedBB(h).func_186662_g(20)))if(AuthorBossTeams.is(npc))npc.func_70106_y();
        for(int x=-6;x<=6;x++)for(int z=-6;z<=7;z++){
            for(int y=1;y<155;y++)w.func_180501_a(h.func_177982_a(x,y,z),block("minecraft:air"),2);
            w.func_180501_a(h.func_177982_a(x,0,z),block("minecraft:stone"),2);
        }
        for(int y=1;y<=18;y++)w.func_180501_a(h.func_177982_a(0,y,0),block("minecraft:oak_log"),2);
        for(int y=1;y<=11;y++)w.func_180501_a(h.func_177982_a(2,y,1),block("minecraft:stone"),2);
        for(int y=1;y<=3;y++)w.func_180501_a(h.func_177982_a(0,y,7),block("minecraft:oak_log"),2);
    }
    static boolean generate(ServerWorld w,BlockPos h){return AuthorBossFeature.INSTANCE.get().func_241855_a(w,w.func_72863_F().func_201711_g(),new Random(190026),new BlockPos(h.func_177958_n(),0,h.func_177952_p()),NoFeatureConfig.field_236559_b_);}
    static EndTableTileEntity table(ServerWorld w,BlockPos h){return (EndTableTileEntity)w.func_175625_s(h.func_177982_a(-1,1,-5));}
    static void empty(EndTableTileEntity t){for(int i=0;i<t.func_70302_i_();i++)t.func_70299_a(i,ItemStack.field_190927_a);}
    static String prize(EndTableTileEntity t){int occupied=0;String id="";for(int i=0;i<t.func_70302_i_();i++){var stack=t.func_70301_a(i);if(stack.func_190926_b())continue;occupied++;prizeSlot=i;prizeCount=stack.func_190916_E();check(prizeCount>=1&&prizeCount<=3,"Table prize quantity is within 1..3");id=stack.func_77973_b().getRegistryName().toString();check(Set.of("minecraft:diamond","minecraft:netherite_scrap").contains(id),"Only the requested two prize types");}check(occupied==1,"One selected reward type/stack, never both types");check(t.getTileData().func_74767_n(AuthorRoomFurnishings.PRIZE),"Persistent once-only marker");return id;}
    public static void prepareNatural(Path path)throws Exception{
        net.minecraft.nbt.CompoundNBT root;try(var input=Files.newInputStream(path)){root=net.minecraft.nbt.CompressedStreamTools.func_74796_a(input);}
        var generation=root.func_74775_l("Data").func_74775_l("WorldGenSettings");generation.func_74757_a("generate_features",true);var generator=generation.func_74775_l("dimensions").func_74775_l("minecraft:overworld").func_74775_l("generator");check(generator.func_74779_i("type").equals("minecraft:flat"),"Private initial template only");generator.func_74778_a("type","minecraft:noise");generator.func_74772_a("seed",generation.func_74763_f("seed"));generator.func_74778_a("settings","minecraft:overworld");var biomes=new net.minecraft.nbt.CompoundNBT();biomes.func_74778_a("type","minecraft:fixed");biomes.func_74778_a("biome","minecraft:forest");generator.func_218657_a("biome_source",biomes);
        try(var output=Files.newOutputStream(path)){net.minecraft.nbt.CompressedStreamTools.func_74799_a(root,output);}
    }
    public static void preview(ServerPlayerEntity player){
        var w=player.func_71121_q();var h=AuthorRoomIndex.get(w).positions(false).stream().filter(p->p.func_177958_n()==104&&p.func_177952_p()==376).findFirst().orElseThrow();
        for(var npc:w.func_217357_a(NPCTrainer.class,new AxisAlignedBB(h).func_186662_g(9)))if(AuthorBossTeams.is(npc))npc.func_70107_b(h.func_177958_n()+3.5,h.func_177956_o()+1,h.func_177952_p()-3.5);
        player.func_200619_a(w,h.func_177958_n()+2.5,h.func_177956_o()+1.1,h.func_177952_p()+1,148,10);BossBalance190Probe.log("AUTHOR190_PREVIEW native furnished room; NPC moved aside only for private screenshot");
    }
    public static boolean natural(ServerPlayerEntity player)throws Exception{
        var w=player.func_71121_q();
        if(naturalHome==null){naturalHome=candidate(w);player.func_200619_a(w,naturalHome.func_177958_n()+4.5,65,naturalHome.func_177952_p()+4.5,145,12);return false;}
        if(++naturalTicks<80)return false;
        var room=AuthorRoomIndex.get(w).positions(false).stream().filter(p->p.func_177958_n()==naturalHome.func_177958_n()&&p.func_177952_p()==naturalHome.func_177952_p()).findFirst().orElseThrow(()->new AssertionError("Native decorated forest candidate did not index an author room"));
        check(AuthorRoomIndex.intact(w,room,false),"Actual decorated forest room intact");check(w.func_217357_a(NPCTrainer.class,new AxisAlignedBB(room).func_186662_g(9)).stream().anyMatch(AuthorBossTeams::is),"Actual native generation spawned its trainer");prize(table(w,room));
        var biome=w.func_241828_r().func_243612_b(net.minecraft.util.registry.Registry.field_239720_u_).func_177774_c(w.func_226691_t_(room));String generator=w.func_72863_F().func_201711_g().getClass().getSimpleName();check(biome!=null&&biome.toString().equals("minecraft:forest")&&generator.equals("NoiseChunkGenerator"),"Actual forest noise generator, no flat/fallback scene");var report=new JsonObject();report.addProperty("x",room.func_177958_n());report.addProperty("y",room.func_177956_o());report.addProperty("z",room.func_177952_p());report.addProperty("biome",biome.toString());report.addProperty("generator",generator);report.addProperty("seed",w.func_72905_C());report.addProperty("trigger","Native noise chunk decoration in forest biome; no direct feature invocation or artificial terrain");Files.writeString(Path.of("author190-natural-results.json"),report.toString());player.func_200619_a(w,room.func_177958_n()+4.5,room.func_177956_o()+1.1,room.func_177952_p()+4.5,145,12);BossBalance190Probe.log("AUTHOR190_NATURAL_PASS actual forest noise chunk decoration/index/native trainer/prize "+room);return true;
    }
    public static void run(ServerPlayerEntity player)throws Exception{
        var w=player.func_71121_q();var h=candidate(w);terrain(w,h);int oldHeight=w.func_201676_a(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,h.func_177958_n(),h.func_177952_p())-1;
        var result=new JsonObject();result.addProperty("x",h.func_177958_n());result.addProperty("z",h.func_177952_p());result.addProperty("old_center_height",oldHeight);result.addProperty("expected_floor",100);
        if(Files.exists(Path.of("author-baseline"))){boolean built=generate(w,h);check(!built,"Baseline rejects tree/terrain height obstacle");result.addProperty("baseline_generated",built);Files.writeString(Path.of("author190-results.json"),result.toString());BossBalance190Probe.log("AUTHOR190_BASELINE_PASS native feature rejects obstructed candidate");return;}
        check(AuthorBossFeature.floorHeight(w,h.func_177958_n(),h.func_177952_p(),"minecraft:overworld")==100,"Native median terrain ignores central trunk");
        var chest=h.func_177982_a(1,2,1);w.func_180501_a(chest,block("minecraft:chest"),2);var chestTile=w.func_175625_s(chest);check(!generate(w,h)&&w.func_175625_s(chest)==chestTile,"Occupied room container aborts before writes");w.func_180501_a(chest,block("minecraft:air"),2);
        check(generate(w,h),"Production feature generates despite natural tree and local height differences");check(AuthorRoomIndex.intact(w,h,false),"Actual room is complete and indexed-compatible");check(w.func_175623_d(h.func_177982_a(2,3,1))&&w.func_175623_d(h.func_177982_a(0,2,7)),"Interior hillside and porch trunk cleared");
        var npc=w.func_217357_a(NPCTrainer.class,new AxisAlignedBB(h).func_186662_g(9)).stream().filter(AuthorBossTeams::is).findFirst().orElseThrow();check(AuthorBossArenas.center(npc).equals(h),"Author actual home matches chosen floor");check(AuthorRoomIndex.get(w).positions(false).contains(h),"Native join indexes actual room");
        var aliasConstructor=AuthorBossLocator.RoomAlias.class.getDeclaredConstructor(boolean.class);aliasConstructor.setAccessible(true);var alias=aliasConstructor.newInstance(false);try(var search=new AuthorBossLocator.Search(w,h,alias)){check(h.equals(search.step()),"Compass returns actual generated room");}
        var chair=h.func_177982_a(-1,1,-4);check(w.func_180495_p(chair).func_177229_b(HorizontalBlock.field_185512_D)==Direction.NORTH,"Chair model front points towards the table/workplace");check(w.func_180495_p(h.func_177982_a(-1,2,-5)).func_177230_c()==ForgeRegistries.BLOCKS.getValue(new ResourceLocation("pixelmon:lightworkplace")),"Native light workplace sits on table");var t=table(w,h);result.addProperty("first_prize",prize(t));result.addProperty("first_prize_count",prizeCount);result.addProperty("first_prize_slot",prizeSlot);
        empty(t);AuthorRoomFurnishings.place(w,h);check(t.func_191420_l(),"Repeated furnishing never refills looted table");var saved=t.func_189515_b(new net.minecraft.nbt.CompoundNBT());var pos=h.func_177982_a(-1,1,-5);var state=w.func_180495_p(pos);w.func_180501_a(pos,block("minecraft:air"),2);w.func_180501_a(pos,state,2);t=table(w,h);t.func_230337_a_(state,saved);AuthorRoomFurnishings.place(w,h);check(t.func_191420_l()&&t.getTileData().func_74767_n(AuthorRoomFurnishings.PRIZE),"Native inventory NBT retains empty once-looted state");
        w.func_180501_a(chair,w.func_180495_p(chair).func_206870_a(HorizontalBlock.field_185512_D,Direction.SOUTH),2);npc.getPersistentData().func_74757_a("AuthorArenaFurnished4",true);npc.getPersistentData().func_82580_o(AuthorRoomFurnishings.TAG);npc.getPersistentData().func_74778_a("qa_user_progress","keep");var upgrade=AuthorBossArenas.class.getDeclaredMethod("upgrade",net.minecraft.entity.Entity.class,ServerWorld.class);upgrade.setAccessible(true);upgrade.invoke(null,npc,w);check(w.func_180495_p(chair).func_177229_b(HorizontalBlock.field_185512_D)==Direction.NORTH&&npc.getPersistentData().func_74767_n(AuthorRoomFurnishings.TAG)&&npc.getPersistentData().func_74779_i("qa_user_progress").equals("keep"),"Version4 room migration turns chair towards the workplace and preserves progress");check(table(w,h).func_191420_l(),"Migration does not replenish an already looted prize");
        t=table(w,h);t.getTileData().func_82580_o(AuthorRoomFurnishings.PRIZE);t.func_70299_a(0,Update180Cases.stack("minecraft:emerald"));AuthorRoomFurnishings.place(w,h);check(t.func_70301_a(0).func_77973_b().getRegistryName().toString().equals("minecraft:emerald")&&t.func_70301_a(4).func_190926_b(),"Occupied legacy table preserves player's contents");empty(t);AuthorRoomFurnishings.place(w,h);check(t.func_191420_l(),"Emptying occupied legacy table later cannot claim a delayed reward");
        Map<String,Integer> counts=new TreeMap<>();Map<Integer,Integer> quantities=new TreeMap<>(),slots=new TreeMap<>();for(int i=0;i<64;i++){empty(table(w,h));w.func_180501_a(pos,block("minecraft:air"),2);w.func_180501_a(pos,state,2);AuthorRoomFurnishings.place(w,h);counts.merge(prize(table(w,h)),1,Integer::sum);quantities.merge(prizeCount,1,Integer::sum);slots.merge(prizeSlot,1,Integer::sum);}check(counts.size()==2&&quantities.keySet().equals(Set.of(1,2,3))&&slots.size()>4,"Both types, all three quantities and variable native slots");result.add("quantity_samples",new Gson().toJsonTree(quantities));result.add("slot_samples",new Gson().toJsonTree(slots));result.add("prize_samples",new Gson().toJsonTree(counts));result.addProperty("generated",true);result.addProperty("container_preserved",true);result.addProperty("compass_matches",true);result.addProperty("migration_once",true);Files.writeString(Path.of("author190-results.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result));viewHome=h;preview(player);BossBalance190Probe.log("AUTHOR190_PASS actual feature tree/slope clearance, protected container, native home/index/compass, chair facing workplace, two prizes, no refill, occupied/NBT/version4 migration "+counts);
    }
}
