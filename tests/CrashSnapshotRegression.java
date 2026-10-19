import java.util.*;
import java.util.jar.JarFile;
import java.lang.reflect.InvocationTargetException;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketBuffer;
import com.pixelmonmod.pixelmon.api.pokemon.Element;

/** Execute the released Snapshot class itself against the crash-shaped null type list. */
public final class CrashSnapshotRegression {
    public static void main(String[] args)throws Exception{
        for(int i=0;i<args.length;i++){
            String path=args[i],name="local.pixelmon.backport.TeraNetwork$Snapshot";
            byte[] bytes;try(var jar=new JarFile(path)){bytes=jar.getInputStream(jar.getJarEntry(name.replace('.','/')+".class")).readAllBytes();}
            Class<?> clazz=new ClassLoader(CrashSnapshotRegression.class.getClassLoader()){
                Class<?> loadSnapshot(){return defineClass(name,bytes,0,bytes.length);}
            }.loadSnapshot();
            Object snapshot=clazz.getConstructor(int.class).newInstance(1);
            @SuppressWarnings("unchecked") Map<UUID,List<Element>> defense=(Map<UUID,List<Element>>)clazz.getField("defense").get(snapshot);
            defense.put(UUID.randomUUID(),Arrays.asList(Element.FIRE,null));
            PacketBuffer buffer=new PacketBuffer(Unpooled.buffer());boolean npe=false;
            try{clazz.getMethod("encode",clazz,PacketBuffer.class).invoke(null,snapshot,buffer);}
            catch(InvocationTargetException error){if(!(error.getCause() instanceof NullPointerException))throw error;npe=true;}
            finally{buffer.release();}
            if(i==0&&!npe)throw new AssertionError("Old release no longer reproduces the crash: "+path);
            if(i>0&&npe)throw new AssertionError("Fixed release still crashes: "+path);
            System.out.println((npe?"REPRODUCED":"PASS")+" released Snapshot.encode [FIRE,null]: "+path);
        }
    }
}
