package qa;
import java.nio.file.*;
import net.minecraft.client.Minecraft;
import net.minecraft.particles.*;
import com.pixelmonmod.pixelmon.entities.pokeballs.*;
import com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.PokeBallRegistry;
public final class Trail190Cases {
 public static boolean recording;public static int count;public static IParticleData last;static int checks;
 static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
 static int flash(PokeBallEntity b){count=0;last=null;recording=true;try{b.premierFlash();}finally{recording=false;}return count;}
 public static void run()throws Exception{
  var mc=Minecraft.func_71410_x();var radiant=PokeBallRegistry.getPokeBall("radiant_ball").getValueUnsafe();
  var b=new EmptyPokeBallEntity(mc.field_71441_e,mc.field_71439_g,radiant,false);b.func_213293_j(.1,.1,.1);
  check(flash(b)==1&&last instanceof RedstoneParticleData,"Native Premier renderer callback emits Radiant dust");var colour=(RedstoneParticleData)last;
  check(Math.abs(colour.func_197562_c()-1)<.001&&Math.abs(colour.func_197563_d()-.74)<.001&&Math.abs(colour.func_197561_e()-.12)<.001,"Native particle RGB is gold");
  int emissions=0;for(int tick=1;tick<=20;tick++){b.field_70173_aa=tick;for(int frame=0;frame<24;frame++)emissions+=flash(b);}check(emissions==20,"480 rendering callbacks limited to20 particles");b.field_70173_aa++;
  b.func_213293_j(0,0,0);check(flash(b)==0,"Stopped ball has no trail");b.func_213293_j(.1,.1,.1);PokeBallEntity.SYNC_IS_WAITING.set(b,true);check(flash(b)==0,"Waiting/capture ball has no trail");PokeBallEntity.SYNC_IS_WAITING.set(b,false);PokeBallEntity.SYNC_IS_ON_GROUND.set(b,true);check(flash(b)==0,"Grounded ball has no trail");PokeBallEntity.SYNC_IS_ON_GROUND.set(b,false);b.func_70106_y();check(flash(b)==0,"Removed ball has no trail");
  var premier=new EmptyPokeBallEntity(mc.field_71441_e,mc.field_71439_g,PokeBallRegistry.getPokeBall("premier_ball").getValueUnsafe(),false);premier.func_213293_j(.1,.1,.1);check(flash(premier)==1,"Original Premier effect preserved");
  var master=new EmptyPokeBallEntity(mc.field_71441_e,mc.field_71439_g,PokeBallRegistry.getPokeBall("master_ball").getValueUnsafe(),false);master.func_213293_j(.1,.1,.1);check(flash(master)==0,"Other ball retains native absence of trail");
  Files.writeString(Path.of("trail190-results.json"),"{\"pass\":true,\"assertions\":"+checks+"}");BossBalance190Probe.log("TRAIL190_PASS "+checks);
 }
}
