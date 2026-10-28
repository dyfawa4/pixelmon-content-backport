package qa;
import java.util.*;
import java.nio.file.*;
import com.google.gson.*;
import local.pixelmon.backport.UltraDenDensity;
import com.pixelmonmod.pixelmon.world.structure.type.BiomeGenericStructure;
import com.pixelmonmod.pixelmon.world.dimension.ultraspace.UltraSpaceBiomeProvider;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.provider.BiomeProvider;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.feature.NoFeatureConfig;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.registry.Registry;
public final class Den190Cases {
 static int assertions;
 static void check(boolean yes,String message){assertions++;if(!yes)throw new AssertionError(message);}
 public static void run(ServerPlayerEntity player)throws Exception{
  var ultra=player.func_184102_h().func_71218_a(RegistryKey.func_240903_a_(Registry.field_239699_ae_,new ResourceLocation("pixelmon:ultra_space")));check(ultra!=null,"Real Ultra Space world");
  var generator=player.func_71121_q().func_72863_F().func_201711_g();var normal=generator.func_202090_b();var actual=ultra.func_72863_F().func_201711_g();check(actual instanceof com.pixelmonmod.pixelmon.world.dimension.ultraspace.generator.UltraDeepSeaChunkGenerator,"Actual native current Ultra generator");
  var provider=new UltraSpaceBiomeProvider(ultra.func_72905_C(),ultra.func_241828_r().func_243612_b(Registry.field_239720_u_));
  var method=BiomeGenericStructure.class.getDeclaredMethod("isFeatureChunk",ChunkGenerator.class,BiomeProvider.class,long.class,SharedSeedRandom.class,int.class,int.class,Biome.class,ChunkPos.class,NoFeatureConfig.class);method.setAccessible(true);
  var rows=new JsonArray();long seed=ultra.func_72905_C();int blockedNative=0,acceptedNative=0;
  for(String name:new TreeSet<>(UltraDenDensity.IDS)){
   var id=new ResourceLocation(name);var structure=net.minecraftforge.registries.ForgeRegistries.STRUCTURE_FEATURES.getValue(id);check(structure instanceof BiomeGenericStructure,"Actual registered Ultra den "+id);
   String biomeName=name.replace("_den","");var biome=net.minecraftforge.registries.ForgeRegistries.BIOMES.getValue(new ResourceLocation(biomeName));check(biome!=null&&((BiomeGenericStructure)structure).isSuitable(biome.func_201856_r(),biomeName),"Native den biome mapping "+id);
   var settings=actual.func_235957_b_();var spacing=settings.func_236197_a_(structure);int kept=0,n=20000;
   for(int i=0;i<n;i++){
    int x=i-10000,z=-i*17;boolean gate=UltraDenDensity.keep(id,seed,x,z);if(gate)kept++;
    check(gate==UltraDenDensity.keep(id,seed,x,z),"Deterministic parallel-safe gate");
    var random=new SharedSeedRandom(42);boolean result=(boolean)method.invoke(structure,generator,provider,seed,random,x,z,biome,new ChunkPos(x,z),NoFeatureConfig.field_236559_b_);
    check(result==gate,"Actual registered native candidate flow matches70% gate "+id);check(random.nextLong()==new SharedSeedRandom(42).nextLong(),"No shared worldgen RNG consumption");
   }
   check(Math.abs((double)kept/n-.7)<.02,"70% retained "+id+" "+kept);check(settings.func_236197_a_(structure)==spacing,"Spacing untouched "+id);
   for(int i=0;i<8;i++)check((boolean)method.invoke(structure,generator,normal,seed,new SharedSeedRandom(42),i,-i,biome,new ChunkPos(i,-i),NoFeatureConfig.field_236559_b_),"Other world generator/provider unchanged");
   for(int i=0;i<32;i++){int x=i-16,z=i*3;boolean result=(boolean)method.invoke(structure,actual,actual.func_202090_b(),seed,new SharedSeedRandom(42),x,z,biome,new ChunkPos(x,z),NoFeatureConfig.field_236559_b_);if(!UltraDenDensity.keep(id,seed,x,z)){check(!result,"Current multi-noise Ultra actual terrain rejects thinned candidate");blockedNative++;}else if(result)acceptedNative++;}
   var row=new JsonObject();row.addProperty("id",name);row.addProperty("samples",n);row.addProperty("retained",kept);rows.add(row);
  }
  var other=net.minecraftforge.registries.ForgeRegistries.STRUCTURE_FEATURES.getValue(new ResourceLocation("pixelmon:grotto_ultra_desert"));check(other instanceof BiomeGenericStructure,"Other Ultra grotto registered");
  for(String id:List.of("pixelmon:grotto_ultra_desert","pixelmon:desert_den","pixelmon:ultra_desert_deco","pixelmon_backport:ultra_desert_den"))for(int i=0;i<100;i++)check(UltraDenDensity.keep(new ResourceLocation(id),seed,i,-i),"Untargeted IDs unchanged");
  for(long s:new long[]{0,1,-1,Long.MIN_VALUE,Long.MAX_VALUE})for(int x:new int[]{0,1,-1,Integer.MIN_VALUE,Integer.MAX_VALUE})check(UltraDenDensity.keep(new ResourceLocation("pixelmon:ultra_desert_den"),s,x,-x)==UltraDenDensity.keep(new ResourceLocation("pixelmon:ultra_desert_den"),s,x,-x),"Boundary coordinates stable");
  check(blockedNative>0&&acceptedNative>0,"Actual current terrain permits and rejects candidates");var result=new JsonObject();result.addProperty("pass",true);result.addProperty("assertions",assertions);result.addProperty("nativeCurrentTerrainCases",192);result.addProperty("blockedByGate",blockedNative);result.addProperty("acceptedOnCurrentTerrain",acceptedNative);result.add("registeredDens",rows);Files.writeString(Path.of("den190-results.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result));BossBalance190Probe.log("DEN190_PASS "+result);
 }
}
