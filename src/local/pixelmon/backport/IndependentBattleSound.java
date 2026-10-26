package local.pixelmon.backport;

import com.pixelmonmod.pixelmon.client.music.BattleMusic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.*;
import net.minecraft.util.*;

/** Native battle fade and cleanup; the shared music source applies the slider. */
public final class IndependentBattleSound extends BattleMusic.FixedTrackSound implements ITickableSound {
    private static final java.util.Set<IndependentBattleSound> ACTIVE=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    private long start,fadeIn,stopAt,fadeOut;
    private float stopGain;
    private boolean closing,done;
    private Runnable onStop;
    public IndependentBattleSound(SoundEvent sound,int index,SoundCategory ignored,float volume,float pitch,boolean loop,int delay,ISound.AttenuationType attenuation,float x,float y,float z){
        super(sound,index,SoundCategory.MASTER,volume,pitch,loop,delay,attenuation,x,y,z);
    }
    public void start(long millis){start=System.nanoTime();fadeIn=Math.max(0,millis)*1_000_000L;ACTIVE.add(this);Minecraft.func_71410_x().func_147118_V().func_147682_a(this);}
    private float envelope(){
        long now=System.nanoTime();
        if(closing)return fadeOut==0?0:stopGain*Math.max(0,1-(float)(now-stopAt)/fadeOut);
        return fadeIn==0?1:Math.min(1,(float)(now-start)/fadeIn);
    }
    public void stop(long millis,Runnable callback){if(done||closing)return;stopGain=envelope();stopAt=System.nanoTime();fadeOut=Math.max(0,millis)*1_000_000L;closing=true;onStop=callback;if(millis<=0)finish();}
    private void finish(){if(done)return;done=true;ACTIVE.remove(this);Minecraft.func_71410_x().func_147118_V().func_147683_b(this);if(onStop!=null){var callback=onStop;onStop=null;callback.run();}}
    public static boolean active(){return !ACTIVE.isEmpty();}
    public static void tickPending(){for(var sound:new java.util.ArrayList<>(ACTIVE))sound.func_73660_a();}
    public static void stopAll(){for(var sound:new java.util.ArrayList<>(ACTIVE))sound.stop(0,null);for(var sound:new java.util.ArrayList<>(ACTIVE))sound.finish();}
    @Override public float func_147653_e(){return super.func_147653_e()*envelope();}
    // Keep a silent stream alive so raising volume resumes the current song immediately.
    @Override public boolean func_211503_n(){return true;}
    @Override public boolean func_147667_k(){return done;}
    @Override public void func_73660_a(){if(closing&&System.nanoTime()-stopAt>=fadeOut&&!done)finish();}
    @net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid=Content.NS,value=net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static final class Cleanup {
        @net.minecraftforge.eventbus.api.SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent event){if(event.phase==net.minecraftforge.event.TickEvent.Phase.END)tickPending();}
        @net.minecraftforge.eventbus.api.SubscribeEvent public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggedOutEvent event){com.pixelmonmod.pixelmon.client.music.BattleMusic.endBattleMusic();stopAll();com.pixelmonmod.pixelmon.client.music.VoidMusicTicker.restoreMusicTicker();}
        @net.minecraftforge.eventbus.api.SubscribeEvent public static void unload(net.minecraftforge.event.world.WorldEvent.Unload event){if(event.getWorld() instanceof net.minecraft.client.world.ClientWorld){com.pixelmonmod.pixelmon.client.music.BattleMusic.endBattleMusic();stopAll();com.pixelmonmod.pixelmon.client.music.VoidMusicTicker.restoreMusicTicker();}}
    }
}
