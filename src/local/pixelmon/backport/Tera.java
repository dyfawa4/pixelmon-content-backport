package local.pixelmon.backport;

import java.util.*;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType;
import com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.Adaptability;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import com.pixelmonmod.pixelmon.battles.status.StatusType;

/** Battle-local Tera state, with only the selected type persisted in Pokemon
 * NBT. MYSTERY represents Stellar at this legacy API boundary, never a random
 * ordinary type. No shared ImmutableAttack or species data is mutated. */
public final class Tera {
    private Tera(){}
    public static final Element[] TYPES={Element.NORMAL,Element.FIRE,Element.WATER,Element.GRASS,
        Element.ELECTRIC,Element.ICE,Element.FIGHTING,Element.POISON,Element.GROUND,Element.FLYING,
        Element.PSYCHIC,Element.BUG,Element.ROCK,Element.GHOST,Element.DRAGON,Element.DARK,Element.STEEL,Element.FAIRY,Element.MYSTERY};
    public static final class State {
        public final List<Element> base;
        public final Element type;
        public final String form;
        public boolean active,selected;
        public final Set<Element> spent=EnumSet.noneOf(Element.class);
        public final Set<Element> pending=EnumSet.noneOf(Element.class);
        public State(PixelmonWrapper p){base=new ArrayList<>(p.pokemon.getForm().getTypes());type=type(p.pokemon);form=p.getForm().getName();}
        private State(State s){base=new ArrayList<>(s.base);type=s.type;form=s.form;active=s.active;selected=s.selected;spent.addAll(s.spent);}
    }
    private static final Map<PixelmonWrapper,State> STATES=Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<BattleParticipant,UUID> USED=Collections.synchronizedMap(new WeakHashMap<>());
    public static Element type(Pokemon p){
        int dex=p.getSpecies().getDex();
        if(dex==1024)return Element.MYSTERY;
        if(dex==1017){String f=p.getForm().getName();return f.contains("wellspring")?Element.WATER:f.contains("hearthflame")?Element.FIRE:f.contains("cornerstone")?Element.ROCK:Element.GRASS;}
        int saved=p.getPersistentData().func_74762_e("TeraType");
        if(saved>=1 && saved<=TYPES.length)return TYPES[saved-1];
        Element initial=p.getForm().getTypes().get(0);
        p.getPersistentData().func_74768_a("TeraType",Arrays.asList(TYPES).indexOf(initial)+1);
        return initial;
    }
    public static boolean changeType(Pokemon p,Element type){
        if(p.getSpecies().getDex()==1017 || p.getSpecies().getDex()==1024)return false;
        int index=Arrays.asList(TYPES).indexOf(type);if(index<0)return false;
        p.getPersistentData().func_74768_a("TeraType",index+1);return true;
    }
    public static State state(PixelmonWrapper p){return STATES.get(p);}
    public static boolean active(PixelmonWrapper p){State s=state(p);return s!=null && s.active;}
    public static boolean active(Pokemon p){synchronized(STATES){return STATES.entrySet().stream().anyMatch(e->e.getKey().pokemon==p && !e.getKey().bc.simulateMode && e.getValue().active);}}
    public static boolean selected(PixelmonWrapper p){State s=state(p);return s!=null && s.selected;}
    public static boolean used(BattleParticipant p){return USED.containsKey(p);}
    public static boolean eligible(PixelmonWrapper p){
        return p.bc!=null && !p.bc.battleEnded && TeraRaids.charged(p) && p.isAlive() && p.onBattlefield && !active(p)
            && !used(p.getParticipant()) && !p.isMega && p.isDynamax==0 && !p.usingZ
            && !p.getForm().getName().contains("mega") && !p.getForm().isForm("ultra")
            && p.getParticipant().evolution==null && p.getParticipant().dynamax==null && !p.getParticipant().usedZ;
    }
    public static boolean select(PixelmonWrapper p){
        if(selected(p)){state(p).selected=false;p.willEvolve=false;return true;}
        if(!eligible(p))return false;
        for(PixelmonWrapper member:p.getParticipant().allPokemon){State s=state(member);if(s!=null && s.selected){s.selected=false;member.willEvolve=false;}}
        State s=new State(p);s.selected=true;STATES.put(p,s);p.willEvolve=true;return true;
    }
    public static void cancelSelection(PixelmonWrapper p){
        State s=state(p);if(s!=null && s.selected){s.selected=false;p.willEvolve=false;}
    }
    public static boolean activate(PixelmonWrapper p){
        if(!selected(p) || !eligible(p))return false;
        if(!p.bc.simulateMode && p.getParticipant() instanceof PlayerParticipant && !TeraItems.hasOrb(((PlayerParticipant)p.getParticipant()).player)){
            state(p).selected=false;p.willEvolve=false;return false;
        }
        State s=state(p);s.selected=false;s.active=true;p.willEvolve=false;USED.put(p.getParticipant(),p.getPokemonUUID());
        if(p.getSpecies().getDex()==1024){
            float hp=p.getHealthPercent()/100f;boolean full=p.hasFullHealth();
            p.setForm("stellar");p.recalculateMaxHP();p.setHealth(full?p.getMaxHealth():Math.round(p.getMaxHealth()*hp));
            p.bc.modifyStats(p);p.updateBattleDamage(p.getHealth());
            // Teraform Zero happens on transformation, not every switch-in.
            com.pixelmonmod.pixelmon.battles.status.Weather weather=p.bc.globalStatusController.getWeatherIgnoreAbility();
            if(weather!=null)p.bc.globalStatusController.removeGlobalStatus(weather);
            com.pixelmonmod.pixelmon.battles.status.Terrain terrain=p.bc.globalStatusController.getTerrain();
            if(!(terrain instanceof com.pixelmonmod.pixelmon.battles.status.NoTerrain))p.bc.globalStatusController.removeGlobalStatus(terrain);
        }else if(p.getSpecies().getDex()==1017){
            p.setForm("tera"+s.form);
            aspect(p,true);
        }
        enforceType(p);
        TeraItems.consumeOrb(p);
        TeraNetwork.sync(p.bc,false);
        if(!p.bc.simulateMode)p.bc.sendToAll("pixelmon.backport.tera.activated",p.getNickname(),s.type==Element.MYSTERY?"星晶":s.type.getLocalizedName());
        return true;
    }
    public static void aspect(PixelmonWrapper p,boolean announce){
        if(!active(p) || p.getSpecies().getDex()!=1017)return;
        String f=p.getForm().getName();BattleStatsType stat=f.contains("wellspring")?BattleStatsType.SPECIAL_DEFENSE:f.contains("hearthflame")?BattleStatsType.ATTACK:f.contains("cornerstone")?BattleStatsType.DEFENSE:BattleStatsType.SPEED;
        p.getBattleStats().modifyStat(1,stat,p,announce);
    }
    public static void enforceType(PixelmonWrapper p){State s=state(p);if(s!=null && s.active)p.type=new ArrayList<>(s.type==Element.MYSTERY?s.base:Collections.singletonList(s.type));}
    public static void copy(PixelmonWrapper original,PixelmonWrapper copy){State s=state(original);if(s!=null)STATES.put(copy,new State(s));}
    public static double stab(PixelmonWrapper p,Element move,double nativeMultiplier){
        State s=state(p);if(s==null || !s.active)return nativeMultiplier;
        boolean original=s.base.contains(move),adapt=p.getBattleAbility() instanceof Adaptability;
        if(s.type==Element.MYSTERY){
            boolean boosted=!s.spent.contains(move) || p.getSpecies().getDex()==1024 || TeraRaids.get(p.bc)!=null;
            // Stellar bypasses Adaptability, matching the modern damage flow.
            if(original)return boosted?2:1.5;
            return boosted?1.2:1;
        }
        if(move==s.type)return original?(adapt?2.25:2):(adapt?2:1.5);
        return original?1.5:1;
    }
    public static void beginMove(PixelmonWrapper p){State s=state(p);if(s!=null)s.pending.clear();}
    public static void damageCalculated(PixelmonWrapper p,Attack attack){State s=state(p);if(s!=null && s.active && s.type==Element.MYSTERY && !p.bc.simulateMode && p.attack==attack && attack.getAttackCategory()!=com.pixelmonmod.pixelmon.api.battles.AttackCategory.STATUS && !attack.getMove().getAttackName().equals("Struggle"))s.pending.add(attack.getType());}
    public static void consumeBoost(PixelmonWrapper p){State s=state(p);if(s!=null){s.spent.addAll(s.pending);s.pending.clear();}
    }
    public static void restoreTerapagos(PixelmonWrapper p){
        if(p.getSpecies().getDex()!=1024 || p.getForm().isForm("normal"))return;
        int hp=p.getHealth(),max=p.getMaxHealth();
        p.setForm("normal");p.recalculateMaxHP();
        p.setHealth(hp==0?0:Math.max(1,(int)Math.round(p.getMaxHealth()*(double)hp/Math.max(1,max))));
    }
    public static void end(BattleController bc){
        TeraNetwork.sync(bc,true);
        for(BattleParticipant participant:bc.participants){
            USED.remove(participant);
            for(PixelmonWrapper p:participant.allPokemon){
                State s=STATES.remove(p);if(s==null)continue;
                if(s.active && (p.getSpecies().getDex()==1024 || p.getSpecies().getDex()==1017)){
                    if(p.getSpecies().getDex()==1024)restoreTerapagos(p);
                    else {p.setForm(s.form);p.recalculateMaxHP();if(p.getHealth()>p.getMaxHealth())p.setHealth(p.getMaxHealth());}
                }
                p.willEvolve=false;
            }
        }
    }
    public static int floorPower(PixelmonWrapper p,Attack a,int power){
        if(!active(p) || power<=0 || power>=60 || p.priority>0 || a.getMove().getPriority(p)>0)return power;
        State s=state(p);if(s.type==Element.MYSTERY){if(s.spent.contains(a.getType()) && p.getSpecies().getDex()!=1024)return power;}else if(s.type!=a.getType())return power;
        // The native effects supply variable power and multi-hit exclusions.
        String name=a.getMove().getAttackName();
        if(Arrays.asList("Water Spout","Eruption","Dragon Energy","Flail","Reversal","Gyro Ball","Electro Ball","Grass Knot","Low Kick","Stored Power","Power Trip","Crush Grip","Wring Out","Hard Press","Return","Frustration","Present","Magnitude","Fling","Natural Gift").contains(name))return power;
        if(a.getMove().effects.stream().anyMatch(e->e instanceof com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.attackModifiers.MultipleHit))return power;
        return 60;
    }
}
