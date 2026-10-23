import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import javax.tools.ToolProvider;
/** Exercise the production bake handler with isolated rendering boundaries. */
public final class Update170Verification {
 public static void main(String[] args)throws Exception{
  Path dir=Files.createTempDirectory(Path.of(".cache/fixes-170"),"art-");
  List<String> options=new ArrayList<>(List.of("--release","11","-proc:none","-encoding","UTF-8","-cp",System.getProperty("java.class.path"),"-d",dir.toString()));
  for(String part:Files.readString(Path.of("tests/resources/BallArt170Cases.java.txt")).split("(?m)^// FILE: ")){
   if(part.isBlank())continue;int end=part.indexOf('\n');Path p=dir.resolve(part.substring(0,end).trim());Files.createDirectories(p.getParent());Files.writeString(p,part.substring(end+1));options.add(p.toString());
  }
  for(String source:List.of("BallItemArt","client/BallItemArtModels","client/TitleMessages"))options.add("src/local/pixelmon/backport/"+source+".java");
  if(ToolProvider.getSystemJavaCompiler().run(null,null,null,options.toArray(new String[0]))!=0)throw new AssertionError("Art fixture compile");
  List<URL> urls=new ArrayList<>();urls.add(dir.toUri().toURL());for(String p:System.getProperty("java.class.path").split(File.pathSeparator))urls.add(Path.of(p).toUri().toURL());
  try(URLClassLoader loader=new URLClassLoader(urls.toArray(new URL[0]),ClassLoader.getPlatformClassLoader())){loader.loadClass("BallArt170Cases").getMethod("run").invoke(null);}
 }
}
