import java.util.*;
import local.pixelmon.backport.GymTrainingPlan;
public final class GymTrainingVerification {
    static void check(GymTrainingPlan p){
        assert p.valid();assert Arrays.stream(p.evs).sum()==510;
        assert Arrays.stream(p.evs).filter(v->v==252).count()==2;
        for(int iv:p.ivs)assert iv>=0 && iv<=31;
    }
    public static void main(String[] args)throws Exception{
        var fast=GymTrainingPlan.choose(new int[]{80,120,80,60,80,100},10000,1000,1,false,false,false);check(fast);
        assert fast.nature.equals("JOLLY") && fast.evs[1]==252 && fast.evs[5]==252;
        var special=GymTrainingPlan.choose(new int[]{90,80,100,130,100,70},1000,10000,1,true,false,false);check(special);
        assert special.nature.equals("MODEST") && special.evs[0]==252 && special.evs[3]==252;
        var wall=GymTrainingPlan.choose(new int[]{255,10,10,75,135,55},0,5000,3,true,false,false);check(wall);
        assert wall.nature.equals("BOLD") && wall.evs[0]==252 && wall.evs[2]==252;
        var room=GymTrainingPlan.choose(new int[]{70,55,95,95,110,65},0,9000,1,false,true,false);check(room);
        assert room.nature.equals("QUIET") && room.ivs[5]==0 && room.evs[5]==0;
        var antiRoom=GymTrainingPlan.choose(new int[]{100,100,90,154,90,130},1000,10000,1,false,true,false);check(antiRoom);
        assert antiRoom.nature.equals("TIMID") && antiRoom.ivs[5]==31;
        for(int speed=20;speed<=160;speed+=10)for(int utility=0;utility<=4;utility++)for(boolean physical:new boolean[]{true,false})for(boolean trick:new boolean[]{true,false}){
            check(GymTrainingPlan.choose(new int[]{80,110,90,120,100,speed},physical?10000:0,physical?0:10000,utility,true,trick,true));
        }
        for(String[] pair:new String[][]{{"IVStore","MAX_IVS","31"},{"EVStore","MAX_EVS","252"},{"EVStore","MAX_TOTAL_EVS","510"}}){
            var node=Version13StaticVerification.node("com/pixelmonmod/pixelmon/api/pokemon/stats/"+pair[0]);
            assert node.fields.stream().anyMatch(f->f.name.equals(pair[1]) && Integer.valueOf(pair[2]).equals(f.value)):"Native legal limit changed";
        }
        for(int bits=0;bits<64;bits++)for(int mode=0;mode<3;mode++){
            int[] original={bits&1,(bits>>1)&1,(bits>>2)&1,(bits>>4)&1,(bits>>5)&1,(bits>>3)&1};
            var plan=GymTrainingPlan.choose(new int[]{80,110,90,120,100,mode==2?60:100},mode==0?10000:0,mode==0?0:10000,1,false,mode==2,false);
            plan.preserveHiddenPower(original);check(plan);
            assert GymTrainingPlan.hiddenPowerIndex(plan.ivs)==GymTrainingPlan.hiddenPowerIndex(original):"Training changed Hidden Power coverage";
        }
        var trainer=Version13StaticVerification.node("local/pixelmon/backport/NpcGimmicks");boolean guarded=false;
        for(var method:trainer.methods)if(method.name.equals("prepare")){
            boolean role=false;
            for(var ins:method.instructions)if(ins instanceof org.objectweb.asm.tree.MethodInsnNode){
                var call=(org.objectweb.asm.tree.MethodInsnNode)ins;
                if(call.owner.equals("local/pixelmon/backport/GymCities") && call.name.equals("isLeader"))role=true;
                if(call.owner.equals("local/pixelmon/backport/NpcTraining") && call.name.equals("prepare"))guarded|=role;
            }
        }
        assert guarded:"Training not wired through leader identity";
        var training=Version13StaticVerification.node("local/pixelmon/backport/NpcTraining");
        for(var method:training.methods)for(var ins:method.instructions)if(ins instanceof org.objectweb.asm.tree.MethodInsnNode){
            String name=((org.objectweb.asm.tree.MethodInsnNode)ins).name;
            assert !Set.of("setLevelNum","setTempLevel","setBattleStats").contains(name):"Training changes level/base stats";
        }
        System.out.println("PASS 497 role/spread and Hidden Power boundaries plus native limits and leader wiring: legal IVs, 252/stat and 510 total EVs; no games initialized");
    }
}
