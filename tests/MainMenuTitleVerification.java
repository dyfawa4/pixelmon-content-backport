import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import local.pixelmon.backport.client.TitleMessages;

/** Targeted native-bytecode boundary checks; does not launch a game or OpenGL. */
public final class MainMenuTitleVerification {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        ClassNode nativeMenu=new ClassNode();
        new ClassReader("net.minecraft.client.gui.screen.MainMenuScreen").accept(nativeMenu,0);
        MethodNode render=nativeMenu.methods.stream().filter(m->m.name.equals("func_230430_a_")).findFirst().orElseThrow();
        int logo=0,edition=0;
        for(AbstractInsnNode instruction:render.instructions.toArray()){
            if(!(instruction instanceof MethodInsnNode))continue;
            MethodInsnNode call=(MethodInsnNode)instruction;
            if(!call.owner.equals("net/minecraft/client/gui/screen/MainMenuScreen"))continue;
            if(call.name.equals("func_238459_a_")&&call.desc.equals("(IILjava/util/function/BiConsumer;)V"))logo++;
            if(call.name.equals("func_238463_a_")&&call.desc.equals("(Lcom/mojang/blaze3d/matrix/MatrixStack;IIFFIIII)V"))edition++;
        }
        check(logo==2,"Both native title branches must be covered");
        check(edition==1,"Exactly one native edition draw is removed");
        ClassNode production=new ClassNode();
        new ClassReader("local.pixelmon.backport.mixin.JourneyMainMenuMixin").accept(production,0);
        MethodNode title=production.methods.stream().filter(m->m.name.equals("backport$title")).findFirst().orElseThrow();
        check(title.desc.equals("(Lnet/minecraft/client/gui/screen/MainMenuScreen;IILjava/util/function/BiConsumer;Lcom/mojang/blaze3d/matrix/MatrixStack;IIF)V"),"Redirect captures native arguments and outer matrix");
        for(int i=0;i<100;i++)check(TitleMessages.next().equals("b站端忆制作"),"Only author credit splash");
        var file=Path.of("resources/assets/pixelmon_backport/textures/gui/journey_title.png");
        var image=ImageIO.read(file.toFile());
        check(image.getWidth()==3113&&image.getHeight()==577,"Supplied full-resolution title retained");
        check(image.getColorModel().hasAlpha()&&(image.getRGB(0,0)>>>24)==0,"Transparent title background");
        for(int[] size:List.of(new int[]{320,240},new int[]{480,270},new int[]{854,480})){
            int width=Math.min(320,Math.max(1,size[0]-24)),height=Math.max(1,Math.round(width*577f/3113f));
            check(width<=size[0]-24&&24+height<size[1]/4+48,"Title fits menu buttons and width");
        }
        System.out.println("TITLE_PASS native draw targets2+1; matrix signature; fixed author credit; transparent supplied image; common GUI layouts. No game launched.");
    }
}
