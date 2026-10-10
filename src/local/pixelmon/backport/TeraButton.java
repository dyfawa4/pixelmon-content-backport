package local.pixelmon.backport;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.pixelmonmod.pixelmon.client.gui.ScreenHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

/** Compact crystal badge, anchored separately from the native gimmick badge. */
public final class TeraButton extends Button {
    private static final ResourceLocation ORB=new ResourceLocation("pixelmon_backport","textures/item/tera_orb.png");
    private static final int[] COLORS={0xaaa9a1,0xff9852,0x58b8ed,0x7cca68,0xf2cf60,0x91deed,0xe78367,0xc48cdb,0xcaa576,0x9baee8,0xed8dac,0xa5b862,0xc6b983,0x9c91c3,0x9691ee,0xa49c99,0x88b9c4,0xecb2db,0x83e4e5};
    private int state,type;
    public TeraButton(int x,int y,ITextComponent title,IPressable click){super(x,y,100,20,title,click);}
    public void configure(int state,int type){this.state=state;this.type=Math.max(0,Math.min(COLORS.length-1,type));}
    private static void pill(MatrixStack m,int x,int y,int w,int h,int color){
        AbstractGui.func_238467_a_(m,x+3,y,x+w-3,y+h,color);
        AbstractGui.func_238467_a_(m,x+1,y+1,x+w-1,y+h-1,color);
        AbstractGui.func_238467_a_(m,x,y+3,x+w,y+h-3,color);
    }
    @Override public void func_230431_b_(MatrixStack matrix,int mx,int my,float partial){
        boolean hover=mx>=field_230690_l_ && mx<field_230690_l_+field_230688_j_ && my>=field_230691_m_ && my<field_230691_m_+field_230689_k_;
        int x=field_230690_l_,y=field_230691_m_,w=field_230688_j_,h=field_230689_k_;
        int accent=state==0?0x6c777e:COLORS[type];
        pill(matrix,x,y+1,w,h,0xa0000000);
        pill(matrix,x,y,w,h,0xff000000|accent);
        pill(matrix,x+1,y+1,w-2,h-2,hover && field_230693_o_?0xff233944:state>=2?0xff1c3943:0xff17252e);
        AbstractGui.func_238467_a_(matrix,x+7,y+2,x+w-7,y+3,0x55000000|accent);
        ScreenHelper.drawImageQuad(ORB,matrix,x+4,y+2,16,16,0,0,1,1,state==0?.5f:1,state==0?.5f:1,state==0?.5f:1,1,0);
        String label=func_230458_i_().getString();
        Minecraft mc=Minecraft.func_71410_x();
        AbstractGui.func_238471_a_(matrix,mc.field_71466_p,label,x+57,y+6,state==0?0x929ea5:0xebffff);
        if(state>=2)AbstractGui.func_238467_a_(matrix,x+6,y+h-3,x+w-6,y+h-2,0xff000000|accent);
    }
}
