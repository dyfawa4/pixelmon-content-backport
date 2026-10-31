package local.pixelmon.backport.client;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.pixelmonmod.pixelmon.client.gui.pokechecker.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.ITextComponent;
import com.pixelmonmod.pixelmon.client.gui.*;
/** Shared compact bevel matches the native gray checker tabs and popup rows. */
public final class EvolutionTabs {
    public static final class Tab extends Button {
        private final boolean selected;
        public Tab(int x,int y,int w,int h,ITextComponent label,IPressable action,boolean selected){super(x,y,w,h,label,action);this.selected=selected;}
        @Override public void func_230431_b_(MatrixStack m,int mx,int my,float tick){if(!field_230694_p_)return;int x=field_230690_l_,y=field_230691_m_,w=field_230688_j_,h=field_230689_k_;boolean hover=mx>=x&&mx<x+w&&my>=y&&my<y+h;int u=hover&&field_230693_o_?94:95,v=hover&&field_230693_o_?205:188;
            region(m,x,y,3,h,u,v,3,15);region(m,x+3,y,w-6,h,u+3,v,63,15);region(m,x+w-3,y,3,h,u+66,v,3,15);
            func_238472_a_(m,Minecraft.func_71410_x().field_71466_p,func_230458_i_(),x+w/2,y+(h-8)/2,field_230693_o_?0xffffff:0xaaaaaa);
        }
        private void region(MatrixStack m,int x,int y,int w,int h,int u,int v,int sw,int sh){ScreenHelper.drawImageQuad(Resources.summarySummary,m,x,y,w,h,u/256f,v/256f,(u+sw)/256f,(v+sh)/256f,1,1,1,1,0);}
    }
}
