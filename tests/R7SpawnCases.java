package qa;
import java.util.*;
import java.nio.file.*;
import local.pixelmon.backport.*;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.*;
import net.minecraft.util.*;
import net.minecraft.item.*;
import net.minecraft.util.text.*;
import net.minecraft.command.*;
import com.pixelmonmod.pixelmon.spawning.*;
import com.pixelmonmod.pixelmon.api.events.spawning.LegendaryCheckSpawnsEvent;
import net.minecraftforge.registries.ForgeRegistries;
/** Native translated output, crafting manager and real structure-start regression checks. */
public final class R7SpawnCases {
 static int checks;
 static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
 public static void run(ServerPlayerEntity player)throws Exception{
  var world=player.func_71121_q();var output=new ArrayList<ITextComponent>();
  ICommandSource sink=new ICommandSource(){public void func_145747_a(ITextComponent text,UUID id){output.add(text);}public boolean func_195039_a(){return true;}public boolean func_195040_b(){return true;}public boolean func_195041_r_(){return false;}};
  var source=new CommandSource(sink,player.func_213303_ch(),new net.minecraft.util.math.vector.Vector2f(0,0),world,4,"R7",new StringTextComponent("R7"),world.func_73046_m(),player);
  var method=com.pixelmonmod.pixelmon.api.spawning.archetypes.algorithms.checkspawns.LegendaryCheckSpawns.class.getDeclaredMethod("lambda$checkSpawns$0",LegendarySpawner.class,LegendaryCheckSpawnsEvent.class,HashMap.class,List.class,CommandSource.class);method.setAccessible(true);
  var results=new com.google.gson.JsonObject();
  for(boolean boss:new boolean[]{true,false,true}){
   output.clear();var spawner=boss?PixelmonSpawning.megaBossSpawner:PixelmonSpawning.legendarySpawner;
   var event=new LegendaryCheckSpawnsEvent(source);event.shouldShowTime=true;event.shouldShowChance=true;
   var candidates=new HashMap<String,Double>();candidates.put("Kyogre",12.5);candidates.put("Beedrill",87.5);
   method.invoke(spawner.checkSpawns,spawner,event,candidates,List.of(),source);
   check(output.size()>=4,"Native query emits timer, chance, pool and candidates");
   check(output.get(0).getString().contains("Boss")==boss,"Timer distinguishes actual Boss/legendary spawner");
   check(output.get(1).getString().contains("Boss")==boss,"Chance distinguishes actual Boss/legendary spawner");
   check(output.get(2).getString().contains("Boss")==boss,"Candidate heading distinguishes actual Boss/legendary spawner");
   float chance=boss?com.pixelmonmod.pixelmon.api.config.PixelmonConfigProxy.getSpawning().getBossSpawning().getBossSpawnChance():com.pixelmonmod.pixelmon.api.config.PixelmonConfigProxy.getSpawning().getLegendarySpawnChance();
   check(output.get(1).getString().contains(new java.text.DecimalFormat(".##").format(chance*100)+"%"),"Native configured chance retained");
   var rows=new com.google.gson.JsonArray();for(var row:output)rows.add(row.getString());results.add(boss?"boss":"legendary",rows);
  }
  var inv=new net.minecraft.inventory.CraftingInventory(new net.minecraft.inventory.container.Container(null,-1){public boolean func_75145_c(net.minecraft.entity.player.PlayerEntity p){return true;}},3,3);
  for(int slot:new int[]{1,3,5})inv.func_70299_a(slot,new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("minecraft:black_wool"))));
  inv.func_70299_a(4,new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("pixelmon:rocky_helmet"))));inv.func_70299_a(7,new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("minecraft:blaze_rod"))));
  var recipe=(net.minecraft.item.crafting.ICraftingRecipe)world.func_199532_z().func_215367_a(new ResourceLocation(Content.NS,"utility_umbrella")).orElseThrow();
  check(recipe.func_77569_a(inv,world),"Actual umbrella recipe accepts pictured grid");check(recipe.func_77572_b(inv).func_77973_b()==Backport.item("utility_umbrella"),"Actual recipe yields umbrella");
  inv.func_70299_a(1,new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("minecraft:white_wool"))));check(!recipe.func_77569_a(inv,world),"White wool cannot replace black wool");
  var info=world.func_72912_H();var saved=new BlockPos(info.func_76079_c(),info.func_76075_d(),info.func_76074_e());float angle=info.func_241860_d();
  try{
   int sx=50000,sz=-60000;world.func_241124_a__(new BlockPos(sx,70,sz),0);
   check(!AltarSpacing.outsideSpawn(world,sx,sz,sx,sz),"Offset world spawn excluded");check(!AltarSpacing.outsideSpawn(world,sx+999,sz,sx+999,sz),"999 excluded");check(AltarSpacing.outsideSpawn(world,sx+1000,sz,sx+1000,sz),"Exact 1000 boundary allowed");
   check(!AltarSpacing.outsideSpawn(world,sx+990,sz-5,sx+1010,sz+5),"Building whose edge enters forbidden circle excluded");
   check(!AltarSpacing.outsideSpawn(world,sx+707,sz+707,sx+707,sz+707),"Diagonal inside radius excluded");check(AltarSpacing.outsideSpawn(world,sx+708,sz+708,sx+708,sz+708),"Diagonal outside radius allowed");
   check(AltarSpacing.outsideSpawn(world,0,0,10,10),"World origin does not replace actual spawn");
   check(!AltarSpacing.author(world,new BlockPos(sx,80,sz)),"Author reservation rejects spawn zone");
   int n=0;var starts=new com.google.gson.JsonArray();
   for(String type:new TreeSet<>(AltarSpacing.SPAWN_PROTECTED)){
    if(type.equals(AltarSpacing.AUTHOR))continue;
    int x=sx+n++*10000;world.func_241124_a__(new BlockPos(x,70,sz),0);
    var structure=ForgeRegistries.STRUCTURE_FEATURES.getValue(new ResourceLocation(type));
    var rejected=Final190Cases.start(world,structure,x>>4,sz>>4);check(!rejected.func_75069_d(),"Native start blocks spawn zone "+type);
    // Move only this private world's spawn, then retry the same start. A rejected start must not reserve spacing.
    world.func_241124_a__(new BlockPos(x+5000,70,sz+5000),0);
    var allowed=Final190Cases.start(world,structure,x>>4,sz>>4);check(allowed.func_75069_d(),"Native same start allowed after world spawn moves "+type);
    check(!Final190Cases.start(world,structure,(x>>4)+1,sz>>4).func_75069_d(),"Existing 3000 spacing remains "+type);starts.add(type);
   }
   results.add("nativeStarts",starts);
  }finally{world.func_241124_a__(saved,angle);}
  Final190Cases.index();
  BlockPos candidate=null;outer:for(int x=2048;x<2088;x++)for(int z=2048;z<2088;z++)if(AuthorBossPolicy.candidate(world.func_72905_C(),x,z,false,"plains")){candidate=new BlockPos(x*16+8,80,z*16+8);break outer;}
  check(candidate!=null,"Actual author candidate available for spawn-zone test");
  try{
   world.func_241124_a__(candidate,0);
   check(!AuthorBossFeature.INSTANCE.get().func_241855_a(world,world.func_72863_F().func_201711_g(),new Random(27),candidate,net.minecraft.world.gen.feature.NoFeatureConfig.field_236559_b_),"Actual author feature rejects valid candidate near spawn");
   check(!AuthorRoomIndex.get(world).positions(false).contains(candidate),"Rejected author is not added to persistent index");
  }finally{world.func_241124_a__(saved,angle);}
  Final190Cases.authorFeature(world);
  results.addProperty("checks",checks);Files.writeString(Path.of("r7-native.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(results));BossBalance190Probe.log("R7_NATIVE_PASS "+checks);
 }
}
