package local.pixelmon.backport;
import java.util.*;
import com.pixelmonmod.pixelmon.api.pokemon.Element;
import com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType;
import com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.*;
import com.pixelmonmod.pixelmon.api.registries.PixelmonItems;
import com.pixelmonmod.pixelmon.items.*;
import com.pixelmonmod.pixelmon.items.heldItems.*;
import com.pixelmonmod.pixelmon.enums.BerryType;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.battles.status.StatusType;
import com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.basic.WeatherBall;
/** Read-only native type preview; never execute attack effects, post events or consume RNG/items. */
public final class BurstMovePreview {
    private static final Set<String> NORMALIZE_EXCEPT=Set.of("Hidden Power","Judgment","Multi-Attack","Natural Gift","Revelation Dance","Struggle","Techno Blast","Weather Ball");
    private static final Element[] HIDDEN={Element.FIGHTING,Element.FLYING,Element.POISON,Element.GROUND,Element.ROCK,Element.BUG,Element.GHOST,Element.STEEL,Element.FIRE,Element.WATER,Element.GRASS,Element.ELECTRIC,Element.PSYCHIC,Element.ICE,Element.DRAGON,Element.DARK};
    private BurstMovePreview(){}
    public static Element hidden(int hp,int attack,int defense,int speed,int specialAttack,int specialDefense){return HIDDEN[((hp&1)+2*(attack&1)+4*(defense&1)+8*(speed&1)+16*(specialAttack&1)+32*(specialDefense&1))*15/63];}
    public static Element teraBlast(Element nativeType,boolean active,Element teraType){return active?teraType:nativeType;}
    public static List<String> electricMoves(PixelmonWrapper pokemon){
        List<String> result=new ArrayList<>();for(Attack move:pokemon.getMoveset())if(move!=null && type(pokemon,move)==Element.ELECTRIC)result.add(move.getMove().getAttackName());return result;
    }
    public static Element type(PixelmonWrapper user,Attack original){
        Attack copy=original.copy();String name=copy.getMove().getAttackName();Element type=copy.getMove().getAttackType();
        var held=user.getHeldItem();
        if(name.equals("Hidden Power")){
            var iv=user.getStats().getIVs();type=hidden(iv.getStat(BattleStatsType.HP),iv.getStat(BattleStatsType.ATTACK),iv.getStat(BattleStatsType.DEFENSE),iv.getStat(BattleStatsType.SPEED),iv.getStat(BattleStatsType.SPECIAL_ATTACK),iv.getStat(BattleStatsType.SPECIAL_DEFENSE));
        }else if(name.equals("Judgment") || name.equals("Revelation Dance")){if(!user.type.isEmpty())type=user.type.get(0);}
        else if(name.equals("Multi-Attack") && held instanceof MemoryItem)type=((MemoryItem)held).type;
        else if(name.equals("Techno Blast"))type=held==PixelmonItems.shock_drive?Element.ELECTRIC:held==PixelmonItems.burn_drive?Element.FIRE:held==PixelmonItems.douse_drive?Element.WATER:held==PixelmonItems.chill_drive?Element.ICE:Element.NORMAL;
        else if(name.equals("Weather Ball"))type=new WeatherBall().getOverrideType(user);
        else if(name.equals("Aura Wheel"))type=user.getForm().isForm("hangry")?Element.DARK:Element.ELECTRIC;
        else if(name.equals("Natural Gift")){
            if(user.hasHeldItem() && HeldItem.canEatBerry(user)){
                if(held instanceof BerryTypeReducingItem)type=((BerryTypeReducingItem)held).typeReduced;
                else if(held instanceof BerryStatusItem && ((BerryStatusItem)held).getBerry()==BerryType.PECHA)type=Element.ELECTRIC;
            }
        }else if(name.equals("Terrain Pulse") && BurstTerrain.current(user.bc)!=null && BurstTerrain.current(user.bc).benefitsGrounded(user))type=Element.ELECTRIC;
        else if(name.equals("Tera Blast"))type=teraBlast(type,Tera.active(user),Tera.active(user)?Tera.state(user).type:type);
        var ability=user.getBattleAbility();copy.overrideType(type);
        // Ate.modifyType itself only reads type/identity. Power callbacks change state and are never called here.
        if(ability instanceof Ate){Element converted=ability.modifyType(user,copy);if(converted!=null)type=converted;}
        if(ability instanceof Normalize && !NORMALIZE_EXCEPT.contains(name))type=Element.NORMAL;
        if(ability instanceof LiquidVoice && copy.isSoundBased())type=Element.WATER;
        if(user.hasStatus(StatusType.Electrify) || type==Element.NORMAL && user.bc.globalStatusController.hasStatus(StatusType.IonDeluge))type=Element.ELECTRIC;
        return type;
    }
}
