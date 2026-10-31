package local.pixelmon.backport;
import java.util.*;
/** Legal competitive spreads, independent of game initialization. Stat order: HP/Atk/Def/SpA/SpD/Spe. */
public final class GymTrainingPlan {
    public final int[] ivs=new int[]{31,31,31,31,31,31},evs=new int[6];
    public final String nature,role;
    private GymTrainingPlan(String nature,String role){this.nature=nature;this.role=role;}
    public static GymTrainingPlan choose(int[] base,double physical,double special,int utility,boolean recovery,boolean room,boolean boosting){
        return choose(base,physical,special,utility,recovery,room,boosting,Collections.emptySet(),100);
    }
    public static GymTrainingPlan choose(int[] base,double physical,double special,int utility,boolean recovery,boolean room,boolean boosting,Set<String> moves,int level){
        boolean physicalFocus=physical+special>0?physical>=special:base[1]>=base[3];
        int output=physicalFocus?1:3;level=Math.max(1,level);
        boolean slow=room&&base[5]<=85;
        boolean mixed=physical>0&&special>0&&Math.min(physical,special)/Math.max(physical,special)>=.75;
        boolean setup=boosting||has(moves,"Calm Mind","Bulk Up","Coil","Quiver Dance","Geomancy","Nasty Plot","Swords Dance","Dragon Dance","Shell Smash");
        boolean screens=has(moves,"Reflect","Light Screen","Aurora Veil");
        boolean pass=moves.contains("Baton Pass");
        GymTrainingPlan plan;
        if(base[0]==1){
            plan=new GymTrainingPlan(physicalFocus?"JOLLY":"TIMID","one-hp-offense");
            plan.evs[output]=252;plan.evs[5]=252;plan.evs[2]=6;
        }else if(moves.contains("Body Press")&&(has(moves,"Iron Defense","Acid Armor","Cotton Guard")||physical>=special)){
            plan=new GymTrainingPlan(slow?"RELAXED":"IMPISH","body-press");
            plan.evs[0]=252;plan.evs[2]=252;plan.evs[4]=6;
        }else if(!slow&&((screens&&base[5]>=85)||(pass&&(base[5]>=80||has(moves,"Agility","Rock Polish","Shell Smash","Quiver Dance","Dragon Dance")))||(utility>=3&&!recovery&&base[5]>=85))){
            plan=new GymTrainingPlan(physical>special?"JOLLY":"TIMID","fast-support");
            plan.evs[0]=252;plan.evs[5]=252;plan.evs[base[2]<=base[4]?2:4]=6;
        }else if((recovery&&utility>=2&&!setup)||(physical+special==0)||(pass&&!setup&&base[5]<80)){
            plan=bulk(base,physical>special,slow,1,1,level,"defensive-support");
        }else if(recovery&&utility>=2&&setup){
            if(!slow&&base[5]>=100){
                plan=new GymTrainingPlan(physicalFocus?"JOLLY":"TIMID","fast-bulky-setup");
                plan.evs[0]=252;plan.evs[5]=252;plan.evs[output]=6;
            }else{
                float defenceBoost=has(moves,"Bulk Up","Coil","Iron Defense","Acid Armor","Cotton Guard")?1.5f:1;
                float specialBoost=has(moves,"Calm Mind","Quiver Dance","Geomancy")?1.5f:1;
                plan=bulk(base,physicalFocus,slow,defenceBoost,specialBoost,level,"bulky-setup");
            }
        }else if(mixed){
            boolean fast=!slow&&(base[5]>=85||boosting);
            // A bulky spread alone is not a reason to sacrifice speed. Only an
            // actual Trick Room plan may use Brave/Quiet on a mixed attacker.
            String nature=fast?(base[2]<=base[4]?"NAIVE":"HASTY"):slow?(physicalFocus?"BRAVE":"QUIET"):
                physicalFocus?(base[2]<=base[4]?"NAUGHTY":"LONELY"):(base[2]<=base[4]?"RASH":"MILD");
            plan=new GymTrainingPlan(nature,fast?"fast-mixed":"bulky-mixed");
            plan.evs[fast?5:0]=252;plan.evs[fast?0:slow?2:5]=6;
            double best=-Double.MAX_VALUE;
            for(int attack=0;attack<=252;attack+=4){
                int spa=252-attack;
                double value=physical/Math.max(1,base[1])*Math.log(stat(base[1],attack,level,false,Set.of("BRAVE","NAUGHTY","LONELY").contains(nature)))
                    +special/Math.max(1,base[3])*Math.log(stat(base[3],spa,level,false,Set.of("QUIET","RASH","MILD").contains(nature)));
                if(value>best){best=value;plan.evs[1]=attack;plan.evs[3]=spa;}
            }
        }else if(slow){
            plan=new GymTrainingPlan(physicalFocus?"BRAVE":"QUIET","slow-room");
            plan.evs[0]=252;plan.evs[output]=252;plan.evs[2]=6;
        }else if(base[5]>=85 || boosting){
            plan=new GymTrainingPlan(physicalFocus?"JOLLY":"TIMID","fast-offense");
            plan.evs[output]=252;plan.evs[5]=252;plan.evs[0]=6;
        }else{
            plan=new GymTrainingPlan(physicalFocus?"ADAMANT":"MODEST","bulky-offense");
            plan.evs[0]=252;plan.evs[output]=252;plan.evs[5]=6;
        }
        if(!plan.valid())throw new IllegalStateException("Illegal gym training spread");return plan;
    }
    private static boolean has(Set<String> moves,String... names){for(String name:names)if(moves.contains(name))return true;return false;}
    private static int stat(int base,int ev,int level,boolean hp,boolean raised){
        if(hp&&base==1)return 1;
        float value=(31+2f*base+ev/4f+(hp?100:0))*level/100f+(hp?10:5);
        return Math.max(1,hp?(int)value:(int)((float)Math.floor(value)*(raised?1.1f:1)));
    }
    /** Search legal bulk spreads using native rounding; maximize balanced physical/special survival. */
    private static GymTrainingPlan bulk(int[] base,boolean physical,boolean slow,float defenceBoost,float specialBoost,int level,String role){
        GymTrainingPlan best=null;double bestScore=-Double.MAX_VALUE;
        for(boolean defence:new boolean[]{true,false}){
            String nature=slow?(defence?"RELAXED":"SASSY"):physical?(defence?"IMPISH":"CAREFUL"):(defence?"BOLD":"CALM");
            for(int hp=0;hp<=252;hp+=4)for(int def=0;def<=252;def+=4){
                int spd=508-hp-def;if(spd<0||spd>252)continue;
                int h=hp,d=def,s=spd;if(h<=250)h+=2;else if(d<=250)d+=2;else s+=2;
                double health=stat(base[0],h,level,true,false),physicalBulk=stat(base[2],d,level,false,defence)*defenceBoost,specialBulk=stat(base[4],s,level,false,!defence)*specialBoost;
                double score=health*physicalBulk*specialBulk/(physicalBulk+specialBulk);
                if(score>bestScore+1e-9){bestScore=score;best=new GymTrainingPlan(nature,role);best.evs[0]=h;best.evs[2]=d;best.evs[4]=s;}
            }
        }
        if(best==null||!best.valid())throw new IllegalStateException("No legal defensive spread");return best;
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
