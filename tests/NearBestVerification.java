import java.util.*;
import local.pixelmon.backport.NearBest;
public final class NearBestVerification {
    public static void main(String[] args){
        int[] counts=new int[5];double[] scores={100,98,70,Double.NaN,Double.POSITIVE_INFINITY};
        Random random=new Random(190);
        for(int i=0;i<20000;i++)counts[NearBest.choose(scores,8,4,random)]++;
        if(counts[0]<=counts[1]||counts[1]<4000||counts[2]+counts[3]+counts[4]!=0)throw new AssertionError(Arrays.toString(counts));
        for(int i=0;i<500;i++){
            if(NearBest.choose(new double[]{100,90},8,4,new Random(i))!=0)throw new AssertionError("Quality floor");
            if(NearBest.choose(scores,0,4,new Random(i))!=0)throw new AssertionError("Protected decisions");
            if(NearBest.choose(scores,8,4,new Random(i))!=NearBest.choose(scores,8,4,new Random(i)))throw new AssertionError("Stable simulation seed");
        }
        if(NearBest.choose(new double[]{Double.NaN,Double.NEGATIVE_INFINITY},8,4,random)!=-1)throw new AssertionError("Invalid pool");
        if(NearBest.choose(new double[]{-100,-101,-200},8,4,random)==2)throw new AssertionError("Negative lead scores");
        System.out.println("NEARBEST190_PASS bounded variation "+Arrays.toString(counts)+", strict priority mode, invalid scores and stable seeds");
    }
}
