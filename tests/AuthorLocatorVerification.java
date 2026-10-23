import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import javax.tools.ToolProvider;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Execute the shipped locator/index with isolated world, ticket and biome services. */
public final class AuthorLocatorVerification {
 public static void main(String[] args)throws Exception {
  Path dir=Files.createTempDirectory(Path.of(".cache/boss-170"),"locator-");
  List<String> options=new ArrayList<>(List.of("--release","11","-proc:none","-encoding","UTF-8","-cp",System.getProperty("java.class.path"),"-d",dir.toString()));
  Set<String> fixtures=new HashSet<>();
  for(String part:Files.readString(Path.of("tests/resources/AuthorLocatorCases.java.txt")).split("(?m)^// FILE: ")){if(part.isBlank())continue;int end=part.indexOf('\n');String path=part.substring(0,end).trim();Path p=dir.resolve(path);Files.createDirectories(p.getParent());Files.writeString(p,part.substring(end+1));fixtures.add(path.substring(0,path.length()-5).replace('/','.'));options.add(p.toString());}
  if(ToolProvider.getSystemJavaCompiler().run(null,null,null,options.toArray(new String[0]))!=0)throw new AssertionError("Locator fixture compile");
  // Child-first fixtures isolate native world initialization, never replace the production search/index.
  List<URL> urls=new ArrayList<>();urls.add(dir.toUri().toURL());for(String p:System.getProperty("java.class.path").split(File.pathSeparator))urls.add(Path.of(p).toUri().toURL());
  try(var loader=new URLClassLoader(urls.toArray(new URL[0]),ClassLoader.getPlatformClassLoader())){try{loader.loadClass("local.pixelmon.backport.AuthorLocatorCases").getMethod("run").invoke(null);}catch(java.lang.reflect.InvocationTargetException e){throw new AssertionError(e.getCause());}}
  ClassNode feature=Version13StaticVerification.node("local/pixelmon/backport/AuthorBossFeature");
  boolean shared=false;for(MethodNode m:feature.methods)if(m.name.equals("func_241855_a"))for(AbstractInsnNode n:m.instructions)if(n instanceof MethodInsnNode&&((MethodInsnNode)n).owner.equals("local/pixelmon/backport/AuthorBossPolicy")&&((MethodInsnNode)n).name.equals("candidate"))shared=true;
  if(!shared)throw new AssertionError("Feature must use the same chunk predicate as the compass");
  System.out.println("PASS shipped author locator: real/demolished rooms, binary NBT persistence, dimension gates, chunk rings/shared generation predicate, async generation, frozen-day timeout and cancellation ticket cleanup; world services isolated.");
 }
}
