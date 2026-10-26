package local.pixelmon.backport;
import java.util.*;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.pokemon.species.*;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType;
/** The two original author legendary slots use powerful mature forms with complementary roles. */
public final class AuthorCorePool {
    public static final class Core {public final int dex;public final String form,item;Core(int d,String f,String i){dex=d;form=f;item=i;}}
    private static final List<Core> POOL=List.of(new Core(383,"primal","pixelmon:red_orb"),new Core(382,"primal","pixelmon:blue_orb"),new Core(493,"","pixelmon:legend_plate"),new Core(888,"crowned","pixelmon:rusted_sword"),new Core(898,"shadowrider",null),new Core(898,"icerider",null),new Core(716,"",null),new Core(717,"",null),new Core(250,"",null),new Core(384,"",null),new Core(1007,"",null),new Core(1008,"",null),new Core(800,"dusk",null));
    public static Core choose(List<Pokemon> existing,Random random){Core best=null;double score=-1e9;
        for(Core core:POOL){Species s=PixelmonSpecies.fromNationalDex(core.dex);if(s==null||existing.stream().anyMatch(p->p.getSpecies().getDex()==core.dex))continue;Stats form=core.form.isEmpty()?s.getDefaultForm():s.getForms().stream().filter(f->f.isForm(core.form)).findFirst().orElse(null);if(form==null)continue;
            int total=0;for(var stat:BattleStatModifiers.STATS)total+=form.getBattleStats().getStat(stat);if(total<650||form.getMoves().getAllMoves().stream().filter(a->a.getAttackCategory()!=com.pixelmonmod.pixelmon.api.battles.AttackCategory.STATUS&&a.getBasePower()>0).count()<2)continue;
            double value=(total-600)*.35+random.nextDouble()*65;
            for(var old:existing){if((old.getSpecies().getDex()==382&&core.dex==383)||(old.getSpecies().getDex()==383&&core.dex==382))value-=160;
                boolean a=old.getForm().getBattleStats().getStat(BattleStatsType.ATTACK)>old.getForm().getBattleStats().getStat(BattleStatsType.SPECIAL_ATTACK),b=form.getBattleStats().getStat(BattleStatsType.ATTACK)>form.getBattleStats().getStat(BattleStatsType.SPECIAL_ATTACK);if(a!=b)value+=25;
                for(Element type:Tera.TYPES)if(type!=Element.MYSTERY){double x=Element.getTotalEffectiveness(old.getForm().getTypes(),type),y=Element.getTotalEffectiveness(form.getTypes(),type);if(x>1&&y>1)value-=8;if(x>1&&y<1)value+=4;}}
            if(value>score){score=value;best=core;}
        }if(best==null)throw new IllegalStateException("No powerful author legendary core");return best;
    }
}
