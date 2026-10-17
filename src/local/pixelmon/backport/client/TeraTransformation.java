package local.pixelmon.backport.client;

import java.util.*;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.pixelmonmod.pixelmon.entities.pixelmon.AbstractClientEntity;
import com.pixelmonmod.pixelmon.comm.packetHandlers.evolution.EvolutionStage;
import local.pixelmon.backport.*;
import local.pixelmon.backport.mixin.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.particles.*;
import net.minecraft.util.*;
import net.minecraftforge.registries.ForgeRegistries;

/** Reuses the native Mega/evolution light-beam renderer without starting an
 * evolution, sending its completion packets, changing form or resizing the body. */
public final class TeraTransformation {
    private static final Map<AbstractClientEntity,Long> SOUNDS=new WeakHashMap<>();
    private TeraTransformation(){}
    public static float age(AbstractClientEntity entity,float partial){
        if(!(entity instanceof TeraVisualState))return -1;
        TeraVisualState state=(TeraVisualState)entity;
        if(state.backport$teraVisualType()<0)return -1;
        return TeraVisualMath.transformationAge(entity.field_70170_p.func_82737_E(),state.backport$teraVisualStart(),partial);
    }
    public static void beams(TeraEvolutionBeamsInvoker nativeRenderer,AbstractClientEntity entity,MatrixStack matrix,IRenderTypeBuffer buffer,float partial){
        float age=age(entity,partial);
        if(age<0 || entity.func_82150_aj() || entity.isEvolving())return;
        TeraEvolutionAccess access=(TeraEvolutionAccess)entity;
        EvolutionStage previous=entity.getEvoStage();int ticks=entity.evoAnimTicks,fade=entity.fadeCount;float height=entity.heightDiff;
        try{
            float nativeTime=age/80f*(EvolutionStage.PreAnimation.ticks+EvolutionStage.PostAnimation.ticks);
            boolean pre=nativeTime<EvolutionStage.PreAnimation.ticks;
            access.backport$visualEvolutionStage(pre?EvolutionStage.PreAnimation:EvolutionStage.PostAnimation);
            entity.evoAnimTicks=(int)(pre?nativeTime:nativeTime-EvolutionStage.PreAnimation.ticks);
            entity.fadeCount=20;entity.heightDiff=0;
            nativeRenderer.backport$teraEvolutionBeams(entity,matrix,buffer);
        }finally{
            access.backport$visualEvolutionStage(previous);entity.evoAnimTicks=ticks;entity.fadeCount=fade;entity.heightDiff=height;
        }
    }
    public static void tick(AbstractClientEntity entity,int type){
        float age=age(entity,0);if(age<0)return;
        float[] color=TeraVisualMath.color(type,entity.field_70173_aa);
        Random random=entity.field_70170_p.field_73012_v;
        double radius=Math.max(.4,entity.func_213311_cf()*.7),height=entity.func_213302_cg();
        int count=age<52?12:age<60?28:5;
        for(int i=0;i<count;i++){
            double angle=random.nextDouble()*Math.PI*2;
            double r=radius*(age<52?1.35:.7+random.nextDouble()*1.2);
            double dx=Math.cos(angle)*r,dz=Math.sin(angle)*r;
            double y=entity.func_226278_cu_()+height*(.15+random.nextDouble());
            double speed=age<52?-.06:.09;
            entity.field_70170_p.func_195594_a(ParticleTypes.field_197624_q,entity.func_226277_ct_()+dx,y,entity.func_226281_cx_()+dz,dx*speed,age<52?.025:.055,dz*speed);
            if(i%3==0)entity.field_70170_p.func_195594_a(new RedstoneParticleData(color[0],color[1],color[2],1.1f),entity.func_226277_ct_()+dx,y,entity.func_226281_cx_()+dz,0,.03,0);
        }
        long start=((TeraVisualState)entity).backport$teraVisualStart();long played=SOUNDS.getOrDefault(entity,Long.MIN_VALUE);
        if(age<52 && played!=start){sound(entity,"block.beacon.activate",.7f,1.4f);SOUNDS.put(entity,start);}
        if(age>=52 && age<60 && played!=-start-2){sound(entity,"block.glass.break",.8f,1.4f);SOUNDS.put(entity,-start-2);}
    }
    private static void sound(AbstractClientEntity entity,String name,float volume,float pitch){
        SoundEvent sound=ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("minecraft",name));
        if(sound!=null)Minecraft.func_71410_x().field_71441_e.func_184134_a(entity.func_226277_ct_(),entity.func_226278_cu_(),entity.func_226281_cx_(),sound,SoundCategory.PLAYERS,volume,pitch,false);
    }
    public static void clear(){SOUNDS.clear();}
}
