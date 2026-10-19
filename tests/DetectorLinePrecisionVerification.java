import local.pixelmon.backport.client.DetectorLineCoordinates;
public final class DetectorLinePrecisionVerification {
    public static final class Marker {public double x,y,z;}
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[] args){
        for(double camera:new double[]{0,155555.5,445561.5,12548484.5,1584848.5,-12548484.5,29999000.5}){
            for(double offset:new double[]{.002,.025,1.625,29.43,100.125}){
                float relative=DetectorLineCoordinates.relative(camera+offset,camera);
                check(Math.abs(relative-offset)<.00001,"Precision lost at "+camera);
            }
        }
        Marker m=new Marker();m.x=12548484.875;m.y=78;m.z=-1584848.25;
        var p=DetectorLineCoordinates.position(m);
        check(p.field_72450_a==m.x&&p.field_72448_b==78.5&&p.field_72449_c==m.z,"Native marker center changed");
        check(Math.abs(DetectorLineCoordinates.relative(p.field_72450_a,12548484.5)-.375)<.000001,"Marker offset lost");
        for(double x:new double[]{0,29999000,-29999000})for(float[] look:new float[][]{{0,0,1},{1,0,0},{0,-1,0},{-.6f,.8f,0}}){
            var camera=new net.minecraft.util.math.vector.Vector3d(x,75.25,x);
            var origin=DetectorLineCoordinates.origin(camera,look[0],look[1],look[2]);
            check(Math.abs(DetectorLineCoordinates.relative(origin.field_72450_a,x)-look[0]*2)<1e-5,"Camera X anchor");
            check(Math.abs(DetectorLineCoordinates.relative(origin.field_72448_b,75.25)-look[1]*2)<1e-5,"Camera Y anchor");
            check(Math.abs(DetectorLineCoordinates.relative(origin.field_72449_c,x)-look[2]*2)<1e-5,"Camera Z anchor");
        }
        System.out.println("PASS detector camera-relative double subtraction at reported 155k/445k and 12.5M coordinates, both signs and world-border coordinates; marker center retained.");
    }
}
