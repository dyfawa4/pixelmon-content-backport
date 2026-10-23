package local.pixelmon.backport.mixin;
import java.util.*;
import com.google.common.collect.ImmutableMap;
import net.minecraft.client.resources.ClientLanguageMap;
import net.minecraft.client.resources.Language;
import net.minecraft.resources.IResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
/** Apply the requested native-item descriptions after the installed legacy language pack. */
@Mixin(ClientLanguageMap.class)
public abstract class ItemDescriptionLanguageMixin {
 @Redirect(method="func_239497_a_",at=@At(value="INVOKE",target="Lcom/google/common/collect/ImmutableMap;copyOf(Ljava/util/Map;)Lcom/google/common/collect/ImmutableMap;"),require=1)
 private static ImmutableMap<String,String> backport$descriptions(Map<String,String> values,IResourceManager resources,List<Language> languages){
  if(!languages.isEmpty()){
   String code=languages.get(languages.size()-1).getCode();
   if(code.equals("zh_cn")||code.equals("en_us"))for(var row:local.pixelmon.backport.Content.read("backport/description_fixes_"+code+".json").entrySet())values.put(row.getKey(),row.getValue().getAsString());
   String training=local.pixelmon.backport.GreninjaEnhancement.trainingDescription(code);if(training!=null)values.put("attack.ninja_training.description",training);
  }
  return ImmutableMap.copyOf(values);
 }
}
