package qa;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.util.text.StringTextComponent;
/** Actual baked item rendering, not a texture contact sheet. */
public final class Update180ArtScreen extends Screen {
    public Update180ArtScreen(){super(new StringTextComponent("1.8.0 original assets"));}
    @Override public boolean func_231177_au__(){return false;}
    @Override public void func_230430_a_(MatrixStack matrix,int mouseX,int mouseY,float partial){
        var mc=Minecraft.func_71410_x();AbstractGui.func_238467_a_(matrix,5,5,field_230708_k_-5,field_230709_l_-5,0xee182331);
        mc.field_71466_p.func_238421_b_(matrix,"Pixelmon 9.4.2 original assets / 1.16 native item models",14,12,0xffffff);
        int columns=8,cell=field_230708_k_/columns;int rows=(Update180VisualCases.ITEMS.length+columns-1)/columns;int rowHeight=(field_230709_l_-40)/rows;float scale=Math.min(2f,(rowHeight-10)/16f);
        for(int i=0;i<Update180VisualCases.ITEMS.length;i++){
            var stack=Update180Cases.stack(Update180VisualCases.ITEMS[i]);var baked=mc.func_175599_af().func_184393_a(stack,mc.field_71441_e,mc.field_71439_g);
            if(baked.func_177554_e().func_195668_m().func_110623_a().contains("missingno"))throw new AssertionError("Missing native model: "+Update180VisualCases.ITEMS[i]);
            int x=14+i%columns*cell,y=30+i/columns*rowHeight;
            matrix.func_227860_a_();matrix.func_227861_a_(x,y,0);matrix.func_227862_a_(scale,scale,scale);com.mojang.blaze3d.systems.RenderSystem.pushMatrix();com.mojang.blaze3d.systems.RenderSystem.multMatrix(matrix.func_227866_c_().func_227870_a_());
            mc.func_175599_af().func_180450_b(stack,0,0);com.mojang.blaze3d.systems.RenderSystem.popMatrix();matrix.func_227865_b_();
            mc.field_71466_p.func_238421_b_(matrix,Update180VisualCases.ITEMS[i].split(":")[1],x,y+(int)(16*scale)+1,0xd0e4ff);
        }
        if(java.nio.file.Files.exists(java.nio.file.Path.of("repair-only")))func_230457_a_(matrix,Update180Cases.stack("pixelmon:charizardite_x"),field_230708_k_-220,field_230709_l_-66);
    }
}
