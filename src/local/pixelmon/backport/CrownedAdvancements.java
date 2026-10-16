package local.pixelmon.backport;

import java.util.*;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.registries.PixelmonItems;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import net.minecraft.entity.player.ServerPlayerEntity;

/** Native initBattle changes held-item forms for reserves too; only actual actors count. */
public final class CrownedAdvancements {
    // Values never retain a wrapper/controller, so abandoned battles can be collected.
    private static final Map<PixelmonWrapper,Integer> PENDING=new WeakHashMap<>();
    private CrownedAdvancements(){}
    private static boolean correct(Pokemon pokemon,int dex){
        return pokemon!=null && pokemon.getSpecies().getDex()==dex && (dex==888 || dex==889)
            && pokemon.getForm()!=null && "crowned".equals(pokemon.getForm().getName())
            && pokemon.getForm().getParentSpecies()!=null && pokemon.getForm().getParentSpecies().getDex()==dex
            && pokemon.getHeldItem().func_77973_b()==(dex==888?PixelmonItems.rusted_sword:PixelmonItems.rusted_shield);
    }
    private static ServerPlayerEntity player(PixelmonWrapper actor,int dex){
        if(actor==null || actor.bc==null || actor.bc.simulateMode || actor.bc.battleEnded || !correct(actor.pokemon,dex)
            || !(actor.getParticipant() instanceof PlayerParticipant))return null;
        var participant=(PlayerParticipant)actor.getParticipant();
        if(!actor.bc.participants.contains(participant) || !Arrays.asList(participant.allPokemon).contains(actor))return null;
        return AcquisitionAdvancements.ownedStored(participant.player,actor.pokemon)?participant.player:null;
    }
    private static void award(ServerPlayerEntity player,int dex){
        LegendAdvancements.award(player,"root");LegendAdvancements.award(player,"forms");
        LegendAdvancements.award(player,dex==888?"form_888_crowned":"form_889_crowned");
    }
    /** Called immediately after the native held-item method, without altering it. */
    public static void changed(PixelmonWrapper actor,String before){
        if(actor==null || actor.pokemon==null || "crowned".equals(before))return;
        int dex=actor.pokemon.getSpecies().getDex();
        if(player(actor,dex)==null)return;
        PENDING.put(actor,dex);complete(actor,dex);
    }
    private static void complete(PixelmonWrapper actor,int dex){
        var player=player(actor,dex);
        if(player==null){PENDING.remove(actor);return;}
        if(actor.onBattlefield && actor.isAlive() && actor.getParticipant().controlledPokemon.contains(actor)){
            PENDING.remove(actor);award(player,dex);
        }
    }
    public static void tick(){for(var row:new HashMap<>(PENDING).entrySet())complete(row.getKey(),row.getValue());}
    public static void clear(){PENDING.clear();}
    public static void field(Pokemon pokemon){
        int dex=pokemon.getSpecies().getDex();
        if(!correct(pokemon,dex) || !FieldMega.active(pokemon)
            || !"crowned".equals(pokemon.getPersistentData().func_74775_l(FieldMega.MARKER).func_74779_i("Target")))return;
        var player=pokemon.getOwnerPlayer();if(AcquisitionAdvancements.ownedStored(player,pokemon))award(player,dex);
    }
}
