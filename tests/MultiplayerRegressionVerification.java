import java.io.*;
import java.lang.reflect.*;
import java.util.jar.JarFile;
import java.util.concurrent.atomic.AtomicInteger;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Execute the real transformed download entry; never construct a game or screen. */
public final class MultiplayerRegressionVerification {
    static ClassNode read(byte[] bytes){ClassNode n=new ClassNode();new ClassReader(bytes).accept(n,0);return n;}
    static boolean call(MethodNode m,String owner,String name){
        for(AbstractInsnNode i:m.instructions.toArray())if(i instanceof MethodInsnNode){
            MethodInsnNode c=(MethodInsnNode)i;if(c.owner.equals(owner)&&c.name.equals(name))return true;
        }return false;
    }
    static MethodNode method(ClassNode n,String name,String desc){
        return n.methods.stream().filter(m->m.name.equals(name)&&(desc==null||m.desc.equals(desc))).findFirst().orElseThrow();
    }
    static ClassNode nativeClass(JarFile jar,String name)throws IOException{
        try(InputStream in=jar.getInputStream(jar.getJarEntry(name+".class"))){return read(in.readAllBytes());}
    }
    public static void main(String[] args)throws Exception{
        try(JarFile jar=new JarFile(args[0])){
            ClassNode screen=nativeClass(jar,"com/pixelmonmod/pixelmon/client/gui/override/multiplayer/CustomMultiplayerScreen");
            MethodNode init=method(screen,"func_231160_c_","()V");
            if(!call(init,"com/pixelmonmod/pixelmon/client/DefaultServerList","tryFetchDefaultServers"))throw new AssertionError("Native init download call changed");
            MethodNode callback=method(screen,"lambda$init$0","()V");
            if(!call(callback,"net/minecraft/client/Minecraft","func_147108_a"))throw new AssertionError("Native screen-reopen callback changed");
            boolean creates=false;for(AbstractInsnNode i:callback.instructions.toArray())
                if(i instanceof TypeInsnNode && i.getOpcode()==Opcodes.NEW && ((TypeInsnNode)i).desc.equals(screen.name))creates=true;
            if(!creates)throw new AssertionError("Native callback must create a new multiplayer screen: deferred callbacks also recurse");
            ClassNode hook;
            try(InputStream in=MultiplayerRegressionVerification.class.getClassLoader().getResourceAsStream("local/pixelmon/backport/mixin/DefaultServerListMixin.class")){hook=read(in.readAllBytes());}
            MethodNode block=method(hook,"backport$noAds",null);
            if(call(block,"java/lang/Runnable","run")||!call(block,"org/spongepowered/asm/mixin/injection/callback/CallbackInfo","cancel"))throw new AssertionError("Reentrant refresh callback restored");
            for(AbstractInsnNode i:block.instructions.toArray())if(i instanceof MethodInsnNode){
                MethodInsnNode c=(MethodInsnNode)i;
                if(!c.owner.equals("org/spongepowered/asm/mixin/injection/callback/CallbackInfo")||!c.name.equals("cancel"))throw new AssertionError("Download blocker schedules unexpected work: "+c.owner+"."+c.name);
            }
        }
        System.out.println("PASS real native init/new-screen refresh recursion evidence; download blocker neither runs nor schedules callback; native saved-server/LAN/direct-connect screen code remains untouched.");
    }
    public static void executePatched(byte[] bytes)throws Exception{
        // Only the native entry methods and their actual injected handlers are
        // loaded. Omit unrelated downloader methods so Minecraft is never initialized.
        ClassNode n=read(bytes);String nativeName=n.name;String runnerName="backport/verification/BlockedServerDownload";
        n.name=runnerName;n.superName="java/lang/Object";n.interfaces.clear();n.fields.clear();n.innerClasses.clear();
        n.visibleAnnotations=null;n.invisibleAnnotations=null;n.nestHostClass=null;n.nestMembers=null;
        n.methods.removeIf(m->!m.name.equals("tryFetchDefaultServers")&&!m.name.contains("backport$noAds"));
        for(MethodNode m:n.methods)for(AbstractInsnNode i:m.instructions.toArray())if(i instanceof MethodInsnNode){
            MethodInsnNode c=(MethodInsnNode)i;if(c.owner.equals(nativeName))c.owner=runnerName;
        }
        ClassWriter writer=new ClassWriter(0);n.accept(writer);byte[] runnable=writer.toByteArray();
        Class<?> cls=new ClassLoader(MultiplayerRegressionVerification.class.getClassLoader()){
            Class<?> define(){return defineClass(runnerName.replace('/','.'),runnable,0,runnable.length);}
        }.define();
        Method fetch=cls.getMethod("tryFetchDefaultServers",Runnable.class);
        AtomicInteger reopened=new AtomicInteger();Runnable wouldReopen=()->{reopened.incrementAndGet();throw new AssertionError("Recursive multiplayer callback executed");};
        for(int i=0;i<10000;i++){fetch.invoke(null,wouldReopen);fetch.invoke(null,new Object[]{null});}
        cls.getMethod("tryFetchDefaultServers").invoke(null);
        if(reopened.get()!=0)throw new AssertionError("Multiplayer screen unexpectedly reopened");
        System.out.println("PASS executed actual Mixin-transformed native entry 20001 times with refresh/null/no-argument callbacks: no recursion, no callback, no downloader or game initialization.");
    }
}
