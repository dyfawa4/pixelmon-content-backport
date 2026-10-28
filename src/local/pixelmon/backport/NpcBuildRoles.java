package local.pixelmon.backport;
import java.util.*;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.api.battles.AttackCategory;
/** Legal, complementary offensive/support plans for the trainer's own legendary cores. */
public final class NpcBuildRoles {
    public static void configure(Pokemon p){
        String[] names=null;switch(p.getSpecies().getDex()){
            case 250:names=new String[]{"Sacred Fire","Brave Bird","Recover","Whirlwind"};break;
            case 249:names=new String[]{"Aeroblast","Psychic","Roost","Whirlwind"};break;
            case 487:names=new String[]{"Dragon Tail","Hex","Will-O-Wisp","Rest"};break;
            case 384:names=new String[]{"Dragon Ascent","Extreme Speed","Earthquake","Dragon Dance"};break;
            case 493:names=p.getForm().getTypes().contains(com.pixelmonmod.pixelmon.api.pokemon.Element.NORMAL)&&!p.getHeldItem().func_77973_b().getRegistryName().func_110623_a().equals("legend_plate")?new String[]{"Extreme Speed","Earthquake","Swords Dance","Recover"}:new String[]{"Judgment","Ice Beam","Calm Mind","Recover"};break;
            case 716:names=new String[]{"Moonblast","Thunder","Focus Blast","Geomancy"};break;
            case 717:names=new String[]{"Oblivion Wing","Dark Pulse","Sucker Punch","Taunt"};break;
            case 383:names=new String[]{"Precipice Blades","Fire Punch","Stone Edge","Swords Dance"};break;
            case 382:names=new String[]{"Origin Pulse","Ice Beam","Thunder","Calm Mind"};break;
            case 483:names=new String[]{"Draco Meteor","Flash Cannon","Stealth Rock","Roar"};break;
            case 484:names=new String[]{"Spacial Rend","Hydro Pump","Thunder","Fire Blast"};break;
            case 888:names=new String[]{"Behemoth Blade","Play Rough","Close Combat","Swords Dance"};break;
            case 889:names=new String[]{"Body Press","Crunch","Iron Defense","Rest"};break;
            case 898:if(p.getForm().isForm("shadowrider"))names=new String[]{"Astral Barrage","Psyshock","Draining Kiss","Nasty Plot"};else if(p.getForm().isForm("icerider"))names=new String[]{"Glacial Lance","High Horsepower","Swords Dance","Trick Room"};break;
            case 1007:names=new String[]{"Collision Course","Flare Blitz","Dragon Claw","Swords Dance"};break;
            case 1008:names=new String[]{"Electro Drift","Draco Meteor","Volt Switch","Calm Mind"};break;
            case 800:if(p.getForm().isForm("dusk"))names=new String[]{"Sunsteel Strike","Earthquake","Dragon Dance","Morning Sun"};break;
        }
        if(names==null)return;List<Attack> next=new ArrayList<>();for(String name:names)for(var legal:p.getForm().getMoves().getAllMoves())if(legal.getAttackName().equals(name)){next.add(new Attack(legal));break;}
        for(var old:p.getMoveset())if(next.size()<4&&next.stream().noneMatch(a->a.getMove().equals(old.getMove())))next.add(old.deepCopy());
        if(next.size()==4&&next.stream().filter(a->a.getAttackCategory()!=AttackCategory.STATUS).count()>=2){p.getMoveset().clear();p.getMoveset().addAll(next);}
    }
}
