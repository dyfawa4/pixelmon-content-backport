import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import com.google.gson.*;
import local.pixelmon.backport.EndCityPolicy;
import javax.tools.ToolProvider;

public final class EndCityVerification {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 public static void main(String[] args)throws Exception{
  for(int sign:new int[]{-1,1}){
   int x=sign>0?2048:-2208,z=sign>0?4096:-4256;
   check(EndCityPolicy.supported(x,z,x+160,z+160,(a,b)->true),"Complete outer island city");
   AtomicInteger reads=new AtomicInteger();
   check(!EndCityPolicy.supported(x,z,x+160,z+160,(a,b)->{reads.incrementAndGet();return a!=x+17||b!=z+19;}),"One unsampled void column must reject the entire city");
   check(reads.get()>400,"Full footprint checked after coarse pass");
   check(!EndCityPolicy.supported(x,z,x+160,z+160,(a,b)->a<x+80),"Small island cannot support a partial city");
  }
  check(!EndCityPolicy.supported(-90,-90,90,90,(a,b)->true),"Center island forbidden");
  check(!EndCityPolicy.supported(1000,-30,1160,130,(a,b)->true),"Entire footprint, not only origin, excludes center");
  check(!EndCityPolicy.supported(2000,2000,3000,3000,(a,b)->{throw new AssertionError("Unbounded terrain query");}),"Oversized data-pack start rejected before querying terrain");
  var cities=new JsonParser().parse(Files.readString(Path.of("resources/backport/gym_cities.json"))).getAsJsonObject();
  Set<String> biomes=new HashSet<>();cities.getAsJsonObject("newThemes").getAsJsonObject("psychic").getAsJsonArray("biomes").forEach(e->biomes.add(e.getAsString()));
  check(biomes.equals(Set.of("minecraft:small_end_islands","minecraft:end_midlands","minecraft:end_highlands","minecraft:end_barrens")),"Only four outer End biomes");
  Path dir=Files.createTempDirectory(Path.of(".cache/update-20261021"),"end-city-");List<String> options=new ArrayList<>(List.of("--release","11","-proc:none","-encoding","UTF-8","-cp",System.getProperty("java.class.path"),"-d",dir.toString()));
  for(String part:Files.readString(Path.of("tests/resources/EndCityCases.java.txt")).split("(?m)^// FILE: ")){if(part.isBlank())continue;int end=part.indexOf('\n');Path file=dir.resolve(part.substring(0,end).trim());Files.createDirectories(file.getParent());Files.writeString(file,part.substring(end+1));options.add(file.toString());}
  options.add("src/local/pixelmon/backport/EndGymCities.java");options.add("src/local/pixelmon/backport/EndCityPolicy.java");
  check(ToolProvider.getSystemJavaCompiler().run(null,null,null,options.toArray(new String[0]))==0,"Terrain adapter fixture compilation");
  List<java.net.URL> urls=new ArrayList<>();urls.add(dir.toUri().toURL());for(String path:System.getProperty("java.class.path").split(java.io.File.pathSeparator))urls.add(Path.of(path).toUri().toURL());
  try(var loader=new java.net.URLClassLoader(urls.toArray(new java.net.URL[0]),ClassLoader.getPlatformClassLoader())){try{loader.loadClass("EndCityCases").getMethod("run").invoke(null);}catch(java.lang.reflect.InvocationTargetException e){throw new AssertionError(e.getCause());}}
  System.out.println("PASS End city policy: four outer biomes, full footprint and single-column void rejection, small island and central exclusion, negative coordinates and bounded queries. Natural terrain generation remains a live acceptance check.");
 }
}
