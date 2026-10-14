package local.pixelmon.backport.client;

import java.lang.reflect.Field;
import net.minecraft.util.math.vector.Vector3d;

/** Subtract the camera in double precision before submitting float vertices. */
public final class DetectorLineCoordinates {
    private DetectorLineCoordinates(){}
    private static final ClassValue<Field[]> POSITIONS=new ClassValue<>(){
        protected Field[] computeValue(Class<?> type){
            try{return new Field[]{type.getField("x"),type.getField("y"),type.getField("z")};}
            catch(ReflectiveOperationException e){throw new IllegalStateException("Unsupported detector marker coordinates",e);}
        }
    };
    public static float relative(double position,double camera){return (float)(position-camera);}
    public static Vector3d position(Object marker){
        Field[] fields=POSITIONS.get(marker.getClass());
        try{return new Vector3d(fields[0].getDouble(marker),fields[1].getDouble(marker)+0.5,fields[2].getDouble(marker));}
        catch(IllegalAccessException e){throw new IllegalStateException("Cannot read detector marker coordinates",e);}
    }
}
