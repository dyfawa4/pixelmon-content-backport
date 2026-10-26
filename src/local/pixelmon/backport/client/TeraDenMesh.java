package local.pixelmon.backport.client;
import com.google.gson.*;
import com.mojang.blaze3d.matrix.MatrixStack;
import local.pixelmon.backport.Content;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ResourceLocation;
/** Original9.4 crystal cluster/cave mesh and texture, with1.16 translucent rendering. */
public final class TeraDenMesh {
    private static final JsonObject MODEL=Content.read("backport/tera_den_mesh.json");
    private static final ResourceLocation TEXTURE=new ResourceLocation(Content.NS,"textures/tera/den/tera_raid_den.png");
    private TeraDenMesh(){}
    public static void render(MatrixStack matrix,IRenderTypeBuffer buffer,int light,int color){
        var vertices=buffer.getBuffer(RenderType.func_228644_e_(TEXTURE));
        var pose=matrix.func_227866_c_();
        for(var row:MODEL.getAsJsonArray("faces")){
            var face=row.getAsJsonArray();boolean gem=face.get(0).getAsInt()==0;var points=face.get(1).getAsJsonArray();
            float r=gem?((color>>16)&255)/255f:.22f,g=gem?((color>>8)&255)/255f:.22f,b=gem?(color&255)/255f:.25f;
            // The native entity format uses quads; duplicate the last vertex of each triangle.
            for(int index:new int[]{0,1,2,2}){
                var v=points.get(index).getAsJsonArray();
                vertices.func_227888_a_(pose.func_227870_a_(),v.get(0).getAsFloat(),v.get(1).getAsFloat(),v.get(2).getAsFloat())
                    .func_227885_a_(r,g,b,gem?.94f:1f).func_225583_a_(v.get(6).getAsFloat(),v.get(7).getAsFloat())
                    .func_225585_a_(OverlayTexture.field_229196_a_&65535,OverlayTexture.field_229196_a_>>>16).func_225587_b_((gem?15728880:light)&65535,(gem?15728880:light)>>>16)
                    .func_227887_a_(pose.func_227872_b_(),v.get(3).getAsFloat(),v.get(4).getAsFloat(),v.get(5).getAsFloat()).func_181675_d();
            }
        }
    }
}
