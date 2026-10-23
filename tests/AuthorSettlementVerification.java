import java.io.*;import java.net.*;import java.nio.file.*;import java.util.*;import java.util.jar.*;import javax.tools.ToolProvider;
import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import com.google.gson.*;
/** Execute shipped end/beat/delivery and native claim methods with isolated world, economy and UI services. */
public final class AuthorSettlementVerification {
 static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 public static void main(String[] args)throws Exception{
  Path dir=Files.createTempDirectory(Path.of(".cache/boss-170"),"settlement-");List<String> opts=new ArrayList<>(List.of("--release","11","-proc:none","-encoding","UTF-8","-cp",System.getProperty("java.class.path"),"-d",dir.toString()));
  for(String part:Files.readString(Path.of("tests/resources/AuthorSettlementCases.java.txt")).split("(?m)^// FILE: ")){if(part.isBlank())continue;int end=part.indexOf('\n');Path p=dir.resolve(part.substring(0,end).trim());Files.createDirectories(p.getParent());Files.writeString(p,part.substring(end+1));opts.add(p.toString());}
  check(ToolProvider.getSystemJavaCompiler().run(null,null,null,opts.toArray(new String[0]))==0,"Settlement fixture compilation");
  AuthorCombatFeedbackVerification.install(dir,"local.pixelmon.backport.AuthorBossArenas",AuthorCombatFeedbackVerification.bytes("local.pixelmon.backport.AuthorBossArenas"),Set.of("end","beat"));
  AuthorCombatFeedbackVerification.install(dir,"local.pixelmon.backport.AuthorBossRewards",AuthorCombatFeedbackVerification.bytes("local.pixelmon.backport.AuthorBossRewards"),Set.of("trainer","money","lambda$trainer$0"));
  try(JarFile jar=new JarFile(args[0])){
   String type="com.pixelmonmod.pixelmon.entities.pixelmon.drops.DropItemQueryList";
   AuthorCombatFeedbackVerification.install(dir,type,TooltipFeedbackVerification.read(jar,type),Set.of("takeItem","takeAllItems","removeQuery","dropAllItems"));
   ClassNode trainer=new ClassNode();new ClassReader(TooltipFeedbackVerification.read(jar,"com.pixelmonmod.pixelmon.entities.npcs.NPCTrainer")).accept(trainer,0);
   MethodNode lose=trainer.methods.stream().filter(m->m.name.equals("loseBattle")).findFirst().orElseThrow();int beat=-1,once=-1,index=0;
   for(AbstractInsnNode ins:lose.instructions){if(ins instanceof MethodInsnNode&&((MethodInsnNode)ins).owner.endsWith("BeatTrainerEvent"))beat=index;if(ins instanceof FieldInsnNode&&((FieldInsnNode)ins).owner.endsWith("EnumEncounterMode")&&((FieldInsnNode)ins).name.equals("Once"))once=index;index++;}
   check(beat>=0&&once>beat,"Native Once processing remains after BeatTrainerEvent");
  }
  List<URL> urls=new ArrayList<>();urls.add(dir.toUri().toURL());for(String p:System.getProperty("java.class.path").split(File.pathSeparator))urls.add(Path.of(p).toUri().toURL());
  try(var loader=new URLClassLoader(urls.toArray(new URL[0]),ClassLoader.getPlatformClassLoader())){try{loader.loadClass("AuthorSettlementCases").getMethod("run").invoke(null);}catch(java.lang.reflect.InvocationTargetException e){throw new AssertionError("Settlement sequence",e.getCause());}}
  JsonObject adv=JsonParser.parseString(Files.readString(Path.of("resources/data/pixelmon_backport/advancements/adventure/defeat_author.json"))).getAsJsonObject(),display=adv.getAsJsonObject("display");
  check(display.get("frame").getAsString().equals("challenge")&&display.getAsJsonObject("icon").get("item").getAsString().equals("minecraft:player_head"),"Challenge frame/head icon");
  String nbt=display.getAsJsonObject("icon").get("nbt").getAsString();UUID profile=local.pixelmon.backport.AuthorVictory.PROFILE;int[] ints={(int)(profile.getMostSignificantBits()>>32),(int)profile.getMostSignificantBits(),(int)(profile.getLeastSignificantBits()>>32),(int)profile.getLeastSignificantBits()};
  check(nbt.contains(Arrays.toString(ints).replace(" ","").replace("[","[I;")),"Offline skin UUID matches renderer profile");
  JsonObject zh=JsonParser.parseString(Files.readString(Path.of("resources/assets/pixelmon_backport/lang/zh_cn.json"))).getAsJsonObject();
  check(zh.get(display.getAsJsonObject("title").get("translate").getAsString()).getAsString().equals("这么强？！？！")&&zh.get(display.getAsJsonObject("description").get("translate").getAsString()).getAsString().equals("击败作者"),"Exact advancement wording");
  System.out.println("PASS shipped author end-before-beat ownership/HP preservation, one-shot economy/Boss query/advancement call, native single/all/drop claims, other-player retention and full-bag fallback. Native Once ordering and challenge/head assets checked; world/economy/UI services isolated.");
 }
}
