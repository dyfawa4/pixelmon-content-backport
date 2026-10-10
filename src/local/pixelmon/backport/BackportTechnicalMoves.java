package local.pixelmon.backport;
import com.pixelmonmod.pixelmon.enums.technicalmoves.ITechnicalMove;
import com.pixelmonmod.pixelmon.enums.technicalmoves.Gen9TechnicalMachines;
import com.pixelmonmod.pixelmon.enums.TMType;
import com.pixelmonmod.pixelmon.api.pokemon.Element;
import java.util.*;

/** Missing Gen 9 TM definitions. Native TM9 item/NBT ids are retained. */
public enum BackportTechnicalMoves implements ITechnicalMove {
    TemperFlare(207,"Temper Flare"),SupercellSlam(210,"Supercell Slam"),
    HardPress(225,"Hard Press"),DragonCheer(226,"Dragon Cheer"),
    AlluringVoice(227,"Alluring Voice"),PsychicNoise(228,"Psychic Noise"),UpperHand(229,"Upper Hand");
    private final int id;private final String attack;
    BackportTechnicalMoves(int id,String attack){this.id=id;this.attack=attack;}
    public String prefix(){return "tm_gen9";}
    public int getId(){return id;}
    public String getAttackName(){return attack;}
    public int getGeneration(){return 9;}
    public Map<Element,List<ITechnicalMove>> getTypeMap(){
        Map<Element,List<ITechnicalMove>> types=Gen9TechnicalMachines.TakeDown.getTypeMap();
        for(BackportTechnicalMoves move:values())if(move.getAttack()!=null){
            List<ITechnicalMove> group=types.computeIfAbsent(move.getAttack().getAttackType(),k->new ArrayList<>());
            if(group.stream().noneMatch(m->m.getId()==move.id))group.add(move);
        }
        return types;
    }
    public static BackportTechnicalMoves byId(int id){for(BackportTechnicalMoves move:values())if(move.id==id)return move;return null;}
    public static BackportTechnicalMoves byName(String name){for(BackportTechnicalMoves move:values())if(move.attack.equalsIgnoreCase(name))return move;return null;}
    public static ITechnicalMove resolve(TMType type,int id){ITechnicalMove nativeMove=ITechnicalMove.getMoveFor(type,id);return nativeMove!=null?nativeMove:type==TMType.TM9?byId(id):null;}
    public static ITechnicalMove resolve(TMType type,String name){ITechnicalMove nativeMove=ITechnicalMove.getMoveFor(type,name);return nativeMove!=null?nativeMove:type==TMType.TM9?byName(name):null;}
    public static ITechnicalMove[] all(TMType type,ITechnicalMove[] original){
        if(type!=TMType.TM9)return original;
        List<ITechnicalMove> list=new ArrayList<>(Arrays.asList(original));
        for(BackportTechnicalMoves move:values())if(list.stream().noneMatch(m->m.getId()==move.id))list.add(move);
        return list.toArray(new ITechnicalMove[0]);
    }
}
