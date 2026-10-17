package local.pixelmon.backport.client;
import com.mojang.blaze3d.vertex.IVertexBuilder;

/** Keep the native SMD renderer off its BufferBuilder vertex-id reuse path:
 * cached vertices belong to the original textured pass, not this overlay. */
public final class TeraSurfaceVertices implements IVertexBuilder {
    private final IVertexBuilder target;
    private final float phase;
    public TeraSurfaceVertices(IVertexBuilder target,float ticks){this.target=target;this.phase=ticks;}
    @Override public void func_225588_a_(float x,float y,float z,float r,float g,float b,float a,float u,float v,int overlay,int light,float nx,float ny,float nz){
        if(phase<0){target.func_225588_a_(x,y,z,r,g,b,a,u,v,overlay,light,nx,ny,nz);return;}
        float shine=.78f+.22f*(float)Math.sin(phase*.09+x*1.7+y*.9+z*1.3);
        target.func_225588_a_(x,y,z,r*shine,g*shine,b*shine,a,u,v,overlay,light,nx,ny,nz);
    }
    @Override public IVertexBuilder func_225582_a_(double x,double y,double z){target.func_225582_a_(x,y,z);return this;}
    @Override public IVertexBuilder func_225586_a_(int r,int g,int b,int a){target.func_225586_a_(r,g,b,a);return this;}
    @Override public IVertexBuilder func_225583_a_(float u,float v){target.func_225583_a_(u,v);return this;}
    @Override public IVertexBuilder func_225585_a_(int a,int b){target.func_225585_a_(a,b);return this;}
    @Override public IVertexBuilder func_225587_b_(int a,int b){target.func_225587_b_(a,b);return this;}
    @Override public IVertexBuilder func_225584_a_(float x,float y,float z){target.func_225584_a_(x,y,z);return this;}
    @Override public void func_181675_d(){target.func_181675_d();}
}
