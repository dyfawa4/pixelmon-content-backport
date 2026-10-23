import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import javax.tools.ToolProvider;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Execute the native ball description/folding and trainer opening paths before and after transformation. */
public final class TooltipFeedbackVerification {
 static void install(Path dir,String name,byte[] bytes,Set<String> methods)throws Exception{
  Path p=dir.resolve(name.replace('.','/')+".class");ClassNode out=new ClassNode(),src=new ClassNode();
  new ClassReader(Files.readAllBytes(p)).accept(out,0);new ClassReader(bytes).accept(src,0);
  for(MethodNode m:src.methods)if(methods.contains(m.name)||m.name.contains("backport$")||m.name.startsWith("lambda$getTooltipText")){
   out.methods.removeIf(v->v.name.equals(m.name)&&v.desc.equals(m.desc));out.methods.add(m);
  }
  ClassWriter writer=new ClassWriter(0);out.accept(writer);Files.write(p,writer.toByteArray());
 }
 static byte[] read(JarFile jar,String name)throws Exception{return jar.getInputStream(jar.getJarEntry(name.replace('.','/')+".class")).readAllBytes();}
 static void run(Path dir,String method)throws Exception{
  List<URL> urls=new ArrayList<>();urls.add(dir.toUri().toURL());for(String p:System.getProperty("java.class.path").split(File.pathSeparator))urls.add(Path.of(p).toUri().toURL());
  try(var loader=new URLClassLoader(urls.toArray(new URL[0]),ClassLoader.getPlatformClassLoader())){
   try{loader.loadClass("TooltipFeedbackCases").getMethod(method,String.class).invoke(null,dir.toString());}
   catch(java.lang.reflect.InvocationTargetException e){throw new AssertionError(method,e.getCause());}
  }
 }
 public static void verify(JarFile jar,byte[] ball,byte[] trainer)throws Exception{
  Path dir=Files.createTempDirectory(Path.of(".cache/boss-170"),"tooltip-");
  List<String> opts=new ArrayList<>(List.of("--release","11","-proc:none","-encoding","UTF-8","-cp",System.getProperty("java.class.path"),"-d",dir.toString()));
  for(String part:Files.readString(Path.of("tests/resources/TooltipFeedbackCases.java.txt")).split("(?m)^// FILE: ")){
   if(part.isBlank())continue;int end=part.indexOf('\n');Path p=dir.resolve(part.substring(0,end).trim());Files.createDirectories(p.getParent());Files.writeString(p,part.substring(end+1));opts.add(p.toString());
  }
  if(ToolProvider.getSystemJavaCompiler().run(null,null,null,opts.toArray(new String[0]))!=0)throw new AssertionError("Tooltip fixture compile");
  for(String lang:List.of("zh_cn","en_us"))Files.write(dir.resolve(lang+".json"),jar.getInputStream(jar.getJarEntry("assets/pixelmon/lang/"+lang+".json")).readAllBytes());
  String items="com.pixelmonmod.pixelmon.items.",npc="com.pixelmonmod.pixelmon.entities.npcs.NPCTrainer";
  install(dir,items+"PokeBallPart",read(jar,items+"PokeBallPart"),Set.of("getPokeBall"));
  install(dir,items+"PixelmonItem",read(jar,items+"PixelmonItem"),Set.of("func_77624_a"));
  install(dir,items+"PokeBallItem",read(jar,items+"PokeBallItem"),Set.of("getTooltipText","func_77624_a"));
  install(dir,npc,read(jar,npc),Set.of("getGreeting"));
  try(InputStream in=TooltipFeedbackVerification.class.getResourceAsStream("/local/pixelmon/backport/AuthorBossTeams.class")){
   install(dir,"local.pixelmon.backport.AuthorBossTeams",in.readAllBytes(),Set.of("is"));
  }
  run(dir,"before");
  install(dir,items+"PokeBallItem",ball,Set.of("getTooltipText","func_77624_a"));install(dir,npc,trainer,Set.of("getGreeting"));
  run(dir,"after");
  System.out.println("PASS native ball NBT identity and native Shift folding: park/origin expanded descriptions, stale native language override, bilingual and custom tooltip boundaries; saved/new author greeting, ordinary trainers retained; alloy wording. UI/language services isolated.");
 }
}
