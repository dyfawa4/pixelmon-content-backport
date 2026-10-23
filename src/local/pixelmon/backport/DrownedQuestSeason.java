package local.pixelmon.backport;
import java.util.Map;
import com.google.gson.*;
import net.minecraft.util.ResourceLocation;
/** Only the entrance quest's real-world calendar gate becomes year-round. */
public final class DrownedQuestSeason {
 private DrownedQuestSeason(){}
 public static void apply(Map<ResourceLocation,JsonElement> quests){for(var entry:quests.entrySet())if(entry.getKey().func_110624_b().equals("pixelmon")&&(entry.getKey().func_110623_a().equals("enter_drowned_world")||entry.getKey().func_110623_a().equals("special/enter_drowned_world"))&&entry.getValue().isJsonObject()){
  var quest=entry.getValue().getAsJsonObject();if(!quest.has("stages"))continue;
  for(var stageValue:quest.getAsJsonArray("stages")){var stage=stageValue.getAsJsonObject();if(stage.get("stage").getAsInt()!=0||!stage.has("objectives"))continue;var objectives=stage.getAsJsonArray("objectives");for(int i=0;i<objectives.size();i++)if(objectives.get(i).isJsonPrimitive()&&objectives.get(i).getAsString().startsWith("DATE "))objectives.set(i,new JsonPrimitive("DATE 01/01 31/12"));}
 }}
}
