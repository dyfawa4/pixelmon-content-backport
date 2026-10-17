import java.nio.file.*;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.api.storage.*;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

/** Opt-in private full-client integration test; never packaged in the production mod. */
@Mod.EventBusSubscriber(modid="backport_tera_visual_test")
public final class LateBloomerGameTest {
    static final String[] IDS={"dragonite","tyranitar","salamence","metagross","garchomp","hydreigon","goodra","kommo_o","dragapult","hisuian_goodra","baxcalibur"};
    static final int[] DEX={149,248,373,376,445,635,706,784,887,706,998};
    static int step;static boolean finished;
    static boolean enabled(){return "backport-tera-visuals-145".equals(System.getProperty("backport.manual.world"))&&Files.isRegularFile(Path.of("late-bloomers-private-test"));}
    static boolean done(ServerPlayerEntity p,String id){
        var a=p.func_184102_h().func_191949_aK().func_192778_a(new ResourceLocation("pixelmon_backport","late_bloomers/"+id));
        if(a==null)throw new AssertionError("Missing loaded advancement "+id);
        return p.func_192039_O().func_192747_a(a).func_192105_a();
    }
    static void revoke(ServerPlayerEntity p,String id){TeraVisualTestMap.cmd(p,"advancement revoke @s only pixelmon_backport:late_bloomers/"+id);}
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||finished||!enabled())return;
        var server=ServerLifecycleHooks.getCurrentServer();if(server==null||server.func_184103_al().func_181057_v().isEmpty())return;
        var p=server.func_184103_al().func_181057_v().get(0);if(p.field_70173_aa<200||p.field_70173_aa%20!=0)return;
        try{
            int row=step/2;
            if(row<IDS.length){
                if(step%2==0){
                    revoke(p,"root");for(String id:IDS)revoke(p,id);
                    check(!done(p,"root"),"Revoke failed");
                    var mon=PokemonFactory.create(PixelmonSpecies.fromNationalDex(DEX[row]));if(row==9)mon.setForm("hisuian");
                    mon.setShiny(row%2!=0);StorageProxy.getPCForPlayer(p).getBox(0).set(new StoragePosition(0,29),mon);
                }else{
                    check(done(p,"root")&&done(p,IDS[row]),"First acquisition failed "+IDS[row]);
                    for(String id:IDS)check(done(p,id)==id.equals(IDS[row]),"Cross award "+IDS[row]+" -> "+id);
                    System.out.println("LATE_BLOOMER_GAME_CASE_PASS "+IDS[row]+" first-obtained opening + distinct species");
                }
                step++;return;
            }
            Files.writeString(Path.of("late-bloomers-game-result.json"),"{\"passed\":true,\"firstAcquisitionCases\":11,\"canonicalStorage\":\"native PCBox slot 29\",\"unrelatedAwards\":0}");
            System.out.println("LATE_BLOOMER_GAME_PASS cases=11");finished=true;
        }catch(Throwable error){
            finished=true;error.printStackTrace();try{Files.writeString(Path.of("late-bloomers-game-result.json"),"{\"passed\":false}");}catch(Exception ignored){}
            System.out.println("LATE_BLOOMER_GAME_FAIL");
        }
    }
    @Mod.EventBusSubscriber(modid="backport_tera_visual_test",value=net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static final class Client {
        static int closing;
        @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){if(e.phase==TickEvent.Phase.END&&enabled()&&Files.isRegularFile(Path.of("late-bloomers-game-result.json"))&&++closing==40)net.minecraft.client.Minecraft.func_71410_x().func_71400_g();}
    }
}
