package local.pixelmon.backport.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import local.pixelmon.backport.JavaRuntimePolicy;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;

/** No continue/back action: the JVM must be changed in the launcher and restarted. */
public final class JavaRuntimeScreen extends Screen {
    public JavaRuntimeScreen(){super(new TranslationTextComponent("pixelmon.backport.java11.title"));}
    @Override protected void func_231160_c_(){
        int width=Math.max(80,Math.min(220,field_230708_k_-24));
        func_230480_a_(new Button((field_230708_k_-width)/2,field_230709_l_-38,width,20,
            new TranslationTextComponent("menu.quit"),button->field_230706_i_.func_71400_g()));
    }
    public static net.minecraft.util.text.IFormattableTextComponent java11(){return new StringTextComponent("Java11").func_240699_a_(TextFormatting.YELLOW);}
    @Override public boolean func_231178_ax__(){return false;}
    @Override public void func_231175_as__(){}
    @Override public void func_230430_a_(MatrixStack matrix,int mouseX,int mouseY,float partial){
        func_238467_a_(matrix,0,0,field_230708_k_,field_230709_l_,0xff101010);
        int width=Math.max(1,Math.min(450,field_230708_k_-32));
        java.util.List<net.minecraft.util.IReorderingProcessor> lines=new java.util.ArrayList<>();
        for(String key:new String[]{"notice","bundled","current"}){
            Object[] args=key.equals("notice")?new Object[]{java11(),java11()}:key.equals("bundled")?new Object[]{java11()}:new Object[]{JavaRuntimePolicy.actual()};
            var text=new TranslationTextComponent("pixelmon.backport.java11."+key,args).func_240699_a_(TextFormatting.WHITE);
            for(var line:field_230712_o_.func_238425_b_(text,width)){
                lines.add(line);
            }
            lines.add(null);
        }
        float scale=Math.min(1f,Math.max(1,field_230709_l_-112)/(lines.size()*12f));
        String title=field_230704_d_.getString();
        float titleScale=Math.min(1.8f,width/(float)Math.max(1,field_230712_o_.func_78256_a(title)));
        float titleHeight=9*titleScale;
        float titleTop=Math.max(14,(field_230709_l_-52-titleHeight-22-lines.size()*12*scale)/2);
        matrix.func_227860_a_();matrix.func_227861_a_(field_230708_k_/2f,Math.max(10,titleTop-12),0);matrix.func_227862_a_(titleScale,titleScale,1);
        func_238471_a_(matrix,field_230712_o_,title,0,0,0xffff55);
        matrix.func_227865_b_();
        float top=titleTop+titleHeight+22;
        matrix.func_227860_a_();matrix.func_227861_a_((field_230708_k_-width*scale)/2,top,0);matrix.func_227862_a_(scale,scale,1);
        for(int i=0;i<lines.size();i++)if(lines.get(i)!=null)field_230712_o_.func_238422_b_(matrix,lines.get(i),0,i*12,0xffffff);
        matrix.func_227865_b_();
        super.func_230430_a_(matrix,mouseX,mouseY,partial);
    }
}
