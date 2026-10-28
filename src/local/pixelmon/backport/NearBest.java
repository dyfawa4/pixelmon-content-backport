package local.pixelmon.backport;
import java.util.Random;
/** Weighted variation inside an explicit quality floor; no scores or priorities are mutated. */
public final class NearBest {
    private NearBest(){}
    public static int choose(double[] scores,double gap,double temperature,Random random){
        int best=-1;
        for(int i=0;i<scores.length;i++)if(Double.isFinite(scores[i])&&(best<0||scores[i]>scores[best]))best=i;
        if(best<0||gap<=0||temperature<=0)return best;
        double floor=scores[best]-gap,total=0;int selected=best;
        for(int i=0;i<scores.length;i++)if(Double.isFinite(scores[i])&&scores[i]>=floor){
            double weight=Math.exp((scores[i]-scores[best])/temperature);total+=weight;
            if(random.nextDouble()*total<weight)selected=i;
        }
        return selected;
    }
}
