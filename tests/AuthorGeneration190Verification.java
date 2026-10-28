import java.util.*;
import local.pixelmon.backport.AuthorBossPolicy;

/** Density and legacy-placement regression across seeds, region edges and all supported themes. */
public final class AuthorGeneration190Verification {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static int[] legacy(long seed,int rx,int rz){
        Random random=new Random(seed+(long)rx*341873128712L+(long)rz*132897987541L+198473905);
        return new int[]{rx*64+random.nextInt(40),rz*64+random.nextInt(40)};
    }
    public static void main(String[] args){
        List<String> land=new ArrayList<>(AuthorBossPolicy.TYPES.keySet());land.remove("ocean");land.addAll(List.of("ultra","paradox"));
        long[] seeds={0,1,-1,927633,Long.MIN_VALUE,Long.MAX_VALUE};
        int[] regions={-2,-1,0,1};int checked=0;
        for(long seed:seeds)for(int rx:regions)for(int rz:regions){
            int[] old=legacy(seed,rx,rz);int candidates=0,ocean=0;
            for(int dx=0;dx<64;dx++)for(int dz=0;dz<64;dz++){
                int x=rx*64+dx,z=rz*64+dz;
                boolean possible=AuthorBossPolicy.candidate(seed,x,z,false);
                boolean sea=AuthorBossPolicy.candidate(seed,x,z,false,"ocean");
                if(possible)candidates++;
                if(sea){ocean++;check(x==old[0]&&z==old[1],"Ocean must be a subset of 1.8 positions");}
                check(!sea||possible,"Ocean candidate omitted by compass prefilter");
                for(String theme:land)check(AuthorBossPolicy.candidate(seed,x,z,false,theme)==possible,"Two land points for "+theme);
                check(!AuthorBossPolicy.candidate(seed,x,z,true,"plains"),"Retired PVP remains disabled");
            }
            check(candidates==2,"Exactly two distinct non-ocean opportunities in region "+rx+","+rz);
            check(ocean<=1,"No additional ocean site");
            check(AuthorBossPolicy.candidate(seed,old[0],old[1],false,"plains"),"Legacy land position retained");
            check(!AuthorBossPolicy.candidate(seed,old[0],old[1],false,null),"Excluded theme cannot generate");
            checked++;
        }
        int trials=200000,retained=0;
        for(int i=0;i<trials;i++){
            long seed=0x123456789ABCDEFL+i*104729L;int rx=i-100000,rz=(i*31)%200003-100000;
            int[] old=legacy(seed,rx,rz);
            boolean first=AuthorBossPolicy.candidate(seed,old[0],old[1],false,"ocean");
            check(first==AuthorBossPolicy.candidate(seed,old[0],old[1],false,"ocean"),"Decoration order independent");
            if(first)retained++;
        }
        check(Math.abs(retained-trials/2)<trials/100,"Ocean retention near 50%: "+retained);
        for(int rx:new int[]{Integer.MIN_VALUE/64,Integer.MAX_VALUE/64})for(int rz:new int[]{-1,0}){
            int[] old=legacy(71,rx,rz);
            check(AuthorBossPolicy.candidate(71,old[0],old[1],false,"plains"),"Extreme chunk coordinates");
        }
        check("ocean".equals(AuthorBossPolicy.theme("minecraft:overworld","beach","minecraft:beach")),"Existing beach/ocean grouping");
        check(AuthorBossPolicy.theme("pixelmon:drowned_world","ocean","minecraft:ocean")==null,"Drowned exclusion");
        check(AuthorBossPolicy.theme("minecraft:overworld","mushroom","minecraft:mushroom_fields")==null,"Mushroom exclusion");
        System.out.println("PASS 1.9 author density: "+checked+" exhaustive regions, all "+land.size()+" non-ocean themes x2; ocean "+retained+"/"+trials+" legacy sites retained; negative/extreme coordinates and exclusions.");
    }
}
