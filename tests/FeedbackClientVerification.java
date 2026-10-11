import java.util.*;
import java.nio.file.*;
import local.pixelmon.backport.*;
import local.pixelmon.backport.client.ItemHelpTooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.util.ScreenShotHelper;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import mezz.jei.api.recipe.*;
import com.pixelmonmod.pixelmon.items.TechnicalMoveItem;
import com.pixelmonmod.pixelmon.enums.TMType;
import mezz.jei.api.constants.VanillaRecipeCategoryUid;

/** QA-only client: opens an isolated distributed map and checks the real JEI runtime. */
@Mod("backport_feedback_client_test")
public final class FeedbackClientVerification {
    @Mod.EventBusSubscriber(modid="backport_feedback_client_test",value=Dist.CLIENT)
    public static class Events {
        static boolean loading,done;static int frames;
        static final String gym=System.getProperty("backport.gym.preview","");
        static void require(boolean b,String why){if(!b)throw new AssertionError(why);}
        @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
            if(e.phase!=TickEvent.Phase.END || done)return;Minecraft mc=Minecraft.func_71410_x();
            if(!loading && mc.field_71462_r!=null && mc.field_71462_r.getClass().getSimpleName().equals("MainMenuScreen")){
                loading=true;mc.func_238191_a_(gym.isEmpty()?"backport-qa-raids":"backport-gym-"+gym);return;
            }
            if(mc.field_71441_e==null || mc.field_71439_g==null || mezz.jei.Internal.getRuntime()==null)return;
            if(++frames<150)return;
            if(!gym.isEmpty()){
                var server=mc.func_71401_C();if(server==null)return;
                var world=server.func_71218_a(net.minecraft.world.World.field_234918_g_);
                if(!GymTestMaps.data(world).ready)return;
                if(frames<450)return;
                System.out.println("BACKPORT_GYM_CLIENT_PASS "+gym+" ready pieces="+GymTestMaps.data(world).cursor);done=true;frames=0;return;
            }
            try{
                var runtime=mezz.jei.Internal.getRuntime();var manager=runtime.getRecipeManager();
                var category=manager.getRecipeCategory(VanillaRecipeCategoryUid.INFORMATION,false);
                require(category!=null,"Missing JEI information category");int checked=0;
                for(var entry:Backport.REGISTERED.values()){
                    ItemStack stack=new ItemStack(entry.get());var focus=manager.createFocus(IFocus.Mode.OUTPUT,stack);
                    require(!manager.getRecipes(category,focus,false).isEmpty(),"No real JEI purpose/source page "+entry.getId());
                    List<ITextComponent> normal=new ArrayList<>(),shift=new ArrayList<>();
                    ItemHelpTooltip.append(stack,normal,false);ItemHelpTooltip.append(stack,shift,true);
                    require(normal.size()==1 && shift.size()>=2,"Tooltip compact/Shift branches "+entry.getId());++checked;
                }
                for(int number:new int[]{171,207,210,225,226,227,228,229}){
                    ItemStack stack=TechnicalMoveItem.of(TMType.TM9,BackportTechnicalMoves.resolve(TMType.TM9,number));
                    require(!manager.getRecipes(category,manager.createFocus(IFocus.Mode.OUTPUT,stack),false).isEmpty(),"Missing TM JEI info "+number);++checked;
                }
                var integrated=mc.func_71401_C();require(integrated!=null,"No integrated map server");
                var world=integrated.func_71218_a(net.minecraft.world.World.field_234918_g_);
                require(world.func_180495_p(new net.minecraft.util.math.BlockPos(0,64,0)).func_177230_c()!=net.minecraft.block.Blocks.field_150350_a,"Map facilities not generated");
                ItemStack disc=new ItemStack(Backport.REGISTERED.get("tera_blast_disc_single_use").get());
                runtime.getRecipesGui().show(manager.createFocus(IFocus.Mode.OUTPUT,disc));
                System.out.println("BACKPORT_FEEDBACK_CLIENT_PASS real JEI information pages="+checked+" compact/Shift details; distributed map helper loaded and platform generated");done=true;frames=0;
            }catch(Throwable failure){done=true;System.err.println("BACKPORT_FEEDBACK_CLIENT_FAIL");failure.printStackTrace();}
        }
        static boolean captured;
        @SubscribeEvent public static void render(TickEvent.RenderTickEvent e){
            if(!done || captured || e.phase!=TickEvent.Phase.END || ++frames<25)return;
            captured=true;Minecraft mc=Minecraft.func_71410_x();
            try{Path p=Paths.get(gym.isEmpty()?"../feedback-jei.png":"../gym-preview-"+gym+".png");try(NativeImage img=ScreenShotHelper.func_198052_a(mc.func_228018_at_().func_198105_m(),mc.func_228018_at_().func_198083_n(),mc.func_147110_a())){img.func_209270_a(p);}}
            catch(Exception ex){ex.printStackTrace();}
            mc.func_71400_g();
        }
    }
}
