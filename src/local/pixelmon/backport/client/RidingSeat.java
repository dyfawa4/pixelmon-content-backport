package local.pixelmon.backport.client;
import java.util.*;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.pixelmonmod.pixelmon.client.models.PixelmonModelSmd;
import com.pixelmonmod.pixelmon.client.models.smd.*;
import com.pixelmonmod.pixelmon.entities.pixelmon.*;
import local.pixelmon.backport.RidingMotion;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.vector.*;

/** Animated torso surface in entity space; never moves an entity or collision box. */
public final class RidingSeat {
    private static final Map<ValveStudioModel,Set<DeformVertex>> BODIES=new WeakHashMap<>();
    private static final Map<PixelmonEntity,Anchor> ANCHORS=new WeakHashMap<>();
    private static final Map<PixelmonEntity,Turn> TURNS=new WeakHashMap<>();
    private static long frame;
    private static class Turn {float yaw;long frame=-1,nanos;}
    private static Context context;
    private static class Context {Context parent;PixelmonEntity entity;Matrix4f inverse;boolean captured;float yaw,bodyYaw,previousBodyYaw;}
    private static class Anchor {UUID rider;Vector3d target=Vector3d.field_186680_a,previous=target,current=target;int seen;double cameraTop,previousTop;}
    public static void begin(AbstractClientEntity entity,MatrixStack matrix,float partial){
        Context c=new Context();c.parent=context;context=c;if(!RidingMotion.active(entity))return;c.entity=(PixelmonEntity)entity;
        c.inverse=new Matrix4f(matrix.func_227866_c_().func_227870_a_());if(!c.inverse.func_226600_c_()){c.entity=null;return;}
        c.yaw=frameYaw(c.entity,partial);
        c.bodyYaw=c.entity.field_70761_aq;c.previousBodyYaw=c.entity.field_70760_ar;c.entity.field_70761_aq=c.entity.field_70760_ar=c.yaw;
    }
    public static void end(){if(context!=null){if(context.entity!=null){context.entity.field_70761_aq=context.bodyYaw;context.entity.field_70760_ar=context.previousBodyYaw;}context=context.parent;}}
    public static void nextFrame(){frame++;}
    public static void capture(ValveStudioModel model,MatrixStack matrix){
        Context c=context;if(c==null||c.entity==null||c.captured||model.body==null||!(c.entity.getModel() instanceof PixelmonModelSmd)||((PixelmonModelSmd<?>)c.entity.getModel()).theModel!=model)return;
        c.captured=true;PixelmonEntity e=c.entity;Matrix4f transform=new Matrix4f(c.inverse);transform.func_226595_a_(matrix.func_227866_c_().func_227870_a_());
        Set<DeformVertex> body=BODIES.computeIfAbsent(model,RidingSeat::body);Map<DeformVertex,Vector3d> positions=new IdentityHashMap<>();
        double minX=Double.POSITIVE_INFINITY,minY=minX,minZ=minX,maxX=-minX,maxY=-minX,maxZ=-minX;
        for(DeformVertex vertex:body){Vector3d p=framePoint(vertex,transform,c.yaw);positions.put(vertex,p);
            minX=Math.min(minX,p.field_72450_a);maxX=Math.max(maxX,p.field_72450_a);minY=Math.min(minY,p.field_72448_b);maxY=Math.max(maxY,p.field_72448_b);minZ=Math.min(minZ,p.field_72449_c);maxZ=Math.max(maxZ,p.field_72449_c);}
        if(!Double.isFinite(minX+minY+minZ+maxX+maxY+maxZ))return;
        double x=(minX+maxX)*.5,z=(minZ+maxZ)*.5,top=Double.NEGATIVE_INFINITY;
        if(body.size()==model.body.verts.size()){
            Entity passenger=e.func_184179_bs();if(passenger!=null){Vector3d original=new Vector3d(passenger.func_226277_ct_()-e.func_226277_ct_(),0,passenger.func_226281_cx_()-e.func_226281_cx_()).func_178785_b((float)Math.toRadians(c.yaw));x=Math.max(minX+.01,Math.min(maxX-.01,original.field_72450_a));z=Math.max(minZ+.01,Math.min(maxZ-.01,original.field_72449_c));}
        }else{
            // Place the saddle toward the shoulders, in the model's own frame.
            // The head supplies the anatomical forward direction for any rig;
            // no species-specific axis or offset is required.
            double hx=0,hz=0;int count=0;
            for(Bone bone:model.body.bones)if(bone.name.toLowerCase(Locale.ROOT).matches(".*(head|skull).*")&&!bone.name.toLowerCase(Locale.ROOT).matches(".*(skin|mesh|end).*")&&bone.verts.size()>=24)
                for(var entry:bone.verts.entrySet())if(entry.getValue()>=.2f){Vector3d p=framePoint(entry.getKey(),transform,c.yaw);hx+=p.field_72450_a;hz+=p.field_72449_c;count++;}
            if(count>0){double dx=hx/count-x,dz=hz/count-z,length=Math.sqrt(dx*dx+dz*dz);
                if(length>.05){dx/=length;dz/=length;double lo=Double.POSITIVE_INFINITY,hi=Double.NEGATIVE_INFINITY;for(Vector3d p:positions.values()){double projection=p.field_72450_a*dx+p.field_72449_c*dz;lo=Math.min(lo,projection);hi=Math.max(hi,projection);}x+=dx*(hi-lo)*.18;z+=dz*(hi-lo)*.18;}}
        }
        for(NormalizedFace face:model.body.faces){Vector3d a=positions.get(face.vertices[0]),b=positions.get(face.vertices[1]),d=positions.get(face.vertices[2]);if(a!=null&&b!=null&&d!=null){double h=surface(x,z,a,b,d);if(Double.isFinite(h))top=Math.max(top,h);}}
        if(body.size()==model.body.verts.size()){
            // A small patch of surface samples avoids choosing a single horn,
            // spike, or the empty gap between segments as the riding surface.
            double radius=Math.max(.08,Math.min(.35,(maxX-minX)*.12));List<double[]> hits=new ArrayList<>();
            for(int ix=-1;ix<=1;ix++)for(int iz=-1;iz<=1;iz++){double sx=x+ix*radius,sz=z+iz*radius,h=Double.NEGATIVE_INFINITY;
                for(NormalizedFace f:model.body.faces){Vector3d p=positions.get(f.vertices[0]),q=positions.get(f.vertices[1]),r=positions.get(f.vertices[2]);if(p!=null&&q!=null&&r!=null){double value=surface(sx,sz,p,q,r);if(Double.isFinite(value))h=Math.max(h,value);}}
                if(Double.isFinite(h))hits.add(new double[]{sx,h,sz});}
            if(!hits.isEmpty()){hits.sort(Comparator.comparingDouble(hit->hit[1]));double[] hit=hits.get(hits.size()/2);x=hit[0];top=hit[1];z=hit[2];}
        }
        if(!Double.isFinite(top)){
            // Do not jump to the global maximum (often a distant horn). Find
            // the nearest substantial horizontal surface instead.
            double closest=Double.POSITIVE_INFINITY,originX=x,originZ=z,bestX=x,bestZ=z;
            for(NormalizedFace f:model.body.faces){Vector3d p=positions.get(f.vertices[0]),q=positions.get(f.vertices[1]),r=positions.get(f.vertices[2]);if(p==null||q==null||r==null)continue;
                Vector3d normal=q.func_178788_d(p).func_72431_c(r.func_178788_d(p));if(normal.func_189985_c()<1e-9||Math.abs(normal.field_72448_b)/Math.sqrt(normal.func_189985_c())<.4)continue;
                double sx=(p.field_72450_a+q.field_72450_a+r.field_72450_a)/3,sz=(p.field_72449_c+q.field_72449_c+r.field_72449_c)/3,sy=(p.field_72448_b+q.field_72448_b+r.field_72448_b)/3,d=(sx-originX)*(sx-originX)+(sz-originZ)*(sz-originZ);
                if(d<closest){closest=d;top=sy;bestX=sx;bestZ=sz;}}
            x=bestX;z=bestZ;
        }
        if(!Double.isFinite(top))return;Entity rider=e.func_184179_bs();if(rider==null)return;
        Vector3d desired=new Vector3d(x,top-Math.min(.8,rider.func_213302_cg()*.44)+.025,z);
        if(!Double.isFinite(desired.field_72450_a+desired.field_72448_b+desired.field_72449_c)||desired.func_189985_c()>1024)return;
        // The seated body stays on the torso. A first-person camera may need
        // clearance above a long neck/horn; inspect the real animated mesh in
        // the center corridor instead of cutting away visible triangles.
        double corridor=Math.max(.45,(maxX-minX)*.32),safeTop=top;List<Double> centerHeights=new ArrayList<>();
        for(DeformVertex vertex:model.body.verts){Vector3d p=framePoint(vertex,transform,c.yaw);double side=p.field_72450_a-x,front=p.field_72449_c-z;
            if(Math.abs(side)<corridor&&Math.abs(front)<Math.max(4,(maxZ-minZ)*1.5))centerHeights.add(p.field_72448_b);}
        if(!centerHeights.isEmpty()){Collections.sort(centerHeights);safeTop=Math.max(safeTop,centerHeights.get((int)((centerHeights.size()-1)*.95)));}
        Anchor a=ANCHORS.computeIfAbsent(e,key->new Anchor());if(!rider.func_110124_au().equals(a.rider)){a.previous=a.current=desired;a.previousTop=a.cameraTop=safeTop;a.rider=rider.func_110124_au();}
        a.target=desired;a.seen=e.field_70173_aa;a.cameraTop=Math.max(safeTop,a.cameraTop-.015);
    }
    private static double surface(double x,double z,Vector3d a,Vector3d b,Vector3d c){
        double det=(b.field_72449_c-c.field_72449_c)*(a.field_72450_a-c.field_72450_a)+(c.field_72450_a-b.field_72450_a)*(a.field_72449_c-c.field_72449_c);if(Math.abs(det)<1e-9)return Double.NaN;
        double u=((b.field_72449_c-c.field_72449_c)*(x-c.field_72450_a)+(c.field_72450_a-b.field_72450_a)*(z-c.field_72449_c))/det,v=((c.field_72449_c-a.field_72449_c)*(x-c.field_72450_a)+(a.field_72450_a-c.field_72450_a)*(z-c.field_72449_c))/det;
        return u<-.001||v<-.001||u+v>1.001?Double.NaN:u*a.field_72448_b+v*b.field_72448_b+(1-u-v)*c.field_72448_b;
    }
    private static int score(String text){String n=text.toLowerCase(Locale.ROOT);if(excluded(n))return 0;return n.contains("shell")||n.contains("carapace")?100:n.contains("torso")?95:n.contains("body")||n.contains("belly")||n.contains("abdomen")?90:n.contains("chest")||n.contains("spine")?80:n.contains("waist")||n.contains("pelvis")||n.contains("hip")?75:n.contains("root")||n.contains("origin")?5:0;}
    private static boolean excluded(String n){return n.toLowerCase(Locale.ROOT).matches(".*(wing|arm|leg|foot|hand|tail|neck|head|face|mouth|jaw|eye|ear|horn|hair|flower|leaf|fin|skin|mesh|fire|flame|mane|effect).*");}
    private static Set<DeformVertex> body(ValveStudioModel model){
        Set<DeformVertex> bestSet=Collections.newSetFromMap(new IdentityHashMap<>()),torso=Collections.newSetFromMap(new IdentityHashMap<>());int best=-1;
        for(Bone bone:model.body.bones){if(excluded(bone.name))continue;Set<DeformVertex> candidate=Collections.newSetFromMap(new IdentityHashMap<>());collect(bone,candidate);
            int priority=score(bone.name);if(candidate.size()>=Math.max(24,model.body.verts.size()/50)){if(priority>=75)torso.addAll(candidate);if(priority>best||priority==best&&candidate.size()>bestSet.size()){best=priority;bestSet=candidate;}}}
        if(!torso.isEmpty())return torso;
        if(bestSet.size()<24)bestSet.addAll(model.body.verts);return bestSet;
    }
    private static void collect(Bone bone,Set<DeformVertex> vertices){for(var entry:bone.verts.entrySet())if(entry.getValue()>=.20f)vertices.add(entry.getKey());for(Bone child:bone.children)if(!excluded(child.name)&&(score(child.name)>=5||score(bone.name)<5))collect(child,vertices);}
    public static void tickAll(){for(var it=ANCHORS.entrySet().iterator();it.hasNext();){var entry=it.next();var e=entry.getKey();var a=entry.getValue();if(!RidingMotion.active(e)){it.remove();continue;}a.previous=a.current;a.current=a.current.func_186678_a(.65).func_178787_e(a.target.func_186678_a(.35));}TURNS.keySet().removeIf(e->!RidingMotion.active(e));}
    public static Vector3d offset(Entity rider,float partial){
        if(!RidingMotion.active(rider.func_184187_bx()))return Vector3d.field_186680_a;Entity mount=rider.func_184187_bx();Anchor a=ANCHORS.get((PixelmonEntity)mount);if(a==null||!rider.func_110124_au().equals(a.rider))return Vector3d.field_186680_a;
        // Smooth the model seat, not its difference from the native passenger position.
        // Subtract the current interpolated passenger point so native seat switching
        // cannot feed back into the visual seat and shake the camera.
        Vector3d relative=new Vector3d(lerp(rider.field_70169_q,rider.func_226277_ct_(),partial)-lerp(mount.field_70169_q,mount.func_226277_ct_(),partial),lerp(rider.field_70167_r,rider.func_226278_cu_(),partial)-lerp(mount.field_70167_r,mount.func_226278_cu_(),partial),lerp(rider.field_70166_s,rider.func_226281_cx_(),partial)-lerp(mount.field_70166_s,mount.func_226281_cx_(),partial));
        Vector3d local=a.previous.func_186678_a(1-partial).func_178787_e(a.current.func_186678_a(partial));
        return local.func_178785_b((float)-Math.toRadians(frameYaw((PixelmonEntity)mount,partial))).func_178788_d(relative);
    }
    private static float frameYaw(PixelmonEntity e,float partial){
        Turn turn=TURNS.get(e);long now=System.nanoTime();
        if(turn==null){turn=new Turn();turn.yaw=e.field_70761_aq;turn.nanos=now;TURNS.put(e,turn);}
        if(turn.frame!=frame){double seconds=Math.min(.25,Math.max(0,(now-turn.nanos)*1.e-9));float blend=(float)(1-Math.exp(-seconds*18));turn.yaw=net.minecraft.util.math.MathHelper.func_76142_g(turn.yaw+net.minecraft.util.math.MathHelper.func_76142_g(e.field_70761_aq-turn.yaw)*blend);turn.nanos=now;turn.frame=frame;}
        return turn.yaw;
    }
    private static Vector3d framePoint(DeformVertex vertex,Matrix4f transform,float yaw){Vector4f p=new Vector4f(vertex.x,vertex.y,vertex.z,1);p.func_229372_a_(transform);return new Vector3d(p.func_195910_a(),p.func_195913_b(),p.func_195914_c()).func_178785_b((float)Math.toRadians(yaw));}
    private static double lerp(double old,double now,float partial){return old+(now-old)*partial;}
    public static Vector3d cameraOffset(Entity rider,float partial,boolean third){
        Vector3d offset=offset(rider,partial);if(third||!(rider.func_184187_bx() instanceof PixelmonEntity))return offset;
        Anchor a=ANCHORS.get((PixelmonEntity)rider.func_184187_bx());if(a==null)return offset;
        double seatY=lerp(a.previous.field_72448_b,a.current.field_72448_b,partial),eye=rider.func_70047_e();
        return offset.func_72441_c(0,Math.max(0,a.cameraTop+.18-seatY-eye),0);
    }
    public static void clear(){ANCHORS.clear();BODIES.clear();TURNS.clear();context=null;}
}
