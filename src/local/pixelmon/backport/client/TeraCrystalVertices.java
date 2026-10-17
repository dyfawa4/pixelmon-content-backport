package local.pixelmon.backport.client;

import com.mojang.blaze3d.vertex.IVertexBuilder;

/** Light facets from the exact animated triangle, never from stretched skin UVs.
 * Positions, UVs, skin cutouts and native vertex normals remain unchanged. */
public final class TeraCrystalVertices implements IVertexBuilder {
    private final IVertexBuilder target;
    private final float ticks;
    private final float[][] vertices=new float[3][12];
    private final int[][] metadata=new int[3][2];
    private int count;
    private int triangle;
    public TeraCrystalVertices(IVertexBuilder target,float ticks){this.target=target;this.ticks=ticks;}
    @Override public void func_225588_a_(float x,float y,float z,float r,float g,float b,float a,float u,float v,int overlay,int light,float nx,float ny,float nz){
        float[] q=vertices[count];
        q[0]=x;q[1]=y;q[2]=z;q[3]=r;q[4]=g;q[5]=b;q[6]=a;q[7]=u;q[8]=v;q[9]=nx;q[10]=ny;q[11]=nz;
        metadata[count][0]=overlay;metadata[count][1]=light;
        if(++count!=3)return;
        float[] p=vertices[0],s=vertices[1],t=vertices[2];
        float ax=s[0]-p[0],ay=s[1]-p[1],az=s[2]-p[2],bx=t[0]-p[0],by=t[1]-p[1],bz=t[2]-p[2];
        float fx=ay*bz-az*by,fy=az*bx-ax*bz,fz=ax*by-ay*bx;
        float length=(float)Math.sqrt(fx*fx+fy*fy+fz*fz);
        double seed=Math.sin(++triangle*12.9898)*43758.5453;
        float facet=(float)(seed-Math.floor(seed));
        float spec=0,reflection=0,rim=0;
        if(length>1.e-8f){
            // A moving reflected light makes highlights travel over the real
            // animated facets. Away from reflections the skin stays visible.
            float hx=.32f+.18f*(float)Math.sin(ticks*.022),hy=-.24f+.12f*(float)Math.cos(ticks*.019),hz=.91f;
            float dot=Math.abs((fx*hx+fy*hy+fz*hz)/length)/(float)Math.sqrt(hx*hx+hy*hy+hz*hz);
            spec=(float)Math.pow(Math.min(1,dot),40);
            reflection=(float)Math.pow(Math.min(1,dot),18);
            rim=(float)Math.pow(1-Math.min(1,Math.abs(fz)/length),5);
        }
        float pulse=(float)Math.pow(Math.max(0,Math.sin(facet*6.2831+ticks*.085)),16);
        float glint=pulse*(.10f+.90f*spec);
        // Attribute tint is concentrated at grazing edges; white/attribute
        // reflections are localized. No opaque random facet recoloring.
        // Colored reflection surrounds a much narrower white glint. Keeping
        // both lobes white erased the differences between all 19 attributes.
        float tint=.010f+.50f*rim+.78f*reflection+.08f*glint;
        float white=.010f+3.20f*spec*spec*spec+.04f*glint;
        float coverage=Math.min(.86f,tint+white);
        float whiteShare=white/(tint+white);
        for(int i=0;i<3;i++){
            q=vertices[i];
            // White highlights retain a small attribute tint of their own.
            float neutral=whiteShare*.90f;
            target.func_225588_a_(q[0],q[1],q[2],q[3]*(1-neutral)+neutral,q[4]*(1-neutral)+neutral,q[5]*(1-neutral)+neutral,q[6]*coverage,q[7],q[8],metadata[i][0],metadata[i][1],q[9],q[10],q[11]);
        }
        count=0;
    }
    public IVertexBuilder func_225582_a_(double x,double y,double z){throw new UnsupportedOperationException("SMD triangle adapter");}
    public IVertexBuilder func_225586_a_(int r,int g,int b,int a){throw new UnsupportedOperationException("SMD triangle adapter");}
    public IVertexBuilder func_225583_a_(float u,float v){throw new UnsupportedOperationException("SMD triangle adapter");}
    public IVertexBuilder func_225585_a_(int a,int b){throw new UnsupportedOperationException("SMD triangle adapter");}
    public IVertexBuilder func_225587_b_(int a,int b){throw new UnsupportedOperationException("SMD triangle adapter");}
    public IVertexBuilder func_225584_a_(float x,float y,float z){throw new UnsupportedOperationException("SMD triangle adapter");}
    public void func_181675_d(){throw new UnsupportedOperationException("SMD triangle adapter");}
}
