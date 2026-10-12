package local.pixelmon.backport;
import java.util.Arrays;
/** Legal competitive spreads, independent of game initialization. Stat order: HP/Atk/Def/SpA/SpD/Spe. */
public final class GymTrainingPlan {
    public final int[] ivs=new int[]{31,31,31,31,31,31},evs=new int[6];
    public final String nature,role;
    private GymTrainingPlan(String nature,String role){this.nature=nature;this.role=role;}
    public static GymTrainingPlan choose(int[] base,double physical,double special,int utility,boolean recovery,boolean room,boolean boosting){
        boolean physicalFocus=physical+special>0?physical>=special:base[1]>=base[3];
        int output=physicalFocus?1:3;
        GymTrainingPlan plan;
        if(room && base[5]<=85){
            plan=new GymTrainingPlan(physicalFocus?"BRAVE":"QUIET","slow-room");
            plan.ivs[5]=0;plan.evs[0]=252;plan.evs[output]=252;plan.evs[2]=6;
        }else if(utility>=2 && recovery && base[0]+base[2]+base[4]>=270){
            boolean defense=base[2]<=base[4];
            plan=new GymTrainingPlan(defense?(physicalFocus?"IMPISH":"BOLD"):(physicalFocus?"CAREFUL":"CALM"),"defensive-support");
            plan.evs[0]=252;plan.evs[defense?2:4]=252;plan.evs[defense?4:2]=6;
        }else if(base[5]>=85 || boosting){
            plan=new GymTrainingPlan(physicalFocus?"JOLLY":"TIMID","fast-offense");
            plan.evs[output]=252;plan.evs[5]=252;plan.evs[0]=6;
        }else{
            plan=new GymTrainingPlan(physicalFocus?"ADAMANT":"MODEST","bulky-offense");
            plan.evs[0]=252;plan.evs[output]=252;plan.evs[5]=6;
        }
        if(!plan.valid())throw new IllegalStateException("Illegal gym training spread");return plan;
    }
    public boolean valid(){return Arrays.stream(ivs).allMatch(v->v>=0 && v<=31) && Arrays.stream(evs).allMatch(v->v>=0 && v<=252) && Arrays.stream(evs).sum()==510;}
    public static int hiddenPowerIndex(int[] values){return ((values[0]&1)+2*(values[1]&1)+4*(values[2]&1)+8*(values[5]&1)+16*(values[3]&1)+32*(values[4]&1))*15/63;}
    public void preserveHiddenPower(int[] original){
        int target=hiddenPowerIndex(original);boolean slow=ivs[5]==0;
        int[] best=null;double score=-Double.MAX_VALUE;
        // Search all native parity combinations, retaining low speed when compatible.
        for(int pass=0;pass<2 && best==null;pass++)for(int bits=0;bits<64;bits++){
            int[] values={30+(bits&1),30+((bits>>1)&1),30+((bits>>2)&1),30+((bits>>4)&1),30+((bits>>5)&1),30+((bits>>3)&1)};
            if(slow && pass==0){if((values[5]&1)!=0)continue;values[5]=0;}
            if(hiddenPowerIndex(values)!=target)continue;
            double benefit=0;for(int i=0;i<6;i++)benefit+=values[i]*(i==5 && evs[i]>0?6:(i==1 || i==3) && evs[i]>0?4:1+evs[i]/126d);
            if(benefit>score){score=benefit;best=values;}
        }
        if(best==null)throw new IllegalStateException("No legal Hidden Power spread");System.arraycopy(best,0,ivs,0,6);
    }
}
