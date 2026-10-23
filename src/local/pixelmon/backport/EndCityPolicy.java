package local.pixelmon.backport;
import java.util.function.BiPredicate;

/** A city is accepted as a whole, before any world blocks are placed. */
public final class EndCityPolicy {
    private EndCityPolicy(){}
    public static boolean outsideCenter(int minX,int minZ,int maxX,int maxZ){
        long x=minX>0?minX:maxX<0?maxX:0,z=minZ>0?minZ:maxZ<0?maxZ:0;
        return x*x+z*z>1024L*1024;
    }
    public static boolean supported(int minX,int minZ,int maxX,int maxZ,BiPredicate<Integer,Integer> ground){
        long width=(long)maxX-minX+1,depth=(long)maxZ-minZ+1;
        if(width<=0||depth<=0||width>512||depth>512||!outsideCenter(minX,minZ,maxX,maxZ))return false;
        // Fail cheaply for the common case of a void edge. Every column is
        // still checked after this coarse pass; samples alone cannot accept it.
        for(int x=minX;x<=maxX;x+=8)for(int z=minZ;z<=maxZ;z+=8)if(!ground.test(x,z))return false;
        for(int x=minX;x<=maxX;x++)for(int z=minZ;z<=maxZ;z++)if(!ground.test(x,z))return false;
        return true;
    }
}
