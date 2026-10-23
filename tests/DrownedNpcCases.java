package qa;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.quests.*;
import com.pixelmonmod.pixelmon.entities.npcs.NPCTrainer;
import net.minecraft.entity.player.ServerPlayerEntity;
public final class DrownedNpcCases {
 static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
 public static void run(ServerPlayerEntity player)throws Exception{
  var quest=QuestRegistry.getInstance().getQuestCollection().stream().filter(q->q.getFilename().contains("enter_drowned_world")).findFirst().orElseThrow();
  check(quest.getStages().stream().filter(s->s.getStage()==0).findFirst().orElseThrow().getRawObjectives().contains("DATE 01/01 31/12"),"Loaded quest year-round");
  var data=StorageProxy.getParty(player).getQuestData(true);var savedProgress=new java.util.ArrayList<>(data.getProgress());data.getProgress().clear();var progress=new QuestProgress(quest,data);data.getProgress().add(progress);progress.start();
  data.receive("DATE");check(progress.getStage()==1,"Real date gate advances to NPC assignment");
  var world=player.func_71121_q();long time=world.func_72820_D();var npc=new com.pixelmonmod.pixelmon.entities.npcs.NPCQuestGiver(world);npc.func_70107_b(player.func_226277_ct_()+3,player.func_226278_cu_(),player.func_226281_cx_());boolean added=world.func_217376_c(npc);System.out.println("DROWNED_NPC_DIAG added="+added+" questgiver="+true+" position="+npc.func_233580_cy_()+" player="+player.func_233580_cy_());
  try{
   world.func_241114_a_(6000);data.receive("NPC_TIMED_INSERTER");check(progress.getDataString("NPC")==null,"Not assigned at daytime");
   world.func_241114_a_(15000);for(int i=0;i<200&&progress.getDataString("NPC")==null;i++)data.receive("NPC_TIMED_INSERTER");
   String id=progress.getDataString("NPC");check(id!=null&&world.func_217461_a(java.util.UUID.fromString(id)) instanceof com.pixelmonmod.pixelmon.entities.npcs.NPCQuestGiver,"Native quest assignment finds a real NPC id="+id+" data="+progress.getDataStringMap()+" stage="+progress.getStage()+" time="+world.func_72820_D()+" config="+com.pixelmonmod.pixelmon.api.config.PixelmonConfigProxy.getQuests().isQuestRandomNPCs()+" loadedNPC="+world.func_217461_a(npc.func_110124_au()));
   check(progress.getStage()==1,"NPC offer remains pending player response");
   System.out.println("DROWNED_NPC_PASS live year-round DATE -> stage1; daytime absent; native15000 night timed inserter assigns live NPC at original10percent; pending offer retained");
  }finally{data.getProgress().clear();data.getProgress().addAll(savedProgress);npc.func_70106_y();world.func_241114_a_(time);}
 }
}
