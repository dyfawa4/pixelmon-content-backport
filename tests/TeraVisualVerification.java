import java.util.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.pixelmonmod.pixelmon.client.models.smd.ValveStudioModel;
import net.minecraft.util.ResourceLocation;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import local.pixelmon.backport.*;
import local.pixelmon.backport.client.TeraSurfaceVertices;
import local.pixelmon.backport.client.TeraBoneTransform;
import local.pixelmon.backport.client.TeraCrystalVertices;
import com.pixelmonmod.pixelmon.tools.Matrix4f;
import com.pixelmonmod.pixelmon.tools.Vector4f;

/** No game launch: real overlay vertex adapter and visual visibility policy. */
public final class TeraVisualVerification {
    static void require(boolean ok,String label){if(!ok)throw new AssertionError(label);}
    public static void main(String[] args)throws Exception{
        if(!(TeraVisualVerification.class.getClassLoader() instanceof NativeRenderLoader)){
            try{new NativeRenderLoader().loadClass("TeraVisualVerification").getMethod("main",String[].class).invoke(null,(Object)args);}
            catch(java.lang.reflect.InvocationTargetException failure){throw new AssertionError("Native visual regression failed",failure.getCause());}
            return;
        }
        ClassLoader loader=TeraVisualVerification.class.getClassLoader();
        JsonArray crowns;
        try(var source=loader.getResourceAsStream("backport/tera_visual_sources.json")){
            crowns=new JsonParser().parse(new java.io.InputStreamReader(source,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("crowns");
        }
        require(crowns.size()==19,"Complete attribute crown manifest");
        Class<?> unsafeType=Class.forName("sun.misc.Unsafe");var unsafeField=unsafeType.getDeclaredField("theUnsafe");unsafeField.setAccessible(true);
        Object unsafe=unsafeField.get(null);
        ValveStudioModel nativeLoader=(ValveStudioModel)unsafeType.getMethod("allocateInstance",Class.class).invoke(unsafe,ValveStudioModel.class);
        for(int i=0;i<19;i++){
            String type=crowns.get(i).getAsJsonObject().get("type").getAsString();require(type.equals(TeraItems.NAMES[i]),"Crown type order must match network Tera type IDs");
            for(String file:new String[]{"model.pqc","crown.bmd","texture.png"})require(loader.getResource("assets/pixelmon_backport/textures/tera/crowns/"+type+"/"+file)!=null,"Missing crown resource "+type+"/"+file);
            nativeLoader.resource=new ResourceLocation("pixelmon_backport","textures/tera/crowns/"+type+"/model.pqc");
            // Reproduce the real loader's hardcoded namespace on relative names,
            // then exercise its actual resolver on our packaged PQC body line.
            require(nativeLoader.getResource("crown.bmd").func_110624_b().equals("pixelmon"),"Native relative namespace bug evidence");
            String body;
            try(var source=loader.getResourceAsStream("assets/pixelmon_backport/textures/tera/crowns/"+type+"/model.pqc")){
                body=new java.io.BufferedReader(new java.io.InputStreamReader(source,java.nio.charset.StandardCharsets.UTF_8)).readLine().split(" ")[1];
            }
            ResourceLocation resolved=nativeLoader.getResource(body);
            require(resolved.func_110624_b().equals("pixelmon_backport") && loader.getResource("assets/"+resolved.func_110624_b()+"/"+resolved.func_110623_a())!=null,"Actual native crown resolver must find packaged mesh "+type);
        }
        for(int bits=0;bits<32;bits++){
            boolean active=(bits&1)!=0,sim=(bits&2)!=0,ended=(bits&4)!=0,field=(bits&8)!=0,alive=(bits&16)!=0;
            require(TeraVisualState.visible(active,sim,ended,field,alive)==(bits==25),"Only real active alive battlefield entities may render "+bits);
        }
        require(TeraVisualMath.transformationAge(100,-1,.5f)==-1 && TeraVisualMath.transformationAge(99,100,0)==-1,"Inactive/future transform boundaries");
        require(TeraVisualMath.transformationAge(100,100,.5f)==.5f && TeraVisualMath.transformationAge(179,100,.5f)==79.5f && TeraVisualMath.transformationAge(180,100,0)==-1,"Server-clock transform ends and cannot replay on late tracking");
        require(Math.abs(TeraVisualMath.surfaceZ(0,0,-1,-1,-5,1,-1,-5,0,1,-5)+5)<.0001f,"Crown seat ray hits central head skin");
        require(Float.isNaN(TeraVisualMath.surfaceZ(4,4,-1,-1,-5,1,-1,-5,0,1,-5)),"Off-center horns cannot raise the central crown seat");
        Random random=new Random(14520261017L);Capture capture=new Capture();
        for(int i=0;i<1000;i++){
            var bone=new Matrix4f();
            bone.m00=random.nextFloat();bone.m01=random.nextFloat();bone.m02=random.nextFloat();
            bone.m10=random.nextFloat();bone.m11=random.nextFloat();bone.m12=random.nextFloat();
            bone.m20=random.nextFloat();bone.m21=random.nextFloat();bone.m22=random.nextFloat();
            bone.m30=random.nextFloat()*30;bone.m31=random.nextFloat()*30;bone.m32=random.nextFloat()*30;
            var p=new Vector4f(random.nextFloat()*20,random.nextFloat()*20,random.nextFloat()*20,1);
            var skinned=Matrix4f.transform(bone,p,null);
            var m=TeraBoneTransform.toRender(bone);
            var rendered=new net.minecraft.util.math.vector.Vector4f(p.x,p.y,p.z,1);rendered.func_229372_a_(m);
            require(Math.abs(skinned.x-rendered.func_195910_a())<.0001f,"Crown X must match actual native skinning including translation");
            require(Math.abs(skinned.y-rendered.func_195913_b())<.0001f,"Crown Y must match actual native skinning");
            require(Math.abs(skinned.z-rendered.func_195914_c())<.0001f,"Crown Z must match actual native skinning");
        }
        for(int i=0;i<1000;i++){
            java.util.List<float[]> emitted=new ArrayList<>();
            Capture triangleCapture=new Capture(){@Override public void func_225588_a_(float x,float y,float z,float r,float g,float b,float a,float u,float v,int overlay,int light,float nx,float ny,float nz){emitted.add(new float[]{x,y,z,a,u,v,nx,ny,nz,overlay,light});}};
            TeraCrystalVertices crystal=new TeraCrystalVertices(triangleCapture,i);
            float[][] original=new float[3][11];
            for(int v=0;v<3;v++){
                float[] p=original[v];for(int j=0;j<9;j++)p[j]=random.nextFloat();p[9]=17;p[10]=23;
                crystal.func_225588_a_(p[0],p[1],p[2],.3f,.8f,.4f,.32f,p[4],p[5],17,23,p[6],p[7],p[8]);
            }
            require(emitted.size()==3,"One crystal triangle emits exactly one original triangle");
            for(int v=0;v<3;v++)for(int j=0;j<11;j++){
                if(j==3)require(emitted.get(v)[j]>=0 && emitted.get(v)[j]<=.32f,"Crystal does not cover the original skin");
                else require(emitted.get(v)[j]==original[v][j],"Crystal preserves animated positions, UVs, normals and metadata");
            }
        }
        for(int i=0;i<1000;i++){
            float x=random.nextFloat()*100-50,y=random.nextFloat()*100-50,z=random.nextFloat()*100-50;
            float nx=random.nextFloat(),ny=random.nextFloat(),nz=random.nextFloat(),u=random.nextFloat(),v=random.nextFloat();
            float ticks=random.nextFloat()*10000;
            new TeraSurfaceVertices(capture,ticks).func_225588_a_(x,y,z,.7f,.6f,.5f,.4f,u,v,17,23,nx,ny,nz);
            require(capture.x==x && capture.y==y && capture.z==z,"Overlay must never expand or displace the body");
            require(capture.nx==nx && capture.ny==ny && capture.nz==nz && capture.overlay==17 && capture.light==23,"Preserve normals and native vertex metadata");
            require(capture.a==.4f && capture.v==v && capture.u==u,"Preserve skin UVs so alpha cutouts cannot drift");
            new TeraSurfaceVertices(capture,-1).func_225588_a_(x,y,z,.7f,.6f,.5f,.4f,u,v,17,23,nx,ny,nz);
            require(capture.r==.7f && capture.g==.6f && capture.b==.5f && capture.u==u && capture.v==v,"Crowns keep original texture/color");
        }
        for(int type=0;type<19;type++)for(int tick=0;tick<200;tick++)for(float value:TeraVisualMath.color(type,tick))require(Float.isFinite(value)&&value>=0&&value<=1,"All attribute/Stellar colors valid");
        for(int type=0;type<19;type++)for(float value:TeraVisualMath.surfaceColor(type,50))require(Float.isFinite(value)&&value>=0&&value<=1,"Saturated material colors valid");
        float[] water=TeraVisualMath.surfaceColor(2,0),fire=TeraVisualMath.surfaceColor(1,0);
        require(water[2]-water[0]>.45f && fire[0]-fire[2]>.55f,"Water/fire material highlights retain attribute contrast");
        Capture flat=facet(0,water,new float[]{1,0,.98f,0,1,0});
        require(flat.a<.12f,"Ordinary non-reflecting skin retains at least 88 percent of its original color");
        for(float albedo:new float[]{.1f,.45f,.95f})for(float material:new float[]{flat.r,flat.g,flat.b})
            require(Math.abs(albedo*(1-flat.a)+material*flat.a-albedo)<.12f,"Clear coat cannot paint over normal or shiny albedo");
        Capture mirror=facet(0,water,new float[]{1,0,-.32f/.91f,0,1,.12f/.91f});
        Capture moved=facet(160,water,new float[]{1,0,-.32f/.91f,0,1,.12f/.91f});
        require(mirror.a>.60f && mirror.r>.70f,"Narrow reflections remain bright and clearly visible");
        require(Math.abs(mirror.a-moved.a)>.20f,"Specular reflection travels with the light over time");
        Capture edge=facet(0,water,new float[]{0,1,0,0,0,1});
        require(edge.a>.30f && edge.b-edge.r>.35f,"Water attribute is concentrated at grazing edges");
        float[] electric=TeraVisualMath.surfaceColor(4,0);
        Capture blueReflection=facet(0,water,new float[]{1,0,-.10f/.91f,0,1,.12f/.91f});
        Capture goldReflection=facet(0,electric,new float[]{1,0,-.10f/.91f,0,1,.12f/.91f});
        float reflectionSeparation=(Math.abs(blueReflection.r-goldReflection.r)+Math.abs(blueReflection.g-goldReflection.g)+Math.abs(blueReflection.b-goldReflection.b))/3*Math.min(blueReflection.a,goldReflection.a);
        require(reflectionSeparation>.15f,"Localized water/electric reflection must remain distinct after compositing over the same skin");
        for(float width:new float[]{0,.01f,1,7,30,1000})require(TeraVisualMath.crownScale(width)>=.35f && TeraVisualMath.crownScale(width)<=5,"Attachment scale bounded independently of entity");
        require(TeraVisualMath.specialCrown(1017)&&TeraVisualMath.specialCrown(1024)&&!TeraVisualMath.specialCrown(25),"Special forms retain their own crowns");
        require(TeraVisualMath.headScore("Head")>TeraVisualMath.headScore("Head1")&&TeraVisualMath.headScore("Bip001 Head")>0,"Skeleton head preference");
        for(int skinAlpha=0;skinAlpha<=255;skinAlpha++)for(int facetAlpha=0;facetAlpha<=255;facetAlpha++){
            int result=TeraVisualMath.facetPixel((skinAlpha<<24)|0x126789,(facetAlpha<<24)|0x987654);
            require((result>>>24)==skinAlpha*facetAlpha/255,"Facet mask preserves transparent skin boundaries");
        }
        System.out.println("TERA_VISUAL_PASS 1000 native skinning transforms, 1000 crystal triangles, 1000 unchanged body geometries, 19 colors, 32 lifecycle boundaries, original crown UVs");
    }
    static Capture facet(float ticks,float[] color,float[] edges){
        Capture capture=new Capture();TeraCrystalVertices vertices=new TeraCrystalVertices(capture,ticks);
        vertices.func_225588_a_(0,0,0,color[0],color[1],color[2],1,0,0,17,23,0,0,1);
        vertices.func_225588_a_(edges[0],edges[1],edges[2],color[0],color[1],color[2],1,1,0,17,23,0,0,1);
        vertices.func_225588_a_(edges[3],edges[4],edges[5],color[0],color[1],color[2],1,0,1,17,23,0,0,1);
        return capture;
    }
    /** Forge's Pixelmon AT exposes Matrix4f fields at runtime. Reproduce that
     * access change only, leaving all native matrix arithmetic unchanged. */
    public static final class NativeRenderLoader extends ClassLoader {
        public NativeRenderLoader(){super(TeraVisualVerification.class.getClassLoader());}
        @Override protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException{
            boolean local=name.equals("TeraVisualVerification") || name.startsWith("TeraVisualVerification$Capture")
                || name.equals("TeraVisualVerification$1") || name.equals("local.pixelmon.backport.client.TeraBoneTransform")
                || name.equals("com.pixelmonmod.pixelmon.tools.Matrix4f")
                || name.equals("net.minecraft.util.math.vector.Matrix4f") || name.equals("net.minecraft.util.math.vector.Vector4f");
            if(!local)return super.loadClass(name,resolve);
            synchronized(getClassLoadingLock(name)){
                Class<?> loaded=findLoadedClass(name);if(loaded!=null)return loaded;
                try(var stream=getParent().getResourceAsStream(name.replace('.','/')+".class")){
                    byte[] bytes=stream.readAllBytes();
                    if(name.equals("net.minecraft.util.math.vector.Matrix4f")){
                        var node=new org.objectweb.asm.tree.ClassNode();new org.objectweb.asm.ClassReader(bytes).accept(node,0);
                        for(var field:node.fields)field.access=(field.access&~(org.objectweb.asm.Opcodes.ACC_PRIVATE|org.objectweb.asm.Opcodes.ACC_PROTECTED))|org.objectweb.asm.Opcodes.ACC_PUBLIC;
                        var writer=new org.objectweb.asm.ClassWriter(0);node.accept(writer);bytes=writer.toByteArray();
                    }
                    loaded=defineClass(name,bytes,0,bytes.length);if(resolve)resolveClass(loaded);return loaded;
                }catch(java.io.IOException failure){throw new ClassNotFoundException(name,failure);}
            }
        }
    }
    static class Capture implements IVertexBuilder {
        float x,y,z,r,g,b,a,u,v,nx,ny,nz;int overlay,light;
        @Override public void func_225588_a_(float x,float y,float z,float r,float g,float b,float a,float u,float v,int overlay,int light,float nx,float ny,float nz){this.x=x;this.y=y;this.z=z;this.r=r;this.g=g;this.b=b;this.a=a;this.u=u;this.v=v;this.overlay=overlay;this.light=light;this.nx=nx;this.ny=ny;this.nz=nz;}
        public IVertexBuilder func_225582_a_(double x,double y,double z){throw new AssertionError();}
        public IVertexBuilder func_225586_a_(int r,int g,int b,int a){throw new AssertionError();}
        public IVertexBuilder func_225583_a_(float u,float v){throw new AssertionError();}
        public IVertexBuilder func_225585_a_(int a,int b){throw new AssertionError();}
        public IVertexBuilder func_225587_b_(int a,int b){throw new AssertionError();}
        public IVertexBuilder func_225584_a_(float x,float y,float z){throw new AssertionError();}
        public void func_181675_d(){throw new AssertionError();}
    }
}
