package local.pixelmon.backport.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.text.StringTextComponent;

/** Pixel widths, rather than spaces, keep CJK dialogue inside the native 240x80 panel. */
public final class NpcDialogueLayout {
    private NpcDialogueLayout(){}
    public static final class Layout {
        public final List<IReorderingProcessor> lines;
        public final float scale;
        public final int rowHeight;
        private final int fontHeight;
        Layout(List<IReorderingProcessor> lines,float scale,int rowHeight,int fontHeight){this.lines=lines;this.scale=scale;this.rowHeight=rowHeight;this.fontHeight=fontHeight;}
        public float height(){return (Math.max(0,lines.size()-1)*rowHeight+fontHeight)*scale;}
    }
    public static Layout layout(FontRenderer font,String text,int width){
        int rowHeight=font.field_78288_b+3;
        float scale=1;
        List<IReorderingProcessor> lines=font.func_238425_b_(new StringTextComponent(text==null?"":text),Math.max(1,width));
        while((Math.max(0,lines.size()-1)*rowHeight+font.field_78288_b)*scale>46){
            scale*=0.85f;
            lines=font.func_238425_b_(new StringTextComponent(text==null?"":text),Math.max(1,(int)(width/scale)));
        }
        return new Layout(lines,scale,rowHeight,font.field_78288_b);
    }
    public static void draw(MatrixStack matrix,String text,float centerX,float centerY,int width,int color,boolean shadow){
        FontRenderer font=Minecraft.func_71410_x().field_71466_p;
        Layout layout=layout(font,text,width);
        float y=centerY-(layout.lines.size()-1)*layout.rowHeight*layout.scale/2;
        matrix.func_227860_a_();
        try{
            matrix.func_227861_a_(centerX,y,0);
            matrix.func_227862_a_(layout.scale,layout.scale,1);
            for(int i=0;i<layout.lines.size();i++){
                IReorderingProcessor line=layout.lines.get(i);
                float x=-font.func_243245_a(line)/2f;
                if(shadow)font.func_238407_a_(matrix,line,x,i*layout.rowHeight,color);
                else font.func_238422_b_(matrix,line,x,i*layout.rowHeight,color);
            }
        }finally{matrix.func_227865_b_();}
    }
}
