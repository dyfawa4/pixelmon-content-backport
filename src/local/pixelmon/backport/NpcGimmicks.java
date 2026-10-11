package local.pixelmon.backport;
import java.util.*;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.registries.PixelmonItems;
import com.pixelmonmod.pixelmon.api.battles.AttackCategory;
import com.pixelmonmod.pixelmon.api.battles.BattleAIMode;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.controller.ai.MoveChoice;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import com.pixelmonmod.pixelmon.entities.npcs.NPCTrainer;
import com.pixelmonmod.pixelmon.enums.EnumMegaItemsUnlocked;
import com.pixelmonmod.pixelmon.items.heldItems.ZCrystalItem;
import net.minecraft.item.*;

/** Decisions layered on native AI; the native roster and equal-level scaling are retained. */
public final class NpcGimmicks {
    private NpcGimmicks(){}
    public enum Choice {NONE, TERA, MEGA, DYNAMAX, Z}
    public static final String PROFILE="BackportNpcProfile";
    private static final Map<PixelmonWrapper,Choice> PENDING=Collections.synchronizedMap(new WeakHashMap<>());
    public static boolean enabled(NPCTrainer npc){
        return !npc.getPersistentData().func_74767_n("BackportNpcDisable")
            && (npc.isGymLeader || "equal".equalsIgnoreCase(npc.getBossTier().getID()));
    }
    public static void prepare(TrainerParticipant participant){
        if(!enabled(participant.trainer) || participant.bc==null || participant.bc.simulateMode || participant.bc.isRaid())return;
        augment(participant);
        NpcMedicine.prepare(participant);
        participant.trainer.setMegaItem(EnumMegaItemsUnlocked.Both);
        if(participant.trainer.getBattleAIMode()==BattleAIMode.DEFAULT)participant.trainer.setBattleAIMode(BattleAIMode.ADVANCED);
        List<Pokemon> threats=new ArrayList<>();
        for(BattleParticipant other:participant.bc.participants)if(other.team!=participant.team){
            for(PixelmonWrapper p:other.allPokemon)if(p.onBattlefield && p.isAlive())threats.add(p.pokemon);
            if(threats.isEmpty() && other.allPokemon.length>0)threats.add(other.allPokemon[0].pokemon);
        }
        for(PixelmonWrapper wrapper:participant.allPokemon){
            profile(wrapper.pokemon,participant.trainer.func_110124_au(),threats);
            // Native battle cleanup restores this snapshot, including its held item.
            wrapper.initialCopyOfPokemon.setHeldItem(wrapper.pokemon.getHeldItem().func_77946_l());
        }
    }
    public static void augment(TrainerParticipant participant){
        NPCTrainer npc=participant.trainer;
        if(npc.getPersistentData().func_74767_n("BackportNpcRoster"))return;
        Random random=new Random(npc.func_110124_au().getLeastSignificantBits());
        Element theme=participant.allPokemon.length==0?Element.NORMAL:participant.allPokemon[0].pokemon.getForm().getTypes().get(0);
        boolean existing=Arrays.stream(participant.allPokemon).anyMatch(p->com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies.isLegendary(p.getSpecies()));
        // Most, but not all, enhanced trainers receive one ace. Never fill a team with legends.
        if(!existing && participant.allPokemon.length>0 && (theme==Element.POISON || random.nextInt(3)!=0)){
            List<com.pixelmonmod.pixelmon.api.pokemon.species.Species> candidates=new ArrayList<>();
            List<com.pixelmonmod.pixelmon.api.pokemon.species.Species> all=new ArrayList<>();
            for(int dex:com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies.getLegendaries()){
                com.pixelmonmod.pixelmon.api.pokemon.species.Species species=com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies.fromNationalDex(dex);
                if(species!=null){all.add(species);if(species.getDefaultForm().getTypes().contains(theme))candidates.add(species);}
            }
            if(candidates.isEmpty() || random.nextInt(5)==0)candidates=all;
            candidates.sort(Comparator.comparingInt(s->s.getDex()));
            if(!candidates.isEmpty()){
                int index=participant.allPokemon.length<6?participant.allPokemon.length:participant.allPokemon.length-1;
                com.pixelmonmod.pixelmon.api.pokemon.species.Species selected=theme==Element.POISON && random.nextDouble()<.70
                    ?com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies.fromNationalDex(890):candidates.get(random.nextInt(candidates.size()));
                Pokemon ace=PokemonFactory.create(selected);
                ace.setLevelNum(participant.allPokemon[0].pokemon.getPokemonLevel());ace.initialize();ace.rerollMoveset();ace.heal();
                if(theme==Element.POISON && ace.getSpecies().getDex()==890)ace.setHeldItem(new ItemStack(Backport.REGISTERED.get("eternatus_meteorite").get()));
                npc.getPokemonStorage().set(index,ace);
                participant.allPokemon=Arrays.copyOf(participant.allPokemon,Math.max(participant.allPokemon.length,index+1));
                participant.allPokemon[index]=new PixelmonWrapper(participant,ace,index);
            }
        }
        npc.getPersistentData().func_74757_a("BackportNpcRoster",true);
    }
    public static void profile(Pokemon pokemon,UUID trainer,List<Pokemon> threats){
        if(pokemon.getPersistentData().func_74767_n(PROFILE))return;
        Random random=new Random(trainer.getLeastSignificantBits() ^ pokemon.getUUID().getMostSignificantBits());
        int saved=pokemon.getPersistentData().func_74762_e("TeraType");Element original=pokemon.getForm().getTypes().get(0);
        // Keep explicitly configured non-default Tera types, locked forms, and existing held items.
        if(saved==0 || saved==Arrays.asList(Tera.TYPES).indexOf(original)+1)Tera.changeType(pokemon,defensiveType(pokemon,threats,random));
        if(pokemon.getSpecies().getDex()==890 && (pokemon.getForm().isForm("eternamax") || (pokemon.getHeldItem().func_190926_b() && random.nextBoolean())))
            pokemon.setHeldItem(new ItemStack(Backport.REGISTERED.get("eternatus_meteorite").get()));
        if(pokemon.getHeldItem().func_190926_b()){
            List<Item> mega=new ArrayList<>();
            for(Item item:PixelmonItems.megaStones)if(PixelmonWrapper.canMegaEvolve(new ItemStack(item),pokemon.getSpecies(),pokemon.getForm().getName()))mega.add(item);
            if(!mega.isEmpty() && random.nextInt(3)!=0)pokemon.setHeldItem(new ItemStack(mega.get(random.nextInt(mega.size()))));
            else if(random.nextBoolean()){
                List<Item> crystals=new ArrayList<>();
                for(Item item:net.minecraftforge.registries.ForgeRegistries.ITEMS)if(item instanceof ZCrystalItem){
                    pokemon.setHeldItem(new ItemStack(item));
                    boolean compatible=pokemon.getMoveset().stream().anyMatch(a->a.getAttackCategory()!=AttackCategory.STATUS && a.getMove().hasZMove(pokemon));
                    pokemon.setHeldItem(ItemStack.field_190927_a);if(compatible)crystals.add(item);
                }
                if(!crystals.isEmpty())pokemon.setHeldItem(new ItemStack(crystals.get(random.nextInt(crystals.size()))));
            }
            if(pokemon.getHeldItem().func_190926_b())pokemon.setHeldItem(new ItemStack(random.nextBoolean()?PixelmonItems.leftovers:PixelmonItems.life_orb));
        }
        pokemon.getPersistentData().func_74757_a(PROFILE,true);
    }
    public static Element defensiveType(Pokemon pokemon,List<Pokemon> threats,Random random){
        Element best=pokemon.getForm().getTypes().get(0);double bestScore=-Double.MAX_VALUE;
        for(int i=0;i<18;i++){
            Element type=Tera.TYPES[i];double incoming=0;
            for(Pokemon enemy:threats)for(Attack a:enemy.getMoveset())if(a.getAttackCategory()!=AttackCategory.STATUS)
                incoming=Math.max(incoming,Math.max(40,a.getMove().getBasePower())*Element.getTotalEffectiveness(Collections.singletonList(type),a.getType()));
            boolean stab=pokemon.getMoveset().stream().anyMatch(a->a.getAttackCategory()!=AttackCategory.STATUS && a.getType()==type);
            double score=-incoming+(stab?18:0)+random.nextDouble()*8;
            if(score>bestScore){bestScore=score;best=type;}
        }
        return best;
    }
    public static boolean spent(BattleParticipant p){return Tera.used(p) || p.evolution!=null || p.dynamax!=null || p.ultraBurst!=null || p.usedZ;}
    public static Choice decide(PixelmonWrapper p,Attack attack){
        if(!(p.getParticipant() instanceof TrainerParticipant) || !enabled(((TrainerParticipant)p.getParticipant()).trainer)
            || p.bc==null || p.bc.simulateMode || p.bc.isRaid() || !p.isAlive() || !p.onBattlefield || Tera.used(p.getParticipant())
            || attack==null || attack.getAttackCategory()==AttackCategory.STATUS)return Choice.NONE;
        List<PixelmonWrapper> enemies=p.getOpponentPokemon();if(enemies.isEmpty())return Choice.NONE;
        boolean queuedTera=false,queuedNative=false,queuedZ=false;
        for(PixelmonWrapper member:p.getParticipant().allPokemon)if(member!=p){queuedTera|=Tera.selected(member);queuedNative|=member.willEvolve || member.usingZ;queuedZ|=member.usingZ;}
        double incoming=0,after=0,pressure=0;Element tera=Tera.type(p.pokemon);
        for(PixelmonWrapper enemy:enemies){
            for(Attack a:enemy.getMoveset())if(a.getAttackCategory()!=AttackCategory.STATUS){
                double power=Math.max(40,a.getMove().getBasePower());
                incoming=Math.max(incoming,power*Element.getTotalEffectiveness(p.type,a.getType()));
                after=Math.max(after,power*Element.getTotalEffectiveness(tera==Element.MYSTERY?p.type:Collections.singletonList(tera),a.getType()));
            }
            pressure=Math.max(pressure,attack.getMove().getBasePower()*Element.getTotalEffectiveness(enemy.type,attack.getType()));
        }
        if(pressure<=0 || (incoming<80 && enemies.stream().allMatch(e->e.getHealthPercent()<25)))return Choice.NONE;
        Choice best=Choice.NONE;double score=30;
        double teraScore=(incoming-after)*.65+(attack.getType()==tera?40:0);
        if(!queuedNative && Tera.eligible(p) && teraScore>score){best=Choice.TERA;score=teraScore;}
        double megaScore=45+Math.min(30,incoming*.1);
        if(!queuedTera && p.bc.oldGen.isYes() && p.getParticipant().evolution==null && p.getParticipant().ultraBurst==null && p.getParticipant().canMegaEvolve() && (p.canMegaEvolve() || p.canUltraBurst()) && megaScore>score){best=Choice.MEGA;score=megaScore;}
        // Respect native generation rules and Mega's precedence over Dynamax with a compatible stone.
        double maxScore=incoming*.3+(p.getHealthPercent()>40 && p.getHealthPercent()<85?25:0);
        if(!queuedTera && p.getParticipant().dynamax==null && p.getParticipant().canDynamax() && (p.canDynamax() || p.canGigantamax())
            && !(p.bc.oldGen.isYes() && (p.canMegaEvolve() || p.canUltraBurst())) && maxScore>score){best=Choice.DYNAMAX;score=maxScore;}
        if(!queuedTera && !queuedZ && !p.getParticipant().usedZ && p.bc.oldGen.isYes() && p.getParticipant().canMegaEvolve() && attack.getMove().hasZMove(p.pokemon) && pressure*.45>score)best=Choice.Z;
        return best;
    }
    public static void plan(PixelmonWrapper p,MoveChoice choice){
        if(choice!=null && choice.isAttack())PENDING.put(p,decide(p,choice.attack));else PENDING.remove(p);
    }
    public static void apply(PixelmonWrapper p){
        Choice choice=PENDING.remove(p);if(choice==null || p.bc.simulateMode)return;
        // Native chooseMove passes evolving=true for every attack. Replace that unconditional choice.
        p.willEvolve=false;p.usingZ=false;
        if(NpcMedicine.plan(p))return;
        if(Tera.used(p.getParticipant()))return;
        switch(choice){case TERA:Tera.select(p);break;case MEGA:case DYNAMAX:p.willEvolve=true;break;case Z:p.usingZ=true;break;default:break;}
    }
}
