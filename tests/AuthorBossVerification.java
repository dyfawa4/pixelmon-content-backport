import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import com.google.gson.*;
import javax.tools.ToolProvider;
import local.pixelmon.backport.AuthorBossPolicy;
import local.pixelmon.backport.AuthorRoomPalette;

/** Execute roster selection against supplied species metadata with isolated world/item services. */
public final class AuthorBossVerification {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 public static void main(String[] args)throws Exception{
  String[][] walls={{"plains","stone_bricks"},{"ocean","prismarine_bricks"},{"dark_forest","brown_terracotta"},{"forest","white_terracotta"},{"badlands","yellow_glazed_terracotta"},{"desert","orange_terracotta"},{"swamp","bricks"},{"taiga","stripped_spruce_log"},{"jungle","lime_glazed_terracotta"},{"paradox","purpur_block"},{"nether","polished_basalt"},{"mountain","stone_bricks"},{"snow","stone_bricks"}};
  for(String[] row:walls){String wall=AuthorRoomPalette.choose(row[0],"minecraft:test",123);check(wall.equals("minecraft:"+row[1])&&AuthorRoomPalette.wall(wall),"Author biome shell "+Arrays.toString(row));}
  Set<String> ultraWalls=new HashSet<>();for(int seed=0;seed<1000;seed++){
   String wall=AuthorRoomPalette.choose("ultra","pixelmon:ultra_forest",seed);ultraWalls.add(wall);
   check(wall.equals(AuthorRoomPalette.choose("ultra","pixelmon:ultra_forest",seed))&&AuthorRoomPalette.wall(wall),"Stable Ultra shell");
   check(AuthorRoomPalette.choose("ultra","pixelmon:ultra_desert",seed).equals("pixelmon:crystal_block"),"Ultra desert overrides random shell");
  }
  check(ultraWalls.equals(new HashSet<>(AuthorRoomPalette.ULTRA)),"All five Ultra shells available");
  for(String dim:List.of("pixelmon:drowned_world","other:drowned_world"))check(AuthorBossPolicy.theme(dim,"plains","minecraft:plains")==null,"Drowned exclusion");
  check(!AuthorBossPolicy.physical("minecraft:overworld","mushroom","minecraft:mushroom_fields"),"PVP removed from mushroom spawn");
  check(!AuthorBossPolicy.physical("minecraft:the_end","mushroom","minecraft:mushroom_fields"),"No melee outside overworld");
  check(AuthorBossPolicy.theme("minecraft:overworld","mushroom","minecraft:mushroom_fields")==null,"No trainer mushroom spawn");
  String[][] routes={{"minecraft:the_end","none","end_highlands","paradox"},{"pixelmon:ultra_space","none","ultra_forest","ultra"},{"minecraft:the_nether","nether","nether_wastes","nether"},{"minecraft:overworld","forest","dark_forest","dark_forest"},{"minecraft:overworld","beach","snowy_beach","ocean"},{"minecraft:overworld","mesa","badlands","badlands"},{"minecraft:overworld","extreme_hills","mountains","mountain"},{"minecraft:overworld","savanna","savanna","desert"},{"minecraft:overworld","taiga","snowy_taiga","taiga"},{"minecraft:overworld","icy","snowy_tundra","snow"}};
  for(String[] r:routes)check(r[3].equals(AuthorBossPolicy.theme(r[0],r[1],r[2])),"Theme route "+Arrays.toString(r));
  check(AuthorBossPolicy.damage(1000)==20&&AuthorBossPolicy.damage(7)==7&&AuthorBossPolicy.damage(-1)==0,"Final damage cap");
  for(int i=0;i<10000;i++){Random r=new Random(i);int money=AuthorBossPolicy.money(r);check(money>=500000&&money<=1000000,"Money bounds");List<Integer>d=AuthorBossPolicy.draw(78,5,r);check(d.size()==5&&new HashSet<>(d).size()==5&&d.stream().allMatch(n->n>=0&&n<78),"Distinct prize rolls");}
  JsonObject prizes=new JsonParser().parse(Files.readString(Path.of("resources/backport/author_boss_rewards.json"))).getAsJsonObject();
  check(prizes.get("extra_draws").getAsInt()==5,"Five author-only random rewards");
  check(prizes.getAsJsonArray("entries").size()==78&&!prizes.get("replacement").getAsBoolean(),"Complete 78-entry pool");
  int z=0;Set<String> ids=new HashSet<>();
  for(JsonElement value:prizes.getAsJsonArray("entries")){JsonObject e=value.getAsJsonObject();String id=e.has("ball")?"ball:"+e.get("ball").getAsString():e.get("item").getAsString();check(ids.add(id),"Duplicate reward entry "+id);int count=e.get("count").getAsInt();check(count==(Set.of("metalbarrels:diamond_barrel","waystones:waystone","pixelmon:rare_candy").contains(id)?3:1),"Screenshot quantity "+id);if(id.endsWith("_z"))z++;if(id.equals("minecraft:enchanted_book"))check(e.get("enchantment").getAsString().equals("minecraft:mending")&&e.get("enchantment_level").getAsInt()==1,"Mending book NBT");}
  check(z==17,"All exclusive Z crystals");
  Path dir=Files.createTempDirectory(Path.of(".cache/boss-170"),"rosters-");JsonArray metadata=new JsonArray();Map<Integer,JsonObject> species=new TreeMap<>();
  try(ZipFile jar=new ZipFile(args[0])){for(var it=jar.entries();it.hasMoreElements();){var e=it.nextElement();if(e.getName().startsWith("data/pixelmon/species/")&&e.getName().endsWith(".json"))try(var reader=new InputStreamReader(jar.getInputStream(e),java.nio.charset.StandardCharsets.UTF_8)){JsonObject s=new JsonParser().parse(reader).getAsJsonObject();species.put(s.get("dex").getAsInt(),s);}}}
  JsonObject additions=new JsonParser().parse(Files.readString(Path.of("resources/backport/content.json"))).getAsJsonObject();
  for(JsonElement patch:additions.getAsJsonArray("patches")){JsonObject s=patch.getAsJsonObject().getAsJsonObject("species");species.putIfAbsent(s.get("dex").getAsInt(),s);}
  JsonObject cap=new JsonParser().parse(species.get(25).toString()).getAsJsonObject();cap.addProperty("dex",1026);cap.addProperty("name","CapPikachu");JsonObject base=cap.getAsJsonArray("forms").get(0).getAsJsonObject();JsonArray tag=new JsonArray();tag.add("legendary");base.add("tags",tag);base.add("evolutions",new JsonArray());species.put(1026,cap);
  species.values().forEach(metadata::add);Path input=dir.resolve("species.json");Files.writeString(input,metadata.toString());
  List<String> options=new ArrayList<>(List.of("--release","11","-proc:none","-encoding","UTF-8","-cp",System.getProperty("java.class.path"),"-d",dir.toString()));
  for(String part:Files.readString(Path.of("tests/resources/AuthorBossCases.java.txt")).split("(?m)^// FILE: ")){if(part.isBlank())continue;int end=part.indexOf('\n');Path p=dir.resolve(part.substring(0,end).trim());Files.createDirectories(p.getParent());Files.writeString(p,part.substring(end+1));options.add(p.toString());}
  options.add("src/local/pixelmon/backport/AuthorBossTeams.java");options.add("src/local/pixelmon/backport/AuthorBossPolicy.java");
  check(ToolProvider.getSystemJavaCompiler().run(null,null,null,options.toArray(new String[0]))==0,"Boss fixture compile");
  List<URL> urls=new ArrayList<>();urls.add(dir.toUri().toURL());for(String p:System.getProperty("java.class.path").split(File.pathSeparator))urls.add(Path.of(p).toUri().toURL());
  try(var loader=new URLClassLoader(urls.toArray(new URL[0]),ClassLoader.getPlatformClassLoader())){try{loader.loadClass("AuthorBossCases").getMethod("run",String.class).invoke(null,input.toString());}catch(java.lang.reflect.InvocationTargetException e){throw new AssertionError("Boss roster fixture",e.getCause());}}
  System.out.println("PASS boss dimension/biome exclusions, 10000 money/distinct-loot rolls, 78 exact screenshot prizes and mending/all17 exclusive Z. World/renderer gameplay remains manual.");
 }
}
