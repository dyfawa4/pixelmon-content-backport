import local.pixelmon.backport.*;
import com.pixelmonmod.pixelmon.api.pokemon.Element;
import com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType;
import com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.basic.HiddenPower;
import net.minecraft.network.PacketBuffer;
import io.netty.buffer.Unpooled;
import org.objectweb.asm.tree.*;
import java.util.*;
public final class BurstDynamicPreviewVerification {
    static void check(boolean ok,String s){if(!ok)throw new AssertionError(s);}
    public static void main(String[] args)throws Exception{
        for(Element type:Tera.TYPES){
            check(BurstMovePreview.teraBlast(Element.NORMAL,true,type)==type,"Tera Blast ignores actual active tera type "+type);
            check(BurstMovePreview.teraBlast(Element.NORMAL,false,type)==Element.NORMAL,"Inactive Tera Blast uses stored tera type");
            boolean electric=BurstMovePreview.teraBlast(Element.NORMAL,true,type)==Element.ELECTRIC;
            check(BurstDisplayPolicy.move(BurstDisplayPolicy.FIELD|BurstDisplayPolicy.GROUNDED,"Tera Blast",electric,true)==(type==Element.ELECTRIC),"Non-electric Tera Blast incorrectly highlighted "+type);
        }
        for(Element expected:new Element[]{Element.FIGHTING,Element.FLYING,Element.POISON,Element.GROUND,Element.ROCK,Element.BUG,Element.GHOST,Element.STEEL,Element.FIRE,Element.WATER,Element.GRASS,Element.ELECTRIC,Element.PSYCHIC,Element.ICE,Element.DRAGON,Element.DARK}){
            var iv=HiddenPower.getOptimalIVs(expected);
            check(BurstMovePreview.hidden(iv.getStat(BattleStatsType.HP),iv.getStat(BattleStatsType.ATTACK),iv.getStat(BattleStatsType.DEFENSE),iv.getStat(BattleStatsType.SPEED),iv.getStat(BattleStatsType.SPECIAL_ATTACK),iv.getStat(BattleStatsType.SPECIAL_DEFENSE))==expected,"Native hidden-power optimal IV preset disagrees: "+expected);
        }
        UUID own=UUID.randomUUID(),other=UUID.randomUUID();var state=new TeraNetwork.Snapshot(42);state.defense.put(own,List.of(Element.ELECTRIC));state.defense.put(other,List.of(Element.WATER));state.burstElectricMoves.put(own,List.of("Hidden Power","Thunderbolt"));
        PacketBuffer buffer=new PacketBuffer(Unpooled.buffer());TeraNetwork.Snapshot.encode(state,buffer);var received=TeraNetwork.Snapshot.decode(buffer);buffer.release();
        check(received.burstElectricMoves.equals(state.burstElectricMoves) && !received.burstElectricMoves.containsKey(other),"Preview packet lost own moves or invented undisclosed moves");
        state.burstElectricMoves.put(own,Collections.nCopies(33,"Thunderbolt"));buffer=new PacketBuffer(Unpooled.buffer());TeraNetwork.Snapshot.encode(state,buffer);boolean rejected=false;try{TeraNetwork.Snapshot.decode(buffer);}catch(IllegalArgumentException e){rejected=true;}finally{buffer.release();}check(rejected,"Oversized preview packet accepted");
        for(MethodNode m:Version13StaticVerification.node("local/pixelmon/backport/BurstMovePreview").methods)for(var i:m.instructions)if(i instanceof MethodInsnNode){var c=(MethodInsnNode)i;check(!c.name.equals("applyEffectStart") && !c.name.equals("modifyPowerAndAccuracyUser") && !c.name.equals("post") && !(c.owner.equals("java/util/Random") && c.name.startsWith("next")) && !c.name.equals("setHeldItem"),"UI preview executes battle/RNG/item mutation: "+c.owner+"."+c.name);}
        check(Version13StaticVerification.node("local/pixelmon/backport/TeraNetwork$Snapshot").methods.stream().anyMatch(m->m.name.equals("forPlayer")),"Missing recipient privacy filter");
        System.out.println("PASS all 16 actual native HiddenPower IV presets, bounded preview packet roundtrip and absent-private-move sentinel, per-player routing and read-only dynamic type resolution; no game initialized.");
    }
}
