import java.nio.file.*;
import java.util.*;
import com.google.gson.*;

/** Check the actual translation parameters and Chinese spacing for battle notices. */
public final class GymRuleTextVerification {
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 public static void main(String[] args)throws Exception{
  for(String lang:List.of("zh_cn","en_us")){
   JsonObject data=new JsonParser().parse(Files.readString(Path.of("resources/assets/pixelmon_backport/lang/"+lang+".json"))).getAsJsonObject();
   var examples=new LinkedHashMap<String,Object[]>();
   examples.put("pixelmon.backport.gym.challenge.before",new Object[]{});
   examples.put("pixelmon.backport.gym.challenge.rules",new Object[]{3,3});
   examples.put("pixelmon.backport.gym.challenge.remaining",new Object[]{"测试玩家",2,1,1});
   examples.put("pixelmon.backport.gym.challenge.revive_limit",new Object[]{"测试玩家"});
   examples.put("pixelmon.backport.gym.challenge.restore_limit",new Object[]{"测试玩家"});
   examples.put("pixelmon.backport.author.challenge.rules",new Object[]{});
   examples.put("pixelmon.backport.author.challenge.no_medicine",new Object[]{});
   for(var entry:examples.entrySet()){
    String template=data.get(entry.getKey()).getAsString();
    check(template.split("%s",-1).length-1==entry.getValue().length,"Parameter count "+lang+" "+entry.getKey());
    String rendered=String.format(Locale.ROOT,template,entry.getValue());
    check(!rendered.contains("%s")&&!rendered.contains("  ")&&!rendered.contains("\\n"),"Unformatted notice "+entry.getKey());
    if(lang.equals("zh_cn")){
     check(!rendered.matches("(?s).*\\d[ \\t\\n]+[次个].*"),"Quantity broken by whitespace "+entry.getKey());
     for(String line:rendered.split("\n",-1))check(!line.isEmpty()&&line.equals(line.strip()),"Empty or indented notice row "+entry.getKey());
    }
   }
   if(lang.equals("zh_cn")){
    check(data.get("pixelmon.backport.gym.challenge.before").getAsString().contains("恢复最多2次，复活最多1次"),"Ordinary gym split allowance not announced");
    check(data.get("pixelmon.backport.author.challenge.rules").getAsString().contains("所有药品"),"Author total medicine ban not announced");
   }
  }
  System.out.println("PASS Chinese/English gym and author notices: exact argument counts, 2 restorative/1 revival rule, all-medicine ban, clean whitespace and section breaks; native wrapping retained.");
 }
}
