import java.util.*;
import local.pixelmon.backport.GymTrainingPlan;
public final class GymTrainingVerification {
    static void check(GymTrainingPlan p){
        assert p.valid();assert Arrays.stream(p.evs).sum()==510;
        assert Arrays.stream(p.evs).allMatch(v->v>=0&&v<=252);
        for(int iv:p.ivs)assert iv>=0 && iv<=31;
    }
    public static void main(String[] args)throws Exception{
        var fast=GymTrainingPlan.choose(new int[]{80,120,80,60,80,100},10000,1000,1,false,false,false);check(fast);
        assert fast.nature.equals("JOLLY") && fast.evs[1]==252 && fast.evs[5]==252;
        var special=GymTrainingPlan.choose(new int[]{90,80,100,130,100,70},1000,10000,1,true,false,false);check(special);
        assert special.nature.equals("MODEST") && special.evs[0]==252 && special.evs[3]==252;
        var wall=GymTrainingPlan.choose(new int[]{255,10,10,75,135,55},0,5000,3,true,false,false);check(wall);
        assert wall.role.equals("defensive-support") && wall.evs[2]>0 && wall.evs[3]==0;
        var room=GymTrainingPlan.choose(new int[]{70,55,95,95,110,65},0,9000,1,false,true,false);check(room);
        assert room.nature.equals("QUIET") && room.ivs[5]==31 && room.evs[5]==0;
        var antiRoom=GymTrainingPlan.choose(new int[]{100,100,90,154,90,130},1000,10000,1,false,true,false);check(antiRoom);
        assert antiRoom.nature.equals("TIMID") && antiRoom.ivs[5]==31;
        for(int speed=20;speed<=160;speed+=10)for(int utility=0;utility<=4;utility++)for(boolean physical:new boolean[]{true,false})for(boolean trick:new boolean[]{true,false}){
            var plan=GymTrainingPlan.choose(new int[]{80,110,90,120,100,speed},physical?10000:0,physical?0:10000,utility,true,trick,true);check(plan);assert Arrays.stream(plan.ivs).allMatch(v->v==31);
        }
        var press=GymTrainingPlan.choose(new int[]{92,120,145,80,145,128},10000,0,2,true,false,false,Set.of("Body Press","Iron Defense","Rest"),100);check(press);assert press.evs[2]==252&&press.evs[1]==0&&press.role.equals("body-press");
        var screens=GymTrainingPlan.choose(new int[]{80,80,80,80,80,120},0,0,4,false,false,false,Set.of("Reflect","Light Screen","Taunt"),100);check(screens);assert screens.evs[0]==252&&screens.evs[5]==252&&screens.role.equals("fast-support");
        var mixed=GymTrainingPlan.choose(new int[]{90,110,90,110,90,110},10000,10000,0,false,false,false,Collections.emptySet(),100);check(mixed);assert mixed.evs[5]==252&&mixed.evs[1]>0&&mixed.evs[3]>0&&Set.of("HASTY","NAIVE").contains(mixed.nature);
        for(int speed=20;speed<85;speed+=5)for(boolean physical:new boolean[]{false,true})for(boolean trick:new boolean[]{false,true}){
            var bulkyMixed=GymTrainingPlan.choose(new int[]{90,110,90,110,95,speed},physical?11000:10000,physical?10000:11000,1,false,trick,false);check(bulkyMixed);
            assert trick?Set.of("BRAVE","QUIET").contains(bulkyMixed.nature):Set.of("NAUGHTY","LONELY","RASH","MILD").contains(bulkyMixed.nature):"Non-room mixed attacker lost speed";
        }
        var calm=GymTrainingPlan.choose(new int[]{120,120,120,120,120,120},0,18000,2,true,false,false,Set.of("Calm Mind","Recover","Judgment","Ice Beam"),100);check(calm);assert calm.evs[0]==252&&calm.evs[5]==252&&calm.role.equals("fast-bulky-setup");
        var oneHp=GymTrainingPlan.choose(new int[]{1,90,45,30,30,40},10000,0,2,false,false,false);check(oneHp);assert oneHp.evs[0]==0&&oneHp.evs[1]==252&&oneHp.evs[5]==252;
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
                if((call.owner.equals("local/pixelmon/backport/NpcTraining")&&call.name.equals("maximize")||call.owner.equals("local/pixelmon/backport/AuthorRoster")&&call.name.equals("train")))guarded|=role;
            }
        }
        assert guarded:"Training not wired through leader identity";
        var training=Version13StaticVerification.node("local/pixelmon/backport/NpcTraining");
        for(var method:training.methods)for(var ins:method.instructions)if(ins instanceof org.objectweb.asm.tree.MethodInsnNode){
            String name=((org.objectweb.asm.tree.MethodInsnNode)ins).name;
            assert !Set.of("setLevelNum","setTempLevel","setBattleStats").contains(name):"Training changes level/base stats";
        }
        System.out.println("PASS role/spread, defensive optimization, mixed/press/screens/setup/Trick Room and Hidden Power boundaries plus native limits and leader wiring: IV31, 252/stat and 510 total EVs; no games initialized");
    }
}
