package qa;
import java.util.*;
import java.nio.file.*;
import local.pixelmon.backport.*;
import net.minecraft.entity.player.ServerPlayerEntity;
import com.pixelmonmod.pixelmon.spawning.PixelmonSpawning;
public final class R8BossTimeCases {
 static int checks;
 static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
 static String last(net.minecraft.world.World world){var rows=BossSpawnCheckItem.lines(world);return rows.get(rows.size()-1);}
 public static void run(ServerPlayerEntity player)throws Exception{
  var world=player.func_71121_q();var boss=PixelmonSpawning.megaBossSpawner;var legendary=PixelmonSpawning.legendarySpawner;
  long oldBoss=boss.nextSpawnTime,oldLegend=legendary.nextSpawnTime;var report=new com.google.gson.JsonObject();
  try{
   legendary.nextSpawnTime=System.currentTimeMillis()+6000000;
   boss.nextSpawnTime=System.currentTimeMillis()+64500;String first=last(world);check(first.contains("1分")&&first.contains("秒"),"Live Boss timer uses minutes and seconds, not legendary timer");
   report.addProperty("future",first);long unchanged=boss.nextSpawnTime;BossSpawnCheckItem.components(world);check(boss.nextSpawnTime==unchanged&&legendary.nextSpawnTime!=unchanged,"Read-only query preserves both schedules");
   boss.nextSpawnTime=System.currentTimeMillis()+500;check(last(world).contains("0分1秒"),"Subsecond future remains one second instead of zero");
   boss.nextSpawnTime=System.currentTimeMillis()-1000;String due=last(world);check(!due.contains("-1")&&due.contains("即将尝试"),"Past deadline never displays a negative countdown");report.addProperty("due",due);
   boss.nextSpawnTime=0;String pending=last(world);check(pending.contains("等待"),"Uninitialized timer has a pending state");report.addProperty("pending",pending);
   PixelmonSpawning.megaBossSpawner=null;String absent=last(world);check(absent.contains("暂不可用"),"Missing Boss spawner does not crash");report.addProperty("absent",absent);PixelmonSpawning.megaBossSpawner=boss;
   var rows=BossSpawnCheckItem.lines(world);String base=rows.stream().filter(v->v.startsWith("独立Boss检查")).findFirst().orElseThrow();check(base.contains("分")&&base.contains("秒")&&base.contains("刻"),"Configured base interval shows both human-readable time and ticks");report.addProperty("base",base);
   for(var tier:com.pixelmonmod.pixelmon.api.pokemon.boss.BossTierRegistry.getBossTiers())if(tier.canRandomlySpawn()&&tier.getWeight()>0){var text=BossSpawnCheckItem.components(world).stream().filter(v->v.getString().contains("["+tier.getID()+"]")).findFirst().orElseThrow();check(text.func_150256_b().func_240711_a_().func_240742_a_()==(tier.getColor().getRGB()&0xffffff),"Boss tier colour retained "+tier.getID());}
  }finally{PixelmonSpawning.megaBossSpawner=boss;boss.nextSpawnTime=oldBoss;legendary.nextSpawnTime=oldLegend;}
  report.addProperty("checks",checks);Files.writeString(Path.of("r8-timing.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(report));BossBalance190Probe.log("R8_TIMING_PASS "+checks);
 }
}
