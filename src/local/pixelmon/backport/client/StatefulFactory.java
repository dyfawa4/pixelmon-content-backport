package local.pixelmon.backport.client;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.pixelmonmod.pixelmon.api.util.ResourceWithFallback;
import com.pixelmonmod.pixelmon.client.models.*;
import com.pixelmonmod.pixelmon.client.models.smd.*;
import com.pixelmonmod.pixelmon.entities.pixelmon.AbstractClientEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

/** Keep separate native model/animation instances for the upstream walk/fly
 * predicates. This avoids applying a closed-shell animation to an open mesh. */
public final class StatefulFactory extends PixelmonSmdFactory {
    private final JsonObject states;
    private StatefulFactory(ResourceWithFallback[] resources,JsonObject states){super(resources);this.states=states;}
    public static PixelmonSmdFactory wrap(PixelmonSmdFactory original,ResourceWithFallback[] resources){
        String ref=resources[0].get().toString();
        if(!ref.startsWith("pixelmon_backport:"))return original;
        String path="assets/pixelmon_backport/"+ref.substring(ref.indexOf(':')+1)+".states.json";
        try(InputStream stream=StatefulFactory.class.getClassLoader().getResourceAsStream(path)){
            return stream==null?original:new StatefulFactory(resources,new JsonParser().parse(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject());
        }catch(IOException e){throw new IllegalStateException(path,e);}
    }
    @Override protected <T extends AbstractClientEntity> PixelmonModelSmd<T> createModel(){return new StateModel<>();}
    private final class StateModel<T extends AbstractClientEntity> extends PixelmonModelSmd<T> {
        private final Map<String,ValveStudioModel> models=new HashMap<>();
        private final Map<String,PixelmonModelRenderer> bodies=new HashMap<>();
        private String selected="always";
        StateModel(){
            add("always",model.get());
            for(Map.Entry<String,JsonElement> entry:states.entrySet()){
                String ref=entry.getValue().getAsString();int sep=ref.indexOf(':');
                add(entry.getKey(),new ResourceLocation(ref.substring(0,sep),"textures/"+ref.substring(sep+1)));
            }
            theModel=models.get(selected);animationIncrement=StatefulFactory.this.animationIncrement;
            movementThreshold=StatefulFactory.this.movementThreshold;
        }
        private void add(String key,ResourceLocation location){
            ValveStudioModel valve=loadModel(location);
            if(valve==null)throw new IllegalStateException("Model failed to load: "+location);
            models.put(key,valve);
            PixelmonModelRenderer body=new PixelmonModelRenderer(this,"body");
            body.addCustomModel(new ModelCustomWrapper(valve));
            body.func_78793_a(xRotation,yRotation,zRotation);
            body.field_78795_f=rotateAngleX;body.field_78796_g=rotateAngleY;
            body.offsetX=xOffset;body.offsetY=yOffset;body.offsetZ=zOffset;
            body.setTransparent(transparency);bodies.put(key,body);
        }
        @Override public void setupForRender(AbstractClientEntity pokemon){
            AnimationType animation=pokemon.getCurrentAnimation();
            selected=animation==AnimationType.FLY&&models.containsKey("flying")?"flying":animation==AnimationType.WALK&&models.containsKey("walk")?"walk":"always";
            theModel=models.get(selected);super.setupForRender(pokemon);
        }
        @Override public void setScale(float scale){for(PixelmonModelRenderer body:bodies.values())body.scale=scale;}
        @Override public void render(Entity pokemon,MatrixStack matrix,IVertexBuilder buffer,int light,int overlay,float r,float g,float b,float a){bodies.get(selected).func_228309_a_(matrix,buffer,light,overlay,r,g,b,a);}
        @Override public void func_225598_a_(MatrixStack matrix,IVertexBuilder buffer,int light,int overlay,float r,float g,float b,float a){}
        @Override public void func_225597_a_(T pokemon,float swing,float amount,float ticks,float yaw,float pitch){}
        @Override protected float getDefaultXRotation(){return rotateAngleX;}
    }
}
