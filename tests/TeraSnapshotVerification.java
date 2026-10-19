import java.util.*;
import java.util.jar.JarFile;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketBuffer;
import com.pixelmonmod.pixelmon.api.pokemon.Element;
import local.pixelmon.backport.TeraNetwork;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Native PacketBuffer round trips for the null type slots used by Protean/Libero. */
public final class TeraSnapshotVerification {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static void roundtrip(List<Element> input,List<Element> expected){
        UUID id=UUID.randomUUID();var state=new TeraNetwork.Snapshot(160);state.teraRaid=true;state.defense.put(id,input);state.active.put(id,1);state.attackStats.put(id,3);state.burst.put(id,31);state.burstBalls.put(id,Arrays.asList("poke_ball","origin_ball"));state.burstElectricMoves.put(id,Arrays.asList("Thunderbolt"));
        var buffer=new PacketBuffer(Unpooled.buffer());
        try{
            TeraNetwork.Snapshot.encode(state,buffer);var copy=TeraNetwork.Snapshot.decode(buffer);
            check(copy.defense.get(id).equals(expected),"Defense types changed: "+input);
            check(buffer.readableBytes()==0,"Wrong type count shifted the rest of the packet");
            check(copy.battle==160&&copy.teraRaid&&copy.active.equals(state.active)&&copy.attackStats.equals(state.attackStats)&&copy.burst.equals(state.burst)&&copy.burstBalls.equals(state.burstBalls)&&copy.burstElectricMoves.equals(state.burstElectricMoves),"Other battle/raid metadata changed");
            check(Objects.equals(state.defense.get(id),input),"Serialization modified native type slots");
        }finally{buffer.release();}
    }
    public static void main(String[] args)throws Exception{
        try(var jar=new JarFile(args[0])){
            for(String ability:new String[]{"Protean","Libero"}){
                var nativeAbility=new ClassNode();new ClassReader(jar.getInputStream(jar.getJarEntry("com/pixelmonmod/pixelmon/api/pokemon/ability/abilities/"+ability+".class"))).accept(nativeAbility,0);
                boolean nullSlot=false;for(var method:nativeAbility.methods)if(method.name.equals("startMove"))for(var instruction:method.instructions)if(instruction.getOpcode()==Opcodes.ACONST_NULL)nullSlot=true;
                check(nullSlot,"Native null-slot reproducer changed: "+ability);
            }
        }
        roundtrip(Arrays.asList(Element.FIRE,null),Arrays.asList(Element.FIRE));
        roundtrip(Arrays.asList(null,Element.WATER),Arrays.asList(Element.WATER));
        roundtrip(Arrays.asList(Element.FIRE,Element.FLYING),Arrays.asList(Element.FIRE,Element.FLYING));
        roundtrip(Arrays.asList(Element.FIRE,null,Element.FLYING),Arrays.asList(Element.FIRE,Element.FLYING));
        roundtrip(Arrays.asList(null,null),Collections.emptyList());roundtrip(Collections.emptyList(),Collections.emptyList());roundtrip(null,Collections.emptyList());
        System.out.println("PASS native Libero/Protean null-slot snapshots, normal dual types, typeless states, native PacketBuffer counts and all battle metadata retained.");
    }
}
