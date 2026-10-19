package local.pixelmon.backport.client;

/** The living-renderer pivot is cancelled once; its animation scale is already in the matrix. */
public final class EvolutionBeamOrigin {
    private EvolutionBeamOrigin(){}
    public static double modelY(double height){return (double)-(-1.501f)-height*.35;}
    public static float length(float original,double width,double height){
        double progress=Math.max(0,Math.min(1,(original-2)/18.0));
        return (float)(Math.max(.65,Math.min(2.5,Math.max(width,height)*.7))*(.2+.8*progress));
    }
    public static float width(float original){return Math.max(.015f,Math.min(.12f,original*.55f));}
}
