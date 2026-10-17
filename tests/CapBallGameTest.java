import java.nio.file.*;
import com.google.gson.*;
import com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.*;
import com.pixelmonmod.pixelmon.entities.pokeballs.*;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import com.pixelmonmod.pixelmon.items.PokeBallPart;
import local.pixelmon.backport.*;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.util.math.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

/** Real native projectile impact in an opt-in throwaway full-client instance. */
@Mod.EventBusSubscriber(modid="backport_tera_visual_test")
public final class CapBallGameTest {
    static int step,nativeBalls;static boolean finished;
    static final String[] BALLS={"master_ball","origin_ball","park_ball","master_ball","origin_ball","park_ball","poke_ball","great_ball","ultra_ball","poke_ball","great_ball","ultra_ball","master_ball","origin_ball","park_ball","master_ball","origin_ball","park_ball","master_ball","origin_ball","park_ball"};
    static final Path RESULT=Path.of("cap-ball-game-result.json");
    static boolean enabled(){return "backport-tera-visuals-145".equals(System.getProperty("backport.manual.world"))&&Files.isRegularFile(Path.of("cap-ball-private-test"));}
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||finished||!enabled())return;
        var server=ServerLifecycleHooks.getCurrentServer();if(server==null||server.func_184103_al().func_181057_v().isEmpty())return;
        var p=server.func_184103_al().func_181057_v().get(0);if(p.field_70173_aa<200||p.field_70173_aa%20!=0)return;
        try{
            if(step==BALLS.length){
                Files.writeString(RESULT,"{\"serverPassed\":true,\"actualImpacts\":21,\"blockedAndRefundedOnce\":6,\"blockedWithoutVirtualRefund\":6,\"legalCaptureStarts\":9,\"nativeBallTypesChecked\":"+nativeBalls+"}");
                System.out.println("CAP_BALL_GAME_SERVER_PASS impacts=21 blocked=12 allowed=9");finished=true;return;
            }
            boolean shiny=(step>=3&&step<6)||(step>=9&&step<12)||step>=18;int dex=step>=12&&step<15?25:1026;
            PixelmonEntity target=new PixelmonEntity(p.func_71121_q(),TeraVisualTestMap.pokemon(dex,"",shiny));target.func_70012_b(20,65,35,0,0);target.func_94061_f(true);p.func_71121_q().func_217376_c(target);
            if(step==0){for(var b:PokeBallRegistry.getAll()){boolean expect=BurstCapture.forbidden(b,target.getPokemon());check(CapBallGuard.fieldBlocked(b,target)==expect,"Native ball selection "+b.getName());nativeBalls++;}}
            var ball=PokeBallRegistry.getPokeBall(BALLS[step]).getValue().orElseThrow();
            var area=new AxisAlignedBB(18,60,32,24,75,40);for(var old:p.func_71121_q().func_175647_a(ItemEntity.class,area,it->true))old.func_70106_y();
            var projectile=new EmptyPokeBallEntity(p.func_71121_q(),p,ball,step<15);projectile.func_70107_b(20,65.5,35);p.func_71121_q().func_217376_c(projectile);
            var impact=EmptyPokeBallEntity.class.getDeclaredMethod("func_70227_a",RayTraceResult.class);impact.setAccessible(true);impact.invoke(projectile,new EntityRayTraceResult(target));
            var drops=p.func_71121_q().func_175647_a(ItemEntity.class,area,it->it.func_70089_S());
            boolean forbidden=step<6||step>=15;
            if(forbidden){
                check(!projectile.func_70089_S()&&target.hitByPokeball==null&&target.func_70089_S(),"Forbidden ball entered animation "+BALLS[step]);
                if(step<6)check(drops.size()==1&&drops.get(0).func_92059_d().func_190916_E()==1&&PokeBallPartType(drops.get(0),ball),"Missing/duplicate/wrong refund "+BALLS[step]+" drops="+drops.size());
                else check(drops.isEmpty(),"Virtual projectile created a ball refund");
            }else check(projectile.func_70089_S()&&target.hitByPokeball==projectile&&drops.isEmpty(),"Legal ball disturbed "+dex+":"+BALLS[step]);
            for(var item:drops)item.func_70106_y();target.hitByPokeball=null;projectile.func_70106_y();target.func_70106_y();
            System.out.println("CAP_BALL_GAME_CASE_PASS "+dex+" "+BALLS[step]+" shiny="+shiny+" rejected="+forbidden);step++;
        }catch(Throwable error){finished=true;error.printStackTrace();try{Files.writeString(RESULT,"{\"serverPassed\":false}");}catch(Exception ignored){}System.out.println("CAP_BALL_GAME_FAIL");}
    }
    static boolean PokeBallPartType(ItemEntity item,PokeBall ball){return PokeBallPart.getPokeBall(item.func_92059_d()).map(b->b.is(ball)).orElse(false);}
    @Mod.EventBusSubscriber(modid="backport_tera_visual_test",value=net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static class Client {
        static int messages,closing;
        @SubscribeEvent public static void chat(net.minecraftforge.client.event.ClientChatReceivedEvent e){if(enabled()&&e.getMessage().getString().contains("不能用大师球、起源球、公园球捕捉小智版皮卡丘"))messages++;}
        @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
            if(e.phase!=TickEvent.Phase.END||!enabled()||!Files.isRegularFile(RESULT)||++closing!=60)return;
            try{var report=new JsonParser().parse(Files.readString(RESULT)).getAsJsonObject();report.addProperty("clientRejectionMessages",messages);report.addProperty("passed",report.get("serverPassed").getAsBoolean()&&messages==12);Files.writeString(RESULT,new GsonBuilder().setPrettyPrinting().create().toJson(report));System.out.println("CAP_BALL_GAME_FINAL_PASS="+report.get("passed"));}catch(Exception error){error.printStackTrace();}
            net.minecraft.client.Minecraft.func_71410_x().func_71400_g();
        }
    }
}
