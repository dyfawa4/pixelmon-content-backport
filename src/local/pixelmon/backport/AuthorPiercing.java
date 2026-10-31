package local.pixelmon.backport;

import java.util.*;
import com.pixelmonmod.pixelmon.api.battles.AttackCategory;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import net.minecraft.util.ResourceLocation;

/** Rematch-only type effectiveness, shared by native damage and AI previews. */
public final class AuthorPiercing {
    private static final Map<BattleController,Set<BattleParticipant>> REMATCHES=new WeakHashMap<>();
    private static final ThreadLocal<Map<BattleParticipant,Integer>> FORECAST=ThreadLocal.withInitial(IdentityHashMap::new);
    private AuthorPiercing(){}
    public static void prepare(TrainerParticipant side){
        BattleController b=side.bc;
        if(b==null||b.simulateMode||b.isRaid()||!AuthorBossTeams.is(side.trainer))return;
        boolean earned=false;
        for(var other:b.participants)if(other.team!=side.team&&other instanceof PlayerParticipant){
            var player=((PlayerParticipant)other).player;
            var advancement=player.func_184102_h().func_191949_aK().func_192778_a(new ResourceLocation(Content.NS,"adventure/defeat_author"));
            if(advancement!=null&&player.func_192039_O().func_192747_a(advancement).func_192105_a())earned=true;
        }
        // Capture entry eligibility once: earning the first win cannot change
        // the difficulty of the battle that awarded it.
        if(earned)REMATCHES.computeIfAbsent(b,k->Collections.newSetFromMap(new IdentityHashMap<>())).add(side);
    }
    public static boolean eligible(PixelmonWrapper p){return p!=null&&p.bc!=null&&!p.bc.battleEnded&&REMATCHES.getOrDefault(p.bc,Collections.emptySet()).contains(p.getParticipant());}
    public static int alive(BattleParticipant side){return FORECAST.get().getOrDefault(side,(int)Arrays.stream(side.allPokemon).filter(PixelmonWrapper::isAlive).count());}
    public static boolean active(PixelmonWrapper p){return eligible(p)&&alive(p.getParticipant())>0&&alive(p.getParticipant())<=3;}
    public static boolean affects(PixelmonWrapper user,PixelmonWrapper target,Attack attack){return attack!=null&&attack.getAttackCategory()!=AttackCategory.STATUS&&target!=null&&user!=null&&user.bc==target.bc&&target.getParticipant() instanceof PlayerParticipant&&!user.isAlly(target)&&active(user);}
    public static double multiplier(double original){return Double.isFinite(original)?original>=4?4:3:original;}
    public static double effectiveness(PixelmonWrapper user,PixelmonWrapper target,Attack attack,double original){return affects(user,target,attack)?multiplier(original):original;}
    /** The revived member is scored in the phase that would actually follow revival. */
    public static double revivedValue(PixelmonWrapper member,PixelmonWrapper own){
        if(!eligible(own))return NpcMedicine.value(member,own);
        var side=own.getParticipant();var values=FORECAST.get();Integer old=values.put(side,alive(side)+1);
        try{return NpcMedicine.value(member,own);}finally{if(old==null)values.remove(side);else values.put(side,old);if(values.isEmpty())FORECAST.remove();}
    }
    /** Cost of turning off the three-survivor phase for the surviving team. */
    public static double revivalCost(PixelmonWrapper own){
        if(!active(own)||alive(own.getParticipant())!=3)return 0;
        var side=own.getParticipant();double before=0,after=0;
        for(var member:side.allPokemon)if(member.isAlive())before=Math.max(before,NpcMedicine.value(member,own));
        var values=FORECAST.get();Integer old=values.put(side,4);
        try{for(var member:side.allPokemon)if(member.isAlive())after=Math.max(after,NpcMedicine.value(member,own));}
        finally{if(old==null)values.remove(side);else values.put(side,old);if(values.isEmpty())FORECAST.remove();}
        return Math.max(0,before-after);
    }
    public static void end(BattleController battle){REMATCHES.remove(battle);}
}
