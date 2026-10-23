import java.io.*;
import java.nio.file.*;
import java.net.*;
import java.util.*;
import javax.tools.ToolProvider;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Execute native original/patched tooltip paths, without native world or language initialization. */
public final class StatueDisplayVerification {
 static void install(Path dir,String name,byte[] bytes,String method)throws Exception{
  Path path=dir.resolve(name.replace('.','/')+".class");ClassNode out=new ClassNode(),source=new ClassNode();new ClassReader(Files.readAllBytes(path)).accept(out,0);new ClassReader(bytes).accept(source,0);
  for(MethodNode m:source.methods)if(m.name.equals(method)||m.name.contains("backport$statueName")||m.name.contains("backport$skipPixelmonHorseStats")){out.methods.removeIf(d->d.name.equals(m.name)&&d.desc.equals(m.desc));out.methods.add(m);}
  ClassWriter writer=new ClassWriter(0);out.accept(writer);Files.write(path,writer.toByteArray());
 }
 static void run(Path dir,String name)throws Exception{
  List<URL> urls=new ArrayList<>();urls.add(dir.toUri().toURL());for(String path:System.getProperty("java.class.path").split(File.pathSeparator))urls.add(Path.of(path).toUri().toURL());
  try(var loader=new URLClassLoader(urls.toArray(new URL[0]),ClassLoader.getPlatformClassLoader())){try{loader.loadClass("StatueCases").getMethod(name).invoke(null);}catch(java.lang.reflect.InvocationTargetException e){throw new AssertionError(name,e.getCause());}}
 }
 public static void verify(byte[] original,byte[] patched,byte[] horseBefore,byte[] horseAfter)throws Exception{
  Path dir=Files.createTempDirectory(Path.of(".cache/boss-170"),"statue-");List<String> opts=new ArrayList<>(List.of("--release","11","-proc:none","-encoding","UTF-8","-cp",System.getProperty("java.class.path"),"-d",dir.toString()));
  for(String part:Files.readString(Path.of("tests/resources/StatueCases.java.txt")).split("(?m)^// FILE: ")){if(part.isBlank())continue;int end=part.indexOf('\n');Path p=dir.resolve(part.substring(0,end).trim());Files.createDirectories(p.getParent());Files.writeString(p,part.substring(end+1));opts.add(p.toString());}
  if(ToolProvider.getSystemJavaCompiler().run(null,null,null,opts.toArray(new String[0]))!=0)throw new AssertionError("Statue display fixture compilation");
  String base="com.pixelmonmod.pixelmon.entities.pixelmon.AbstractBaseEntity",horse="snownee.jade.addon.vanilla.HorseProvider";
  install(dir,base,original,"func_200200_C_");install(dir,horse,horseBefore,"appendBody");run(dir,"original");
  install(dir,base,patched,"func_200200_C_");install(dir,horse,horseAfter,"appendBody");run(dir,"fixed");
  System.out.println("PASS native English statue name and Jade missing horse.jump_strength reproduced; patched deferred Chinese/English names, custom label retained, null-species fallback, Pixelmon/statue excluded only from horse stats, real horses unchanged. World/language services isolated.");
 }
}
