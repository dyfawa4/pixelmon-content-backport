package local.pixelmon.backport;
import java.util.*;
import net.minecraft.world.*;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.gen.*;
import net.minecraft.world.gen.feature.*;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.*;

/** Compact encounters generated within one new chunk, after vegetation. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid=Content.NS)
public final class AuthorBossFeature extends Feature<NoFeatureConfig> {
    public static final DeferredRegister<Feature<?>> FEATURES=DeferredRegister.create(ForgeRegistries.FEATURES,Content.NS);
    public static final net.minecraftforge.fml.RegistryObject<AuthorBossFeature> INSTANCE=FEATURES.register("mysterious_man_arena",AuthorBossFeature::new);
    private static ConfiguredFeature<?,?> configured;
    public AuthorBossFeature(){super(NoFeatureConfig.field_236558_a_);}
    public static void configure(){configured=INSTANCE.get().func_225566_b_(NoFeatureConfig.field_236559_b_);}
    @SubscribeEvent public static void biome(BiomeLoadingEvent event){
        if(configured!=null)event.getGeneration().getFeatures(GenerationStage.Decoration.TOP_LAYER_MODIFICATION).add(()->configured);
    }
    @Override public boolean func_241855_a(ISeedReader region,ChunkGenerator generator,Random random,BlockPos origin,NoFeatureConfig config){
        ServerWorld world=region.func_201672_e();String dimension=world.func_234923_W_().func_240901_a_().toString();
        int x=(origin.func_177958_n()>>4)*16+8,z=(origin.func_177952_p()>>4)*16+8;
        int y=region.func_201676_a(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,x,z)-1;
        if(dimension.equals("minecraft:the_nether")){
            y=-1;for(int py=100;py>=16;py--)if(region.func_180495_p(new BlockPos(x,py,z)).func_185904_a().func_76220_a()&&region.func_175623_d(new BlockPos(x,py+1,z))&&region.func_175623_d(new BlockPos(x,py+2,z))){y=py;break;}
        }
        if(y<5||y>240)return false;BlockPos home=new BlockPos(x,y,z);
        var biome=region.func_226691_t_(home);
        ResourceLocation biomeId=world.func_241828_r().func_243612_b(net.minecraft.util.registry.Registry.field_239720_u_).func_177774_c(biome);
        String name=biomeId==null?"":biomeId.toString(),category=biome.func_201856_r().name().toLowerCase(java.util.Locale.ROOT);
        boolean physical=AuthorBossPolicy.physical(dimension,category,name);
        String theme=AuthorBossPolicy.theme(dimension,category,name);
        if(!physical&&theme==null||!AuthorBossPolicy.candidate(world.func_72905_C(),x>>4,z>>4,physical))return false;
        var holder=region.func_217353_a(x>>4,z>>4,net.minecraft.world.chunk.ChunkStatus.field_222607_c,false);
        if(holder==null||holder.func_201604_d().values().stream().anyMatch(refs->!refs.isEmpty())
            ||holder.func_201609_c().values().stream().anyMatch(start->start!=null&&start.func_75069_d()))return false;
        // Keep all writes in this chunk and never replace an existing container/structure interior.
        for(int dx=-6;dx<=6;dx++)for(int dz=-6;dz<=6;dz++){
            int ground=region.func_201676_a(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,x+dx,z+dz)-1;
            if(!dimension.equals("minecraft:the_nether")&&Math.abs(ground-y)>3)return false;
            for(int dy=0;dy<=7;dy++)if(region.func_175625_s(home.func_177982_a(dx,dy,dz))!=null)return false;
        }
        String material=AuthorRoomPalette.choose(theme,name,world.func_72905_C()^home.func_218275_a());
        build(region,home,physical,material);Entity boss=create(world,home,physical,theme);boss.getPersistentData().func_74778_a(AuthorRoomPalette.TAG,material);
        return region.func_217376_c(boss);
    }
    public static Entity create(ServerWorld world,BlockPos home,boolean physical,String theme){
        if(physical)throw new IllegalArgumentException("Author PVP arenas have been retired");
        Entity boss=AuthorBossTeams.create(world,home,theme);boss.getPersistentData().func_74778_a(AuthorRoomPalette.TAG,material(world,home,theme));return boss;
    }
    public static String material(ServerWorld world,BlockPos home,String theme){
        var biome=world.func_226691_t_(home);var id=world.func_241828_r().func_243612_b(net.minecraft.util.registry.Registry.field_239720_u_).func_177774_c(biome);
        String name=id==null?"":id.toString();
        if(theme==null||theme.isEmpty())theme=AuthorBossPolicy.theme(world.func_234923_W_().func_240901_a_().toString(),biome.func_201856_r().name(),name);
        return AuthorRoomPalette.choose(theme,name,world.func_72905_C()^home.func_218275_a());
    }
    /** Upgrade owned legacy rooms without clearing player edits outside the obsolete blocks. */
    public static void upgrade(ServerWorld world,BlockPos home,boolean physical){
        upgrade(world,home,physical,material(world,home,null));
    }
    public static void upgrade(ServerWorld world,BlockPos home,boolean physical,String material){
        if(physical)return;
        for(int x:new int[]{-6,6})for(int z:new int[]{-6,6})if(!world.func_175667_e(home.func_177982_a(x,0,z)))return;
        Block quartz=ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft:chiseled_quartz_block"));
        Block bricks=ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft:stone_bricks"));
        BlockState wool=ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft:white_wool")).func_176223_P();
        BlockState wall=ForgeRegistries.BLOCKS.getValue(new ResourceLocation(material)).func_176223_P();
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++){
            BlockPos pos=home.func_177982_a(x,0,z);if(world.func_180495_p(pos).func_177230_c()==quartz)world.func_180501_a(pos,wool,2);
        }
        for(int x:new int[]{-4,4})for(int z:new int[]{-4,4}){
            BlockPos old=home.func_177982_a(x,6,z),top=old.func_177982_a(0,1,0);
            Block roof=world.func_180495_p(top).func_177230_c();
            if(world.func_180495_p(old).func_177230_c()==Blocks.field_180398_cJ&&roof==bricks){
                world.func_180501_a(old,Blocks.field_150350_a.func_176223_P(),2);world.func_180501_a(top,Blocks.field_180398_cJ.func_176223_P(),2);
            }
        }
        for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++)for(int y=0;y<=7;y++){
            if(y!=0&&y!=7&&Math.abs(x)!=6&&Math.abs(z)!=6)continue;
            BlockPos p=home.func_177982_a(x,y,z);
            if(world.func_180495_p(p).func_177230_c()==bricks)world.func_180501_a(p,wall,2);
        }
        for(int x=-1;x<=1;x++){BlockPos p=home.func_177982_a(x,0,7);if(world.func_180495_p(p).func_177230_c()==bricks)world.func_180501_a(p,wall,2);}
    }
    public static void build(net.minecraft.world.IWorld world,BlockPos home,boolean physical){
        build(world,home,physical,world instanceof ServerWorld?material((ServerWorld)world,home,null):"minecraft:stone_bricks");
    }
    public static void build(net.minecraft.world.IWorld world,BlockPos home,boolean physical,String material){
        if(physical)return;
        BlockState floor=ForgeRegistries.BLOCKS.getValue(new ResourceLocation(material)).func_176223_P(),wall=floor;
        BlockState accent=ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft:white_wool")).func_176223_P();
        for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++)for(int y=0;y<=7;y++){
            BlockState state=Blocks.field_150350_a.func_176223_P();
            if(y==0||y==7||((Math.abs(x)==6||Math.abs(z)==6)&&y<=6))state=wall;
            if(y==0&&(Math.abs(x)<=4&&Math.abs(z)<=4))state=accent;
            if(z==6&&Math.abs(x)<=1&&y>=1&&y<=3)state=Blocks.field_150350_a.func_176223_P();
            if(y>=3&&y<=4&&(Math.abs(x)==6&&Math.abs(z)<=3||Math.abs(z)==6&&Math.abs(x)>=3&&Math.abs(x)<=4))state=Blocks.field_150359_w.func_176223_P();
            if(y==7&&Math.abs(x)==4&&Math.abs(z)==4)state=Blocks.field_180398_cJ.func_176223_P();
            if(y==7&&x==0&&z==0)state=Blocks.field_180398_cJ.func_176223_P();
            world.func_180501_a(home.func_177982_a(x,y,z),state,2);
        }
        for(int x=-1;x<=1;x++)world.func_180501_a(home.func_177982_a(x,0,7),floor,2);
        AuthorRoomFurnishings.place(world,home);
    }
}
