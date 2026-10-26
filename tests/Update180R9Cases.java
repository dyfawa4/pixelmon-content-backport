package qa;
import java.util.*;
import local.pixelmon.backport.*;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.model.ItemCameraTransforms.TransformType;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.*;
public final class Update180R9Cases {
    static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);}
    public static void setup(ServerPlayerEntity p){p.func_71033_a(net.minecraft.world.GameType.CREATIVE);p.field_71071_by.func_174888_l();p.field_71071_by.func_70299_a(0,new ItemStack(Backport.item("shrinking_lamp")));p.field_71071_by.func_70299_a(1,new ItemStack(Backport.item("enlarging_lamp")));p.field_71071_by.field_70461_c=0;p.func_71121_q().func_180501_a(new net.minecraft.util.math.BlockPos(38,73,46),net.minecraft.block.Blocks.field_150348_b.func_176223_P(),2);p.func_200619_a(p.func_71121_q(),38,74,46,180,8);}
    public static void client(int age){var mc=Minecraft.func_71410_x();
        if(age==35){for(String name:List.of("shrinking_lamp","enlarging_lamp")){var model=mc.func_175599_af().func_184393_a(new ItemStack(Backport.item(name)),mc.field_71441_e,mc.field_71439_g);Set<String> gui=null;
            for(var perspective:List.of(TransformType.GUI,TransformType.FIRST_PERSON_RIGHT_HAND,TransformType.FIRST_PERSON_LEFT_HAND,TransformType.THIRD_PERSON_RIGHT_HAND,TransformType.THIRD_PERSON_LEFT_HAND,TransformType.GROUND,TransformType.FIXED)){
                var baked=model.handlePerspective(perspective,new MatrixStack());var quads=baked.func_200117_a(null,null,new Random(9));Set<String> textures=new HashSet<>();for(var q:quads)textures.add(q.func_187508_a().func_195668_m().toString());check(!textures.isEmpty()&&textures.stream().noneMatch(s->s.contains("missingno")),"No missing flashlight texture "+name+perspective);
                if(perspective==TransformType.GUI){gui=textures;check(textures.contains(Content.NS+":item/"+name),"GUI uses flashlight sprite");}
                else{String color=name.equals("shrinking_lamp")?"red":"green";check(textures.contains("minecraft:block/"+color+"_concrete")&&textures.contains("minecraft:block/sea_lantern")&&textures.contains("minecraft:block/iron_block"),"Actual 3D flashlight with correct body/lens/bezel "+perspective);check(quads.size()>=100,"True 3D geometry "+perspective);}
                System.out.println("R9_PERSPECTIVE "+name+" "+perspective+" quads="+quads.size()+" textures="+textures);
            }}System.out.println("R9_MODELS_PASS both native GUI sprites, first/third person left/right, ground and item frame 3D geometry");}
        if(age==55)shot("qa-r9-red-held.png");if(age==65)mc.field_71439_g.field_71071_by.field_70461_c=1;if(age==90)shot("qa-r9-green-held.png");if(age==100)mc.func_147108_a(new Preview());if(age==115)shot("qa-r9-flashlight-icons.png");if(age>125)mc.func_71400_g();
    }
    static void shot(String name){var mc=Minecraft.func_71410_x();net.minecraft.util.ScreenShotHelper.func_148259_a(mc.field_71412_D,name,mc.func_228018_at_().func_198107_o(),mc.func_228018_at_().func_198087_p(),mc.func_147110_a(),message->{});}
    static final class Preview extends Screen {
        Preview(){super(new StringTextComponent("Flashlight models"));}
        @Override public boolean func_231177_au__(){return false;}
        @Override public void func_230430_a_(MatrixStack m,int x,int y,float tick){func_238467_a_(m,0,0,field_230708_k_,field_230709_l_,0xff1f2936);var mc=Minecraft.func_71410_x();int i=0;for(String name:List.of("shrinking_lamp","enlarging_lamp")){int px=field_230708_k_/4+i*field_230708_k_/2,py=field_230709_l_/2-45;m.func_227860_a_();m.func_227861_a_(px-48,py,0);m.func_227862_a_(6,6,6);com.mojang.blaze3d.systems.RenderSystem.pushMatrix();com.mojang.blaze3d.systems.RenderSystem.multMatrix(m.func_227866_c_().func_227870_a_());mc.func_175599_af().func_180450_b(new ItemStack(Backport.item(name)),0,0);com.mojang.blaze3d.systems.RenderSystem.popMatrix();m.func_227865_b_();func_238471_a_(m,mc.field_71466_p,new TranslationTextComponent("item.pixelmon_backport."+name).getString(),px,py+110,0xffffff);i++;}}
    }
}
