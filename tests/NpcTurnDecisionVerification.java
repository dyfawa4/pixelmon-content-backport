import java.io.*;import java.net.*;import java.nio.file.*;import java.util.*;import javax.tools.ToolProvider;
/** Exercise shipped per-turn deferral, current-action knowledge, finite medicines and revive planning. */
public final class NpcTurnDecisionVerification {
 public static void main(String[] args)throws Exception{
  Path dir=Files.createTempDirectory(Path.of(".cache/boss-170"),"npc-turn-");List<String> opts=new ArrayList<>(List.of("--release","11","-proc:none","-encoding","UTF-8","-cp",System.getProperty("java.class.path"),"-d",dir.toString()));
  for(String part:Files.readString(Path.of("tests/resources/NpcTurnCases.java.txt")).split("(?m)^// FILE: ")){if(part.isBlank())continue;int end=part.indexOf('\n');Path p=dir.resolve(part.substring(0,end).trim());Files.createDirectories(p.getParent());Files.writeString(p,part.substring(end+1));opts.add(p.toString());}
  for(String name:List.of("NpcTurnKnowledge","NpcRevivePolicy","NpcMedicine","NpcMedicineForecast","NpcArrivalPreview","NpcSwitchPolicy","NpcIntentResponse","NpcMoveIntent","BattleMedicines"))opts.add("src/local/pixelmon/backport/"+name+".java");
  if(ToolProvider.getSystemJavaCompiler().run(null,null,null,opts.toArray(new String[0]))!=0)throw new AssertionError("NPC turn fixture compile");
  TooltipFeedbackVerification.install(dir,"local.pixelmon.backport.GymChallenge",AuthorCombatFeedbackVerification.bytes("local.pixelmon.backport.GymChallenge"),Set.of("author","medicineDenied","lambda$author$0"));
  TooltipFeedbackVerification.install(dir,"com.pixelmonmod.pixelmon.battles.controller.participants.BattleParticipant",AuthorCombatFeedbackVerification.bytes("com.pixelmonmod.pixelmon.battles.controller.participants.BattleParticipant"),Set.of("getWait"));
  TooltipFeedbackVerification.install(dir,"local.pixelmon.backport.NinjaTraining",AuthorCombatFeedbackVerification.bytes("local.pixelmon.backport.NinjaTraining"),Set.of("bonus"));
  var tacticNode=new org.objectweb.asm.tree.ClassNode();new org.objectweb.asm.ClassReader(AuthorCombatFeedbackVerification.bytes("local.pixelmon.backport.NpcTactics")).accept(tacticNode,0);var dest=new org.objectweb.asm.tree.ClassNode();Path tacticFile=dir.resolve("local/pixelmon/backport/NpcTactics.class");new org.objectweb.asm.ClassReader(Files.readAllBytes(tacticFile)).accept(dest,0);for(var method:tacticNode.methods)if(method.name.equals("incoming")||method.name.equals("finishFirst")||method.name.equals("medicineDamage")||method.name.equals("knocksOut")||method.name.equals("threats")&&method.desc.contains(";Z)")){method.name=method.name.equals("incoming")?"forecastIncoming":method.name.equals("finishFirst")?"forecastFinish":method.name.equals("medicineDamage")?"forecastRisk":method.name.equals("knocksOut")?"knocksOut":"directThreats";dest.methods.removeIf(m->m.name.equals(method.name));dest.methods.add(method);}var writer=new org.objectweb.asm.ClassWriter(0);dest.accept(writer);Files.write(tacticFile,writer.toByteArray());
  TooltipFeedbackVerification.install(dir,"com.pixelmonmod.pixelmon.battles.controller.ai.BattleAIBase",AuthorCombatFeedbackVerification.bytes("com.pixelmonmod.pixelmon.battles.controller.ai.BattleAIBase"),Set.of("weightOffensiveMove"));
  // Use the actual native restorative effects, including the herb's distinct
  // implementation, rather than a test double which always heals to full.
  for(String name:List.of("com.pixelmonmod.pixelmon.items.IMedicine","com.pixelmonmod.pixelmon.items.IHealHP","com.pixelmonmod.pixelmon.items.HealFraction","com.pixelmonmod.pixelmon.items.medicine.MedicineRevive")){
   Path file=dir.resolve(name.replace('.','/')+".class");Files.createDirectories(file.getParent());Files.write(file,AuthorCombatFeedbackVerification.bytes(name));
  }
  TooltipFeedbackVerification.install(dir,"com.pixelmonmod.pixelmon.items.medicine.MedicineItem",AuthorCombatFeedbackVerification.bytes("com.pixelmonmod.pixelmon.items.medicine.MedicineItem"),Set.of("useMedicine"));
  TooltipFeedbackVerification.install(dir,"com.pixelmonmod.pixelmon.items.medicine.MedicinePotion",AuthorCombatFeedbackVerification.bytes("com.pixelmonmod.pixelmon.items.medicine.MedicinePotion"),Set.of("useMedicine"));
  nativeRevivalFactories(dir);
  List<URL> urls=new ArrayList<>();urls.add(dir.toUri().toURL());for(String p:System.getProperty("java.class.path").split(File.pathSeparator))urls.add(Path.of(p).toUri().toURL());
  try(var loader=new URLClassLoader(urls.toArray(new URL[0]),ClassLoader.getPlatformClassLoader())){try{loader.loadClass("NpcTurnCases").getMethod("run").invoke(null);}catch(java.lang.reflect.InvocationTargetException e){throw new AssertionError(e.getCause());}}
  System.out.println("PASS shipped player-submission deferral with native wait logic, doubles partial input, status/item/switch/attack and target scope, new-turn/end/simulation cleanup; leader/author-only player medicine ban, two Max Potions and one Max Revive, reserve-ace timing. Detached active/bench medicine HP/status/PP, real submitted-action restoration, incoming switch slot bridge, native damage-weighting entry versus empty shortlist and conservative item risk verified; battle/world services isolated.");
 }
 static void nativeRevivalFactories(Path dir)throws Exception{
  var registration=new org.objectweb.asm.tree.ClassNode();new org.objectweb.asm.ClassReader(AuthorCombatFeedbackVerification.bytes("com.pixelmonmod.pixelmon.init.registry.ItemRegistration")).accept(registration,0);
  var factories=new HashMap<String,String>();String id=null;
  for(var method:registration.methods)if(method.name.equals("<clinit>"))for(var op:method.instructions){
   if(op instanceof org.objectweb.asm.tree.LdcInsnNode&&((org.objectweb.asm.tree.LdcInsnNode)op).cst instanceof String)id=(String)((org.objectweb.asm.tree.LdcInsnNode)op).cst;
   if(op instanceof org.objectweb.asm.tree.InvokeDynamicInsnNode&&Set.of("revive","max_revive","revival_herb").contains(id))for(Object arg:((org.objectweb.asm.tree.InvokeDynamicInsnNode)op).bsmArgs)if(arg instanceof org.objectweb.asm.Handle){factories.put(id,((org.objectweb.asm.Handle)arg).getName());break;}
  }
  if(factories.size()!=3)throw new AssertionError("Native revival registry inventory changed: "+factories);
  long total=registration.methods.stream().filter(m->java.util.stream.StreamSupport.stream(m.instructions.spliterator(),false).anyMatch(op->op instanceof org.objectweb.asm.tree.TypeInsnNode&&((org.objectweb.asm.tree.TypeInsnNode)op).desc.equals("com/pixelmonmod/pixelmon/items/ReviveItem"))).count();
  if(total!=3)throw new AssertionError("Uncovered native revival factory: "+total);
  Path file=dir.resolve("com/pixelmonmod/pixelmon/api/registries/PixelmonItems.class");var items=new org.objectweb.asm.tree.ClassNode();new org.objectweb.asm.ClassReader(Files.readAllBytes(file)).accept(items,0);
  for(var entry:factories.entrySet()){
   var factory=registration.methods.stream().filter(m->m.name.equals(entry.getValue())).findFirst().orElseThrow();factory.name="create_"+entry.getKey();factory.access=org.objectweb.asm.Opcodes.ACC_PUBLIC|org.objectweb.asm.Opcodes.ACC_STATIC;
   items.methods.removeIf(m->m.name.equals(factory.name));items.methods.add(factory);
  }
  var writer=new org.objectweb.asm.ClassWriter(0);items.accept(writer);Files.write(file,writer.toByteArray());
 }
}
