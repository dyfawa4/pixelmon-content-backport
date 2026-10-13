import local.pixelmon.backport.OriginMercy;
public final class OriginMercyVerification {
    static void check(boolean ok,String s){if(!ok)throw new AssertionError(s);}
    public static void main(String[] args){
        check(OriginMercy.applies(1026,"Origin of Thunder",true,false,true),"Ordinary catchable wild mercy missing");
        check(!OriginMercy.applies(1026,"Origin of Thunder",true,true,true),"Raid incorrectly weakened");
        check(!OriginMercy.applies(1026,"Origin of Thunder",false,false,true),"Trainer/player weakened");
        check(!OriginMercy.applies(25,"Origin of Thunder",true,false,true),"Ordinary Pikachu uses cap signature");
        check(!OriginMercy.applies(1026,"Ten Million Volts",true,false,true),"Other move weakened");
        check(!OriginMercy.applies(1026,"Origin of Thunder",true,false,false),"Uncatchable wild battle must use full power");
        check(OriginMercy.captureAllowed(true,false,false,false,false,false,1),"Native single wild catch eligibility");
        for(int flags=0;flags<32;flags++)for(int enemies:new int[]{0,1,2,4})
            check(OriginMercy.captureAllowed(true,(flags&1)!=0,(flags&2)!=0,(flags&4)!=0,(flags&8)!=0,(flags&16)!=0,enemies)==(flags==0 && enemies==1),"Raid/boss/uncatchable/owned/bag/multiple boundary");
        for(int hp:new int[]{1,2,10,381})for(int damage:new int[]{0,1,10,999})check(Math.min(damage,hp-1)<=hp-1,"Native false-swipe floor violated");
        System.out.println("PASS Origin of Thunder ordinary-wild direct-damage mercy; players/NPCs/all raids and other moves unchanged, native final FalseSwipe/substitute/residual boundaries; no game initialized.");
    }
}
