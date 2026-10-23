package local.pixelmon.backport;
import java.util.*;
import net.minecraft.command.CommandSource;
import net.minecraft.command.impl.LocateCommand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** /locate shares the compass's bounded asynchronous room search, rather than searching empty aliases. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid=Content.NS)
public final class AuthorLocateCommands {
    private static final Map<CommandSource,Job> JOBS=new LinkedHashMap<>();
    private static final class Job implements AutoCloseable {
        final ServerWorld world;final BlockPos origin;final long started;final AuthorBossLocator.Search search;
        Job(CommandSource source,AuthorBossLocator.RoomAlias alias){world=source.func_197023_e();origin=new BlockPos(source.func_197036_d());started=world.func_82737_E();search=new AuthorBossLocator.Search(world,origin,alias);}
        public void close(){search.close();}
    }
    public static int begin(CommandSource source,AuthorBossLocator.RoomAlias alias){
        if(!AuthorBossLocator.available(source.func_197023_e(),alias)){source.func_197021_a(new TranslationTextComponent("pixelmon.backport.author.locate.missing"));return 0;}
        for(var it=JOBS.entrySet().iterator();it.hasNext();){var e=it.next();if(e.getKey()==source||source.func_197022_f()!=null&&source.func_197022_f()==e.getKey().func_197022_f()){e.getValue().close();it.remove();}}
        JOBS.put(source,new Job(source,alias));source.func_197030_a(new TranslationTextComponent("pixelmon.backport.author.locate.searching"),false);return 1;
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        for(var it=JOBS.entrySet().iterator();it.hasNext();){var e=it.next();CommandSource source=e.getKey();Job job=e.getValue();
            if(source.func_197022_f()!=null&&(source.func_197022_f().field_70128_L||source.func_197022_f().field_70170_p!=job.world)){job.close();it.remove();continue;}
            try{
                BlockPos found=job.search.step();
                if(found!=null){LocateCommand.func_241054_a_(source,"作者道馆",job.origin,found,"commands.locate.success");job.close();it.remove();}
                else if(job.search.finished()||job.world.func_82737_E()-job.started>=1200){source.func_197021_a(new TranslationTextComponent("pixelmon.backport.author.locate.missing"));job.close();it.remove();}
            }catch(RuntimeException failure){job.close();it.remove();source.func_197021_a(new TranslationTextComponent("pixelmon.backport.author.locate.missing"));}
        }
    }
    @SubscribeEvent public static void unload(WorldEvent.Unload event){for(var it=JOBS.values().iterator();it.hasNext();){Job job=it.next();if(job.world==event.getWorld()){job.close();it.remove();}}}
    @SubscribeEvent public static void stopped(net.minecraftforge.fml.event.server.FMLServerStoppedEvent event){JOBS.values().forEach(Job::close);JOBS.clear();}
}
