package local.pixelmon.backport;
/** Deterministic rules adapted from upstream Tera strategies, in game ticks. */
public final class TeraRaidRules {
    public long elapsed;
    public float shield;
    public boolean raised,stolen;
    public int lastSecond=-1;
    public boolean shouldRaise(float hp){return !raised && (hp<=50 || elapsed>=3600);}
    public void raise(){if(!raised){raised=true;shield=100;}}
    public float damage(float raw,boolean tera,boolean matching){return Math.max(0,Math.round(raw*(tera?(matching?.75f:.35f):.2f)));}
    public void hit(float damage){shield=Math.max(0,shield-Math.max(0,damage));}
    public int segments(){return shield<=0?0:(int)Math.ceil(shield/100*8);}
}
