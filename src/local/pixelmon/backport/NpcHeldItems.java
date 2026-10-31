package local.pixelmon.backport;

import java.util.*;
import com.pixelmonmod.pixelmon.api.battles.AttackCategory;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.pokemon.Element;
import com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.items.heldItems.*;
import net.minecraft.item.*;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

/** Choose equipment from the existing moves/ability; never rewrite the build to fit an item. */
public final class NpcHeldItems {
    private NpcHeldItems(){}
    private static final List<String> CANDIDATES=List.of("leftovers","black_sludge","life_orb","assault_vest",
        "choice_band","choice_specs","choice_scarf","focus_sash","rocky_helmet","eviolite","expert_belt",
        "flame_orb","toxic_orb","white_herb","power_herb","light_clay","chesto_berry","lum_berry",
        "light_ball","thick_club","soul_dew","booster_energy","ability_shield","heavy_duty_boots");
    public static boolean reserved(Pokemon p){
        Item item=p.getHeldItem().func_77973_b();String id=id(item);int dex=p.getSpecies().getDex();
        if(item instanceof MegaStoneItem&&PixelmonWrapper.canMegaEvolve(p.getHeldItem(),p.getSpecies(),p.getForm().getName()))return true;
        if(item instanceof ZCrystalItem)return dex==CapSpecies.DEX&&id.equals("pikashunium_z")
            ||p.getMoveset().stream().anyMatch(a->a.getAttackCategory()!=AttackCategory.STATUS&&a.getMove().hasZMove(p));
        if(item instanceof ReferenceItems.Meteorite||item instanceof Backport.Mask)return true;
        return Set.of("red_orb","blue_orb","rusted_sword","rusted_shield","griseous_orb","griseous_core","adamant_crystal","lustrous_globe","legend_plate").contains(id)
            ||dex==493&&id.endsWith("_plate")||dex==773&&id.endsWith("_memory")||dex==649&&id.endsWith("_drive");
    }
    private static String id(Item item){return item.getRegistryName()==null?"":item.getRegistryName().func_110623_a();}
    public static void equip(Pokemon p){
        if(reserved(p))return;
        Profile build=new Profile(p);Item best=p.getHeldItem().func_190926_b()?null:p.getHeldItem().func_77973_b();
        double score=best==null?-1:build.score(id(best));
        for(String name:CANDIDATES){
            Item item=name.equals("ability_shield")?Backport.item(name):ForgeRegistries.ITEMS.getValue(new ResourceLocation("pixelmon",name));
            if(item==null||item==Items.field_190931_a)continue;
            double value=build.score(name);if(value>score){score=value;best=item;}
        }
        if(best!=null&&(p.getHeldItem().func_190926_b()||p.getHeldItem().func_77973_b()!=best))p.setHeldItem(new ItemStack(best));
    }
    /** Resolve duplicates only; keep the original on the member with the greatest need. */
    public static void equipUnique(List<Pokemon> team){
        Map<Item,List<Pokemon>> groups=new LinkedHashMap<>();
        for(var p:team)if(!p.getHeldItem().func_190926_b())groups.computeIfAbsent(p.getHeldItem().func_77973_b(),k->new ArrayList<>()).add(p);
        Set<Item> used=new HashSet<>(groups.keySet());
        for(var entry:groups.entrySet())if(entry.getValue().size()>1){
            List<Pokemon> members=new ArrayList<>(entry.getValue());
            members.sort(Comparator.comparingDouble((Pokemon p)->retention(p,used)).reversed());
            for(int i=1;i<members.size();i++){
                Pokemon p=members.get(i);Item next=alternative(p,used);
                p.setHeldItem(next==null?ItemStack.field_190927_a:new ItemStack(next));if(next!=null)used.add(next);
            }
        }
    }
    private static double retention(Pokemon p,Set<Item> used){
        Profile profile=new Profile(p);Item next=alternative(p,used);
        boolean supportCombo=AuthorSupportPlan.support(p)&&id(p.getHeldItem().func_77973_b()).equals("power_herb")&&profile.has("Sky Attack")&&profile.has("Snatch");
        return (supportCombo?20000:reserved(p)?10000:profile.score(id(p.getHeldItem().func_77973_b())))-(next==null?-1:alternativeScore(profile,id(next)));
    }
    private static Item alternative(Pokemon p,Set<Item> used){
        Profile profile=new Profile(p);Item best=null;double score=-1;
        List<String> candidates=new ArrayList<>(CANDIDATES);candidates.add("sitrus_berry");
        for(String name:candidates){
            Item item=name.equals("ability_shield")?Backport.item(name):ForgeRegistries.ITEMS.getValue(new ResourceLocation("pixelmon",name));
            if(item==null||item==Items.field_190931_a||used.contains(item))continue;
            double value=alternativeScore(profile,name);if(value>score){score=value;best=item;}
        }
        return best;
    }
    private static double alternativeScore(Profile profile,String name){
        if(name.equals("sitrus_berry"))return 40;
        if(name.equals("lum_berry"))return Math.max(30,profile.score(name));
        return profile.score(name);
    }
    private static final class Profile {
        final Pokemon p;final Set<String> moves=new HashSet<>();final int dex,hp,def,spdef,speed,atk,spa;
        final String ability;final boolean poison,fire,steel,recovery;int physical,special,status;
        Profile(Pokemon p){
            this.p=p;dex=p.getSpecies().getDex();var stats=p.getForm().getBattleStats();
            hp=stats.getStat(BattleStatsType.HP);def=stats.getStat(BattleStatsType.DEFENSE);spdef=stats.getStat(BattleStatsType.SPECIAL_DEFENSE);
            speed=stats.getStat(BattleStatsType.SPEED);atk=stats.getStat(BattleStatsType.ATTACK);spa=stats.getStat(BattleStatsType.SPECIAL_ATTACK);
            var types=p.getForm().getTypes();poison=types.contains(Element.POISON);fire=types.contains(Element.FIRE);steel=types.contains(Element.STEEL);
            ability=p.getAbility()==null?"":p.getAbility().getClass().getSimpleName();boolean heal=false;
            for(var move:p.getMoveset()){
                moves.add(move.getMove().getAttackName());heal|=NpcMoves.recovery(move)||move.getMove().getAttackName().equals("Rest");
                switch(move.getAttackCategory()){case PHYSICAL:physical++;break;case SPECIAL:special++;break;default:status++;}
            }
            recovery=heal;
        }
        boolean has(String...names){return Arrays.stream(names).anyMatch(moves::contains);}
        double score(String item){
            int damage=physical+special,bulk=hp+def+spdef;
            switch(item){
                case "ability_shield":return Set.of("WonderGuard","Unaware","MagicBounce").contains(ability)?112:-1;
                case "heavy_duty_boots":return p.getPersistentData().func_74767_n("BackportScoutHazards")&&!ability.equals("MagicGuard")?95+com.pixelmonmod.pixelmon.api.pokemon.Element.getTotalEffectiveness(p.getForm().getTypes(),Element.ROCK)*12+(recovery?20:0):-1;
                case "leftovers":return 50+status*12+(bulk>=260?15:0)+(recovery?5:0);
                case "black_sludge":return poison?score("leftovers")+2:-1;
                case "life_orb":return damage==0?-1:ability.equals("MagicGuard")?180:55+damage*5+Math.max(atk,spa)/10-status*3;
                case "assault_vest":return status>0||damage==0?-1:speed<80?108:65;
                case "choice_band":return status==0&&physical>=3&&atk>=spa?100+atk/20:-1;
                case "choice_specs":return status==0&&special>=3&&spa>=atk?100+spa/20:-1;
                case "choice_scarf":return status==0&&damage>=3&&speed>=60&&speed<100&&!has("Trick Room")?112:-1;
                case "focus_sash":return damage>0&&speed>=90&&bulk<230&&!recovery&&!ability.equals("Sturdy")?115:-1;
                case "rocky_helmet":return def>=110&&status>=2?110:-1;
                case "eviolite":return !p.getForm().getEvolutions().isEmpty()?165:-1;
                case "expert_belt":return damage>=3?65+damage*3:-1;
                case "flame_orb":return !fire&&(ability.equals("Guts")&&physical>0||ability.equals("FlareBoost")&&special>0)?230:-1;
                case "toxic_orb":return ability.equals("PoisonHeal")?250:!poison&&!steel&&fire&&ability.equals("Guts")&&physical>0?230:-1;
                case "white_herb":return has("Shell Smash")?210:-1;
                case "power_herb":return has("Geomancy")?240:has("Meteor Beam","Sky Attack")?125:
                    has("Solar Beam","Solar Blade")&&!ability.equals("Drought")&&!ability.equals("DesolateLand")?125:-1;
                case "light_clay":return has("Reflect","Light Screen","Aurora Veil")?190:-1;
                case "chesto_berry":return has("Rest")&&!has("Sleep Talk")?145:-1;
                case "lum_berry":return has("Outrage","Petal Dance")?95:-1;
                case "light_ball":return dex==25&&damage>0?250:-1;
                case "thick_club":return (dex==104||dex==105)&&physical>0?250:-1;
                case "soul_dew":return (dex==380||dex==381)&&special>0?135:-1;
                case "booster_energy":return ability.equals("Protosynthesis")||ability.equals("QuarkDrive")?165:-1;
                default:return -1;
            }
        }
    }
}
