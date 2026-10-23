import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import javax.tools.ToolProvider;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Native melee reset plus actual shipped begin/tick methods, with isolated world/player services. */
public final class AuthorCombatFeedbackVerification {
 static byte[] bytes(String name)throws Exception{try(var in=ClassLoader.getSystemResourceAsStream(name.replace('.','/')+".class")){return in.readAllBytes();}}
 static void install(Path dir,String name,byte[] input,Set<String> methods)throws Exception{
  Path p=dir.resolve(name.replace('.','/')+".class");ClassNode dest=new ClassNode(),source=new ClassNode();new ClassReader(Files.readAllBytes(p)).accept(dest,0);new ClassReader(input).accept(source,0);
  for(MethodNode method:source.methods)if(methods.contains(method.name)||method.name.startsWith("lambda$func_70071_h_$")||method.name.startsWith("lambda$inCombat$")){dest.methods.removeIf(m->m.name.equals(method.name)&&m.desc.equals(method.desc));dest.methods.add(method);}
  ClassWriter out=new ClassWriter(0);dest.accept(out);Files.write(p,out.toByteArray());
 }
 static void run(Path dir,String method)throws Exception{
  List<URL> urls=new ArrayList<>();urls.add(dir.toUri().toURL());for(String p:System.getProperty("java.class.path").split(File.pathSeparator))urls.add(Path.of(p).toUri().toURL());
  try(var loader=new URLClassLoader(urls.toArray(new URL[0]),ClassLoader.getPlatformClassLoader())){try{loader.loadClass("AuthorCombatCases").getMethod(method).invoke(null);}catch(java.lang.reflect.InvocationTargetException e){throw new AssertionError(method,e.getCause());}}
 }
 public static void main(String[] args)throws Exception{
  Path dir=Files.createTempDirectory(Path.of(".cache/boss-170"),"retired-pvp-");
  List<String> options=new ArrayList<>(List.of("--release","11","-proc:none","-encoding","UTF-8","-cp",System.getProperty("java.class.path"),"-d",dir.toString()));
  for(String part:Files.readString(Path.of("tests/resources/AuthorCombatCases.java.txt")).split("(?m)^// FILE: ")){if(part.isBlank())continue;int end=part.indexOf('\n');Path p=dir.resolve(part.substring(0,end).trim());Files.createDirectories(p.getParent());Files.writeString(p,part.substring(end+1));options.add(p.toString());}
  if(ToolProvider.getSystemJavaCompiler().run(null,null,null,options.toArray(new String[0]))!=0)throw new AssertionError("Legacy retirement fixture compile");
  install(dir,"local.pixelmon.backport.AuthorCombatBoss",bytes("local.pixelmon.backport.AuthorCombatBoss"),Set.of("reset","func_70071_h_"));run(dir,"retired");
  System.out.println("PASS shipped legacy PVP entity retires, clears challenger and unlocks arena without rewards or room rebuild. New PVP generation and aliases removed.");
 }
}
