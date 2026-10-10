import java.nio.file.*;
import java.util.*;
import net.minecraft.nbt.*;

/** Offline preparation of separate copies of the private flat test world. */
public final class PrepareTestMaps {
    public static void main(String[] args)throws Exception{
        Path source=Paths.get(args[0]).toRealPath(),saves=Paths.get(args[1]).toAbsolutePath().normalize();
        String[] ids={"shop","forms","single","double","raids","eternatus","interactions"};
        String[] titles={"01 道具与背包测试","02 换形与技能回忆测试","03 太晶 Mega Z 单打测试","04 双打与目标选择测试","05 太晶巢穴与掉落测试","06 无极汰那专属测试","07 新宝可梦原生交互测试"};
        for(int i=0;i<ids.length;i++){
            Path target=saves.resolve("backport-qa-"+ids[i]);
            if(Files.exists(target))continue;
            Files.createDirectories(target);
            try(java.util.stream.Stream<Path> files=Files.walk(source)){
                for(Path file:(Iterable<Path>)files::iterator){
                    Path relative=source.relativize(file);
                    if(relative.toString().equals("session.lock") || relative.toString().startsWith("playerdata") || relative.toString().startsWith("stats") || relative.toString().startsWith("advancements"))continue;
                    Path copy=target.resolve(relative);
                    if(Files.isDirectory(file))Files.createDirectories(copy);else Files.copy(file,copy);
                }
            }
            Path level=target.resolve("level.dat");CompoundNBT root=CompressedStreamTools.func_244263_a(level.toFile());CompoundNBT data=root.func_74775_l("Data");
            data.func_74778_a("LevelName","回迁测试 "+titles[i]);data.func_74757_a("allowCommands",true);data.func_74768_a("GameType",0);data.func_74774_a("Difficulty",(byte)1);
            data.func_82580_o("Player");data.func_74768_a("SpawnX",0);data.func_74768_a("SpawnY",65);data.func_74768_a("SpawnZ",7);
            data.func_74772_a("Time",6000);data.func_74772_a("DayTime",6000);data.func_74757_a("raining",false);data.func_74757_a("thundering",false);
            CompressedStreamTools.func_244264_a(root,level.toFile());
            System.out.println("PREPARED "+target.getFileName());
        }
    }
}
