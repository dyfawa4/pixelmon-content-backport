package local.pixelmon.backport;
import java.util.Locale;

/** Rendering policy without Minecraft/client dependencies, also used by audits. */
public final class TeraVisualMath {
    // Reflected-light hues need more separation than the muted UI/raid palette.
    // This palette affects only localized crystal reflections, never skin data.
    private static final int[] REFLECTION_COLORS={0xcddfea,0xff590d,0x1268ff,0x29d938,0xffdc12,0x77f1ff,0xff243e,0xb82ef0,0xe08a30,0x90b8ff,0xff49a2,0xa4dd12,0xc5ad68,0x6030c9,0x6338ff,0x3f254f,0x9ac5db,0xff8bdc};
    private TeraVisualMath(){}
    public static int headScore(String name){
        String n=name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]","");
        if(n.equals("head"))return 100;
        if(n.matches("(bip[0-9]*)?head[0-9]*") || n.equals("headjnt") || n.equals("jnthead"))return 90;
        if(n.startsWith("head") && !n.contains("end") && !n.contains("top"))return 75;
        if(n.contains("head") && !n.contains("end") && !n.contains("top") && !n.contains("forehead"))return 70;
        if(n.equals("skull") || n.equals("face"))return 65;
        return 0;
    }
    public static float crownScale(float headWidth){return Math.max(.35f,Math.min(5f,headWidth/7f));}
    public static boolean specialCrown(int dex){return dex==1017 || dex==1024;}
    /** Vertical ray through a rest-pose triangle, NaN outside its footprint. */
    public static float surfaceZ(float x,float y,float ax,float ay,float az,float bx,float by,float bz,float cx,float cy,float cz){
        float d=(by-cy)*(ax-cx)+(cx-bx)*(ay-cy);if(Math.abs(d)<1.e-6f)return Float.NaN;
        float a=((by-cy)*(x-cx)+(cx-bx)*(y-cy))/d,b=((cy-ay)*(x-cx)+(ax-cx)*(y-cy))/d,c=1-a-b;
        return a>=-.0001f && b>=-.0001f && c>=-.0001f?a*az+b*bz+c*cz:Float.NaN;
    }
    public static int facetPixel(int skinAbgr,int facetAbgr){return (((skinAbgr>>>24)*(facetAbgr>>>24)/255)<<24)|0xffffff;}
    public static float transformationAge(long now,long start,float partial){
        if(start<0 || now<start || now-start>=80)return -1;
        return now-start+partial;
    }
    public static float[] color(int type,float ticks){
        if(type<0 || type>=19)throw new IllegalArgumentException("Tera type");
        if(type==18){
            double t=ticks*.025;
            return new float[]{(float)(.65+.35*Math.sin(t)),(float)(.65+.35*Math.sin(t+2.094)),(float)(.65+.35*Math.sin(t+4.189))};
        }
        int c=TeraRaidClientColors.COLORS[type];
        return new float[]{.24f+.76f*((c>>16)&255)/255f,.24f+.76f*((c>>8)&255)/255f,.24f+.76f*(c&255)/255f};
    }
    public static float[] surfaceColor(int type,float ticks){
        if(type==18)return color(type,ticks);
        if(type<0 || type>=19)throw new IllegalArgumentException("Tera type");
        int c=REFLECTION_COLORS[type];
        return new float[]{((c>>16)&255)/255f,((c>>8)&255)/255f,(c&255)/255f};
    }
}
