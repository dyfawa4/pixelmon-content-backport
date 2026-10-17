package local.pixelmon.backport.client;

import java.util.*;
import com.google.gson.*;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.pixelmonmod.pixelmon.client.models.*;
import com.pixelmonmod.pixelmon.client.models.smd.*;
import com.pixelmonmod.pixelmon.client.render.PixelmonRendering;
import com.pixelmonmod.pixelmon.entities.pixelmon.*;
import com.pixelmonmod.pixelmon.tools.Vector4f;
import local.pixelmon.backport.*;
import local.pixelmon.backport.mixin.TeraVertexAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.particles.*;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Additive visual layers only: entity dimensions and body transforms are untouched. */
@Mod.EventBusSubscriber(modid=Content.NS,value=Dist.CLIENT)
public final class TeraVisualRenderer {
    private static final String[] NAMES=TeraItems.NAMES;
    private static final Map<Integer,ValveStudioModel> CROWNS=new HashMap<>();
    private static final Map<ValveStudioModel,Attachment> ANCHORS=new WeakHashMap<>();
    private static final Set<Integer> FAILED=new HashSet<>();
    private static final JsonArray CROWN_DATA=Content.read("backport/tera_visual_sources.json").getAsJsonArray("crowns");
    private static final JsonObject ATTACHMENTS=Content.read("backport/tera_attachments.json").getAsJsonObject("models");
    private static Context context;
    private static final Map<ResourceLocation,RenderType> SURFACES=new LinkedHashMap<>(16,.75f,true);
    private TeraVisualRenderer(){}
    private static RenderType surface(Context c){
        ResourceLocation texture=TeraFacetTextures.get(c.entity.getTexture());
        return SURFACES.computeIfAbsent(texture,key->{
            RenderType base=PixelmonRendering.getSMDTransparentRenderType(key);
            // Native "transparent" is additive glow (SRC_ALPHA, ONE). Use a
            // normal alpha decal, with the same depth rule as armor glint.
            // EQUAL shades only the visible original surface, without biasing
            // back faces through the skin or displacing any body vertex.
            return new RenderType("backport_tera_surface",base.func_228663_p_(),4,256,true,false,()->{
                base.func_228547_a_();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.depthFunc(514);RenderSystem.depthMask(false);
            },()->{RenderSystem.depthMask(true);RenderSystem.depthFunc(515);base.func_228549_b_();}){};
        });
    }

    private static int type(AbstractClientEntity entity){
        return entity instanceof PixelmonEntity && entity instanceof TeraVisualState?((TeraVisualState)entity).backport$teraVisualType():-1;
    }
    public static boolean captureHead(Bone bone){
        return context!=null && context.type>=0 && !context.auxiliary && TeraVisualMath.headScore(bone.name)>0;
    }
    public static void begin(AbstractClientEntity entity,IRenderTypeBuffer buffer,float partial,boolean pokedex,int pass){
        int type=type(entity);
        boolean show=!pokedex && pass==1 && type>=0 && type<19 && !entity.func_82150_aj() && !((PixelmonEntity)entity).stopRender;
        if(!show && context==null)return;
        context=new Context(context,entity,buffer,partial,show?type:-1);
    }
    public static void model(ValveStudioModel model,MatrixStack matrix,int light,int overlay,float alpha){
        Context c=context;
        if(c==null || c.type<0 || c.auxiliary || model.body==null || !c.seen.add(model))return;
        // Record transforms, then draw after the native pass has finished. Getting
        // another buffer here would flush its still-in-use shared BufferBuilder.
        MatrixStack copy=new MatrixStack();
        copy.func_227866_c_().func_227870_a_().func_226595_a_(matrix.func_227866_c_().func_227870_a_());
        copy.func_227866_c_().func_227872_b_().func_226118_b_(matrix.func_227866_c_().func_227872_b_());
        Attachment a=ANCHORS.computeIfAbsent(model,TeraVisualRenderer::attachment);
        if(a.bone<0){Attachment animated=attachment(model,true);a=new Attachment(-1,animated.x,animated.y,animated.z,a.scale);}
        Bone head=a.bone<0?null:model.body.bones.get(a.bone);
        net.minecraft.util.math.vector.Matrix4f pose=head instanceof TeraBonePose?((TeraBonePose)head).backport$teraHeadPose():null;
        c.draws.add(new Draw(model,copy,light,overlay,alpha,a,pose));
    }
    private static void drawModel(Context c,Draw draw){
        ValveStudioModel model=draw.model;MatrixStack matrix=draw.matrix;int light=draw.light,overlay=draw.overlay;float alpha=draw.alpha;
        float[] color=TeraVisualMath.surfaceColor(c.type,c.ticks);
        // Directly rerender the already animated mesh. Do not call setupForRender,
        // animate, setScale, or alter any native cached per-vertex state.
        model.body.render(matrix,new TeraCrystalVertices(c.buffer.getBuffer(surface(c)),c.ticks),0xf000f0,overlay,c.partial,color[0],color[1],color[2],Math.min(1,alpha));
        c.body=true;
        if(!c.crown && c.entity.getModel() instanceof PixelmonModelSmd && ((PixelmonModelSmd<?>)c.entity.getModel()).theModel==model){
            c.crown=true;
            if(!TeraVisualMath.specialCrown(c.entity.getSpecies().getDex())){
                Attachment a=draw.attachment;
                matrix.func_227860_a_();
                try{
                    if(draw.bonePose!=null){
                        matrix.func_227866_c_().func_227870_a_().func_226595_a_(draw.bonePose);
                        matrix.func_227866_c_().func_227872_b_().func_226118_b_(new net.minecraft.util.math.vector.Matrix3f(draw.bonePose));
                    }
                    matrix.func_227861_a_(a.x,a.y,a.z);drawCrown(c,matrix,a.scale,light,overlay);
                }finally{matrix.func_227865_b_();}
            }
        }
    }
    public static void finish(MatrixStack matrix,int light){
        Context c=context;if(c==null)return;
        try{
            if(c.type<0)return;
            for(Draw draw:c.draws)drawModel(c,draw);
            if(c.body)return;
            // Non-SMD models still receive a complete body overlay; every buffer
            // requested by a compound model is routed into the same surface pass.
            PixelmonModelBase model=(PixelmonModelBase)c.entity.getModel();
            if(model==null)return;
            IRenderTypeBuffer previous=model.buffer;
            float[] color=TeraVisualMath.surfaceColor(c.type,c.ticks);c.auxiliary=true;
            try{
                RenderType surface=surface(c);
                model.setBuffer(renderType->new TeraSurfaceVertices(c.buffer.getBuffer(surface),c.ticks));
                model.render(c.entity,matrix,new TeraSurfaceVertices(c.buffer.getBuffer(surface),c.ticks),0xf000f0,net.minecraft.client.renderer.texture.OverlayTexture.field_229196_a_,color[0],color[1],color[2],.16f);
            }finally{model.setBuffer(previous);c.auxiliary=false;}
            if(!TeraVisualMath.specialCrown(c.entity.getSpecies().getDex())){
                float height=(float)c.entity.getPokemon().getForm().getDimensions().getHeight();
                matrix.func_227860_a_();
                try{
                    matrix.func_227861_a_(0,1.501-height,0);
                    matrix.func_227863_a_(Vector3f.field_229179_b_.func_229187_a_(-90));
                    matrix.func_227862_a_(.0625f,.0625f,.0625f);
                    drawCrown(c,matrix,TeraVisualMath.crownScale((float)c.entity.getPokemon().getForm().getDimensions().getWidth()*16),light,net.minecraft.client.renderer.texture.OverlayTexture.field_229196_a_);
                }finally{matrix.func_227865_b_();}
            }
        }finally{context=c.parent;}
    }
    private static void drawCrown(Context c,MatrixStack matrix,float scale,int light,int overlay){
        float age=TeraTransformation.age(c.entity,c.partial);
        if(age>=0 && age<44)return;
        if(FAILED.contains(c.type))return;
        ValveStudioModel crown=CROWNS.get(c.type);
        if(crown==null){
            try{
                crown=new ValveStudioModel(new ResourceLocation(Content.NS,"textures/tera/crowns/"+NAMES[c.type]+"/model.pqc"));
                if(crown.body==null)throw new IllegalStateException("Missing crown geometry");
                CROWNS.put(c.type,crown);
            }catch(Exception e){FAILED.add(c.type);com.pixelmonmod.pixelmon.Pixelmon.LOGGER.error("Cannot load backported Tera crown "+NAMES[c.type],e);return;}
        }
        c.auxiliary=true;matrix.func_227860_a_();
        try{
            // Crown meshes have different exported base heights. Seat their
            // lowest vertex on the attachment plane without changing geometry.
            matrix.func_227861_a_(0,0,CROWN_DATA.get(c.type).getAsJsonObject().get("bottom").getAsFloat()*scale);
            matrix.func_227862_a_(scale,scale,scale);
            ResourceLocation texture=new ResourceLocation(Content.NS,"textures/tera/crowns/"+NAMES[c.type]+"/texture.png");
            // A non-BufferBuilder adapter avoids native vertex-id caches crossing
            // entities and preserves the original crown texture coordinates.
            float alpha=age>=44 && age<56?(age-44)/12:1;
            crown.func_225598_a_(matrix,new TeraSurfaceVertices(c.buffer.getBuffer(PixelmonRendering.getSMDRenderType(texture)),-1),0xf000f0,overlay,1,1,1,alpha);
        }finally{matrix.func_227865_b_();c.auxiliary=false;}
    }
    private static Attachment attachment(ValveStudioModel model){
        return attachment(model,false);
    }
    private static Vector4f position(DeformVertex vertex,boolean animated){
        return animated?new Vector4f(vertex.x,vertex.y,vertex.z,1):((TeraVertexAccess)vertex).backport$restPosition();
    }
    private static Attachment attachment(ValveStudioModel model,boolean animated){
        if(!animated && ATTACHMENTS.has(model.resource.toString())){
            JsonObject profile=ATTACHMENTS.getAsJsonObject(model.resource.toString());
            Bone bone=model.body.getBoneByName(profile.get("bone").getAsString());
            if(bone!=null){JsonArray p=profile.getAsJsonArray("position");return new Attachment(bone.ID,p.get(0).getAsFloat(),p.get(1).getAsFloat(),p.get(2).getAsFloat(),profile.get("scale").getAsFloat());}
        }
        Bone chosen=null;int score=0;
        if(!animated)for(Bone bone:model.body.bones){int candidate=TeraVisualMath.headScore(bone.name);if(candidate>score){chosen=bone;score=candidate;}}
        Collection<DeformVertex> vertices=chosen==null?model.body.verts:chosen.verts.keySet();
        if(vertices.isEmpty() && chosen!=null){Set<DeformVertex> children=new HashSet<>();headVertices(chosen,children);vertices=children;}
        if(vertices.isEmpty()){vertices=model.body.verts;chosen=null;}
        float minX=Float.POSITIVE_INFINITY,minY=minX,minZ=minX,maxX=-minX,maxY=-minX,maxZ=-minX;
        for(DeformVertex vertex:vertices){Vector4f p=position(vertex,animated);
            minX=Math.min(minX,p.x);minY=Math.min(minY,p.y);minZ=Math.min(minZ,p.z);maxX=Math.max(maxX,p.x);maxY=Math.max(maxY,p.y);maxZ=Math.max(maxZ,p.z);}
        if(!Float.isFinite(minX))return new Attachment(-1,0,0,0,1);
        float x=(minX+maxX)/2,y=(minY+maxY)/2,seat=Float.POSITIVE_INFINITY;
        Set<DeformVertex> headSet=new HashSet<>(vertices);
        for(NormalizedFace face:model.body.faces){
            if(!headSet.contains(face.vertices[0]) || !headSet.contains(face.vertices[1]) || !headSet.contains(face.vertices[2]))continue;
            Vector4f a=position(face.vertices[0],animated),b=position(face.vertices[1],animated),d=position(face.vertices[2],animated);
            float z=TeraVisualMath.surfaceZ(x,y,a.x,a.y,a.z,b.x,b.y,b.z,d.x,d.y,d.z);
            if(Float.isFinite(z))seat=Math.min(seat,z);
        }
        // Seat on the central skin surface, not the highest horn/ear vertex.
        if(!Float.isFinite(seat))seat=minZ;
        return new Attachment(chosen==null?-1:chosen.ID,x,y,seat-.01f,TeraVisualMath.crownScale(maxX-minX));
    }
    private static void headVertices(Bone bone,Set<DeformVertex> result){
        result.addAll(bone.verts.keySet());
        for(Bone child:bone.children){String n=child.name.toLowerCase(Locale.ROOT);if(!n.contains("ear") && !n.contains("horn") && !n.contains("hair"))headVertices(child,result);}
    }
    private static final class Attachment {
        final int bone;final float x,y,z,scale;
        Attachment(int bone,float x,float y,float z,float scale){this.bone=bone;this.x=x;this.y=y;this.z=z;this.scale=scale;}
    }
    private static final class Context {
        final Context parent;final AbstractClientEntity entity;final IRenderTypeBuffer buffer;final int type;final float partial,ticks;
        final Set<ValveStudioModel> seen=Collections.newSetFromMap(new IdentityHashMap<>());
        final List<Draw> draws=new ArrayList<>();
        boolean auxiliary,body,crown;
        Context(Context parent,AbstractClientEntity entity,IRenderTypeBuffer buffer,float partial,int type){this.parent=parent;this.entity=entity;this.buffer=buffer;this.partial=partial;this.type=type;this.ticks=entity.field_70173_aa+partial;}
    }
    private static final class Draw {
        final ValveStudioModel model;final MatrixStack matrix;final int light,overlay;final float alpha;final Attachment attachment;final net.minecraft.util.math.vector.Matrix4f bonePose;
        Draw(ValveStudioModel model,MatrixStack matrix,int light,int overlay,float alpha,Attachment attachment,net.minecraft.util.math.vector.Matrix4f bonePose){this.model=model;this.matrix=matrix;this.light=light;this.overlay=overlay;this.alpha=alpha;this.attachment=attachment;this.bonePose=bonePose;}
    }
    @SubscribeEvent public static void particles(net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent event){
        if(!(event.getEntityLiving() instanceof PixelmonEntity))return;
        PixelmonEntity entity=(PixelmonEntity)event.getEntityLiving();Minecraft mc=Minecraft.func_71410_x();
        if(!entity.field_70170_p.field_72995_K || mc.field_71439_g==null || entity.func_82150_aj() || entity.stopRender || entity.func_70068_e(mc.field_71439_g)>1024)return;
        int type=type(entity);if(type<0 || type>=19)return;
        if(TeraTransformation.age(entity,0)>=0){TeraTransformation.tick(entity,type);return;}
        if((entity.field_70173_aa+entity.func_145782_y())%8!=0)return;
        float[] color=TeraVisualMath.color(type,entity.field_70173_aa);
        Random random=entity.field_70170_p.field_73012_v;
        double x=entity.func_226277_ct_()+(random.nextDouble()-.5)*entity.func_213311_cf();
        double y=entity.func_226278_cu_()+random.nextDouble()*entity.func_213302_cg();
        double z=entity.func_226281_cx_()+(random.nextDouble()-.5)*entity.func_213311_cf();
        entity.field_70170_p.func_195594_a(new RedstoneParticleData(color[0],color[1],color[2],.65f),x,y,z,0,.015,0);
        if(entity.field_70173_aa%16<8)entity.field_70170_p.func_195594_a(ParticleTypes.field_197624_q,x,y,z,0,.008,0);
    }
    @Mod.EventBusSubscriber(modid=Content.NS,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Reload {
        @SubscribeEvent public static void textures(net.minecraftforge.client.event.TextureStitchEvent.Pre event){clear();}
    }
    private static void clear(){CROWNS.clear();ANCHORS.clear();FAILED.clear();SURFACES.clear();TeraFacetTextures.clear();TeraTransformation.clear();context=null;}
    @SubscribeEvent public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggedOutEvent event){clear();}
    @SubscribeEvent public static void cacheTick(net.minecraftforge.event.TickEvent.ClientTickEvent event){
        if(event.phase!=net.minecraftforge.event.TickEvent.Phase.START)return;
        TeraFacetTextures.trim();Iterator<ResourceLocation> keys=SURFACES.keySet().iterator();while(SURFACES.size()>64 && keys.hasNext()){keys.next();keys.remove();}
    }
}
