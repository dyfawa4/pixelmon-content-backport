package local.pixelmon.backport;
import java.util.*;
import com.google.gson.*;
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
    private static final Map<PixelmonWrapper,MoveChoice> PENDING=Collections.synchronizedMap(new WeakHashMap<>());
    public static boolean enabled(NPCTrainer npc){
        return !npc.getPersistentData().func_74767_n("BackportNpcDisable")
            && "equal".equalsIgnoreCase(npc.getBossTier().getID()) && GymCities.theme(npc)!=null;
    }
    public static void prepare(TrainerParticipant participant){
        if(participant.bc==null || participant.bc.simulateMode || participant.bc.isRaid())return;
        if(!enabled(participant.trainer)){
            if(!participant.trainer.getPersistentData().func_74767_n("BackportNpcDisable") && GymCities.theme(participant.trainer)!=null)
                for(PixelmonWrapper wrapper:participant.allPokemon){NpcTraining.optimizeNature(wrapper.pokemon);NpcTraining.copyNature(wrapper.pokemon,wrapper.initialCopyOfPokemon);}
            return;
        }
        augment(participant);
        NpcTeamKnowledge.capture(participant);
        armTeam(participant);
        NpcMedicine.prepare(participant);
        participant.trainer.setMegaItem(EnumMegaItemsUnlocked.Both);
        participant.trainer.setBattleAIMode(BattleAIMode.ADVANCED);
        List<Pokemon> threats=new ArrayList<>();
        for(BattleParticipant other:participant.bc.participants)if(other.team!=participant.team){
            for(PixelmonWrapper p:other.allPokemon)if(p.isAlive())threats.add(p.pokemon);
            if(threats.isEmpty() && other.allPokemon.length>0)threats.add(other.allPokemon[0].pokemon);
        }
        for(PixelmonWrapper wrapper:participant.allPokemon){
            if(GymCities.isLeader(participant.trainer) && wrapper.getSpecies().getDex()==890){
                wrapper.pokemon.getPersistentData().func_74757_a(NpcMoves.ACE,true);
                wrapper.pokemon.setHeldItem(new ItemStack(Backport.REGISTERED.get("eternatus_meteorite").get()));
            }
            // Legacy trainer NBT can retain an ability slot absent from the current form.
            // Repair that slot with a legal species ability before native AI simulates switches.
            if(wrapper.pokemon.getAbility()==null){
                for(var ability:wrapper.pokemon.getForm().getAbilities().getAbilities())if(ability!=null){wrapper.pokemon.setAbility(ability);break;}
                if(wrapper.pokemon.getAbility()==null)throw new IllegalStateException("Gym member has no legal ability: "+wrapper.pokemon.getSpecies().getName()+" / "+wrapper.pokemon.getForm().getName());
            }
            NpcMoves.configure(wrapper.pokemon,participant.trainer.func_110124_au());
            if(GymCities.isLeader(participant.trainer))NpcMoves.tune(wrapper.pokemon,participant.trainer.func_110124_au());
            NpcMoves.coverage(wrapper.pokemon);
            NpcBuildRoles.configure(wrapper.pokemon);
            profile(wrapper.pokemon,participant.trainer.func_110124_au(),threats);
            // Native battle cleanup restores this snapshot, including its held item.
            wrapper.initialCopyOfPokemon.setHeldItem(wrapper.pokemon.getHeldItem().func_77946_l());
        }
        if(AuthorBossTeams.is(participant.trainer))AuthorRoster.support(Arrays.stream(participant.allPokemon).map(w->w.pokemon).collect(java.util.stream.Collectors.toList()));
        else if(GymCities.isLeader(participant.trainer))GymCounterTeam.configure(Arrays.stream(participant.allPokemon).map(w->w.pokemon).collect(java.util.stream.Collectors.toList()));
        NpcTeamKnowledge.configure(participant);
        if(AuthorBossTeams.is(participant.trainer))AuthorSupportPlan.configure(Arrays.stream(participant.allPokemon).map(w->w.pokemon).collect(java.util.stream.Collectors.toList()));
        armTeam(participant); // Recheck compatibility after moves and persistent profiles are finalized.
        for(PixelmonWrapper wrapper:participant.allPokemon){
            if(GymCities.isLeader(participant.trainer)){
                if(AuthorBossTeams.is(participant.trainer))AuthorRoster.train(wrapper.pokemon);else NpcTraining.maximize(wrapper.pokemon);NpcTraining.copy(wrapper.pokemon,wrapper.initialCopyOfPokemon);
            }else {NpcTraining.optimizeNature(wrapper.pokemon);NpcTraining.copyNature(wrapper.pokemon,wrapper.initialCopyOfPokemon);}
            NpcHeldItems.equip(wrapper.pokemon);
            wrapper.initialCopyOfPokemon.setHeldItem(wrapper.pokemon.getHeldItem().func_77946_l());
        }
        GymChallenge.prepare(participant);
    }
    public static void augment(TrainerParticipant participant){
        GymLegendaryPair.ensure(participant);
    }

    private static boolean aceEligible(Pokemon pokemon,Element theme){return aceEligible(pokemon.getSpecies(),pokemon.getForm(),theme);}
    private static boolean aceEligible(com.pixelmonmod.pixelmon.api.pokemon.species.Species species,com.pixelmonmod.pixelmon.api.pokemon.species.Stats form,Element theme){
        int total=0;for(var stat:new com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType[]{com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType.HP,com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType.ATTACK,com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType.DEFENSE,com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType.SPECIAL_ATTACK,com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType.SPECIAL_DEFENSE,com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType.SPEED})total+=form.getBattleStats().getStat(stat);
        boolean usable=form.getMoves().getAllMoves().stream().filter(a->a.getAttackCategory()!=AttackCategory.STATUS && a.getBasePower()>0).count()>=2;
        boolean legendary=species.getDefaultForm().getTags().isLegendary() || species.getDefaultForm().getTags().isMythical() || form.getTags().isLegendary() || form.getTags().isMythical();
        return GymAcePolicy.eligible(species.getDex(),total,legendary,usable,form.getTypes().contains(theme));
    }

    public static void profile(Pokemon pokemon,UUID trainer,List<Pokemon> threats){
        if(pokemon.getPersistentData().func_74767_n(PROFILE))return;
        Random random=new Random(trainer.getLeastSignificantBits() ^ pokemon.getUUID().getMostSignificantBits());
        int saved=pokemon.getPersistentData().func_74762_e("TeraType");Element original=pokemon.getForm().getTypes().get(0);
        // Keep explicitly configured non-default Tera types, locked forms, and existing held items.
        if(saved==0 || saved==Arrays.asList(Tera.TYPES).indexOf(original)+1)Tera.changeType(pokemon,defensiveType(pokemon,threats,random));
        if(pokemon.getSpecies().getDex()==890 && pokemon.getForm().isForm("eternamax"))
            pokemon.setHeldItem(new ItemStack(Backport.REGISTERED.get("eternatus_meteorite").get()));
        if(pokemon.getHeldItem().func_190926_b()){
            List<Item> mega=new ArrayList<>();
            for(Item item:PixelmonItems.megaStones){
                if(item==Backport.REGISTERED.get("dragoninite").get() && item!=Backport.item("dragoninite"))continue;
                if(PixelmonWrapper.canMegaEvolve(new ItemStack(item),pokemon.getSpecies(),pokemon.getForm().getName()))mega.add(item);
            }
            if(!mega.isEmpty() && random.nextInt(3)!=0)pokemon.setHeldItem(new ItemStack(mega.get(random.nextInt(mega.size()))));
            else if(pokemon.getSpecies().getDex()!=493&&random.nextBoolean()){
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
    private static List<Item> megaItems(Pokemon pokemon){
        List<Item> stones=new ArrayList<>();
        for(Item item:PixelmonItems.megaStones)if(PixelmonWrapper.canMegaEvolve(new ItemStack(item),pokemon.getSpecies(),pokemon.getForm().getName()))stones.add(item);
        stones.sort(Comparator.comparing(i->i.getRegistryName().toString()));return stones;
    }
    private static boolean formItem(Pokemon pokemon){
        Item item=pokemon.getHeldItem().func_77973_b();
        if(item instanceof ReferenceItems.Meteorite || item instanceof Backport.Mask)return true;
        String name=item.getRegistryName()==null?"":item.getRegistryName().func_110623_a();
        return Arrays.asList("red_orb","blue_orb","rusted_sword","rusted_shield","griseous_orb","griseous_core","adamant_crystal","lustrous_globe","legend_plate").contains(name)
            ||pokemon.getSpecies().getDex()==493&&name.endsWith("_plate")||pokemon.getSpecies().getDex()==773&&name.endsWith("_memory")||pokemon.getSpecies().getDex()==649&&name.endsWith("_drive");
    }
    /** A legal Mega user and a distinct Z user provide options; native rules decide which can be spent. */
    public static void armTeam(TrainerParticipant participant){
        if(participant.trainer.getPersistentData().func_74767_n("BackportAuthorTrainer"))return;
        if(!GymCities.isLeader(participant.trainer))return;
        boolean hasMega=Arrays.stream(participant.allPokemon).anyMatch(p->PixelmonWrapper.canMegaEvolve(p.pokemon.getHeldItem(),p.getSpecies(),p.getForm().getName()));
        if(!hasMega && Arrays.stream(participant.allPokemon).noneMatch(p->!megaItems(p.pokemon).isEmpty())){
            int dex=GymCities.DATA.getAsJsonObject("megaAnchors").get(GymCities.theme(participant.trainer).name().toLowerCase(Locale.ROOT)).getAsInt();
            int index=participant.allPokemon.length;
            if(index>=6){
                index=-1;for(int i=participant.allPokemon.length-1;i>=0;i--)if(!AuthorBossTeams.legendary(participant.allPokemon[i].getSpecies()) && !(participant.allPokemon[i].pokemon.getHeldItem().func_77973_b() instanceof ZCrystalItem)){index=i;break;}
            }
            if(index>=0 && participant.allPokemon.length>0){
                Pokemon source=participant.allPokemon[Math.min(index,participant.allPokemon.length-1)].pokemon;
                Pokemon pokemon=PokemonFactory.create(com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies.fromNationalDex(dex));
                pokemon.setLevelNum(source.getPokemonLevel());pokemon.initialize();
                net.minecraft.nbt.CompoundNBT training=new net.minecraft.nbt.CompoundNBT();source.getIVs().writeToNBT(training);source.getEVs().writeToNBT(training);
                pokemon.getIVs().readFromNBT(training);pokemon.getEVs().readFromNBT(training);pokemon.getStats().recalculateStats();pokemon.heal();
                pokemon.getPersistentData().func_74757_a(NpcMoves.ACE,true);NpcMoves.configure(pokemon,participant.trainer.func_110124_au());
                participant.trainer.getPokemonStorage().set(index,pokemon);
                participant.allPokemon=Arrays.copyOf(participant.allPokemon,Math.max(participant.allPokemon.length,index+1));
                PixelmonWrapper added=new PixelmonWrapper(participant,pokemon,index);
                added.bc=participant.bc;participant.allPokemon[index]=added;
            }
        }
        PixelmonWrapper mega=null;
        for(PixelmonWrapper member:participant.allPokemon){
            if(PixelmonWrapper.canMegaEvolve(member.pokemon.getHeldItem(),member.getSpecies(),member.getForm().getName())){mega=member;break;}
        }
        if(mega==null)for(PixelmonWrapper member:participant.allPokemon){
            if(!member.pokemon.getHeldItem().func_190926_b() && !(member.pokemon.getHeldItem().func_77973_b()==PixelmonItems.leftovers || member.pokemon.getHeldItem().func_77973_b()==PixelmonItems.life_orb))continue;
            List<Item> stones=megaItems(member.pokemon);if(!stones.isEmpty()){member.pokemon.setHeldItem(new ItemStack(stones.get(0)));mega=member;break;}
        }
        // If authored berry/utility slots prevented equipment, retain special form items and select a legal ordinary slot.
        if(mega==null)for(PixelmonWrapper member:participant.allPokemon){
            if(member.pokemon.getHeldItem().func_77973_b() instanceof ZCrystalItem || formItem(member.pokemon))continue;
            List<Item> stones=megaItems(member.pokemon);if(!stones.isEmpty()){member.pokemon.setHeldItem(new ItemStack(stones.get(0)));mega=member;break;}
        }
        boolean hasZ=Arrays.stream(participant.allPokemon).anyMatch(p->p.pokemon.getHeldItem().func_77973_b() instanceof ZCrystalItem && p.getMoveset().stream().anyMatch(a->a.getAttackCategory()!=AttackCategory.STATUS && a.getMove().hasZMove(p.pokemon)));
        if(!hasZ)for(PixelmonWrapper member:participant.allPokemon){
            Pokemon pokemon=member.pokemon;
            if(member==mega || formItem(pokemon) || pokemon.getSpecies().getDex()==493 || !megaItems(pokemon).isEmpty() && !pokemon.getHeldItem().func_190926_b() && PixelmonWrapper.canMegaEvolve(pokemon.getHeldItem(),pokemon.getSpecies(),pokemon.getForm().getName()))continue;
            ItemStack original=pokemon.getHeldItem().func_77946_l();List<Item> crystals=new ArrayList<>();
            for(Item item:net.minecraftforge.registries.ForgeRegistries.ITEMS)if(item instanceof ZCrystalItem){
                pokemon.setHeldItem(new ItemStack(item));
                if(pokemon.getMoveset().stream().anyMatch(a->a.getAttackCategory()!=AttackCategory.STATUS && a.getMove().hasZMove(pokemon)))crystals.add(item);
            }
            pokemon.setHeldItem(original);crystals.sort(Comparator.comparing(i->i.getRegistryName().toString()));
            if(!crystals.isEmpty()){pokemon.setHeldItem(new ItemStack(crystals.get(0)));break;}
        }
    }
    public static Element defensiveType(Pokemon pokemon,List<Pokemon> threats,Random random){
        Element best=pokemon.getForm().getTypes().get(0);double bestScore=-Double.MAX_VALUE;
        for(int i=0;i<18;i++){
            Element type=Tera.TYPES[i];double incoming=0;
            for(Pokemon enemy:threats)for(Element enemyType:enemy.getForm().getTypes())
                incoming=Math.max(incoming,100*Element.getTotalEffectiveness(Collections.singletonList(type),enemyType));
            boolean stab=pokemon.getMoveset().stream().anyMatch(a->a.getAttackCategory()!=AttackCategory.STATUS && a.getType()==type);
            double score=-incoming+(stab?18:0)+random.nextDouble()*8;
            if(score>bestScore){bestScore=score;best=type;}
        }
        return best;
    }
    private static final ThreadLocal<MoveChoice> ACTIVE_MOVE=new ThreadLocal<>();
    public static boolean spent(BattleParticipant p){return Tera.used(p) || p.evolution!=null || p.dynamax!=null || p.ultraBurst!=null || p.usedZ;}
    private static double estimateEffectiveness(PixelmonWrapper user,PixelmonWrapper target,Attack move,double value){return AuthorPiercing.affects(user,target,move)?AuthorPiercing.multiplier(value):LightningOath.effectiveness(user,target,move,value);}
    public static Choice decide(PixelmonWrapper p,Attack attack){
        if(!(p.getParticipant() instanceof TrainerParticipant) || !enabled(((TrainerParticipant)p.getParticipant()).trainer)
            || p.bc==null || p.bc.simulateMode || p.bc.isRaid() || !p.isAlive() || !p.onBattlefield || Tera.used(p.getParticipant())
            || attack==null)return Choice.NONE;
        List<PixelmonWrapper> enemies=p.getOpponentPokemon();if(enemies.isEmpty())return Choice.NONE;
        boolean author=((TrainerParticipant)p.getParticipant()).trainer.getPersistentData().func_74767_n("BackportAuthorTrainer");
        boolean authorMega=author&&Arrays.stream(p.getParticipant().allPokemon).anyMatch(m->m.isAlive()&&PixelmonWrapper.canMegaEvolve(m.pokemon.getHeldItem(),m.getSpecies(),m.getForm().getName()));
        boolean queuedTera=false,queuedNative=false,queuedZ=false;
        for(PixelmonWrapper member:p.getParticipant().allPokemon)if(member!=p){queuedTera|=Tera.selected(member);queuedNative|=member.willEvolve || member.usingZ;queuedZ|=member.usingZ;}
        double incoming=0,after=0,pressure=0;Element tera=Tera.type(p.pokemon);
        for(PixelmonWrapper enemy:enemies){
            for(Attack a:NpcTactics.knownMoves(p,enemy))if(a.getAttackCategory()!=AttackCategory.STATUS){
                double power=Math.max(40,a.getMove().getBasePower());
                incoming=Math.max(incoming,power*estimateEffectiveness(enemy,p,a,Element.getTotalEffectiveness(p.type,a.getType())));
                after=Math.max(after,power*estimateEffectiveness(enemy,p,a,Element.getTotalEffectiveness(tera==Element.MYSTERY?p.type:Collections.singletonList(tera),a.getType())));
            }
            pressure=Math.max(pressure,attack.getMove().getBasePower()*estimateEffectiveness(p,enemy,attack,Element.getTotalEffectiveness(enemy.type,attack.getType())));
        }
        boolean status=attack.getAttackCategory()==AttackCategory.STATUS;
        double immediate=0,defended=0;
        if(NpcTurnKnowledge.active(p.bc)){
            immediate=NpcTactics.incoming(p,NpcTactics.threats(p));defended=NpcTactics.defensiveRisk(p,tera);
        }
        if((!status && pressure<=0) || (!author && incoming<80 && enemies.stream().allMatch(e->e.getHealthPercent()<25)))return Choice.NONE;
        boolean priorityNeeded=attack.getMove().getPriority(p)>0&&immediate>=p.getHealth();
        Choice best=Choice.NONE;double score=30;
        double teraScore=(incoming-after)*.65+(!status && attack.getType()==tera?40:0);
        if(immediate>0)teraScore+=Math.max(-80,Math.min(150,(immediate-defended)/Math.max(1,p.getMaxHealth())*150));
        if(immediate>=p.getHealth()&&defended<p.getHealth())teraScore+=150;
        if(!authorMega && !queuedNative && Tera.eligible(p) && teraScore>score){best=Choice.TERA;score=teraScore;}
        double megaScore=45+Math.min(30,incoming*.1)+(author?80:0);
        if(!queuedTera && !queuedNative && (!status || incoming>=80) && p.bc.oldGen.isYes() && p.getParticipant().evolution==null && p.getParticipant().ultraBurst==null && p.getParticipant().canMegaEvolve() && (p.canMegaEvolve() || p.canUltraBurst()) && megaScore>score){best=Choice.MEGA;score=megaScore;}
        // Respect native generation rules and Mega's precedence over Dynamax with a compatible stone.
        double maxScore=incoming*.3+(p.getHealthPercent()>40 && p.getHealthPercent()<85?25:0)+(immediate>=p.getHealth()&&immediate<p.getHealth()*1.5?100:0);
        double control=enemies.stream().mapToDouble(e->NpcSignatureTactics.benefit(p,attack,e)).max().orElse(0);
        maxScore-=control*2; // A Max move replaces the signature and loses its control effect.
        if(!priorityNeeded && !authorMega && !status && !queuedTera && !queuedNative && p.getParticipant().dynamax==null && p.getParticipant().canDynamax() && (p.canDynamax() || p.canGigantamax())
            && !(p.bc.oldGen.isYes() && (p.canMegaEvolve() || p.canUltraBurst())) && maxScore>score){best=Choice.DYNAMAX;score=maxScore;}
        if(!priorityNeeded && !status && !queuedTera && !queuedNative && !queuedZ && !p.getParticipant().usedZ && p.bc.oldGen.isYes() && p.getParticipant().canMegaEvolve() && attack.getMove().hasZMove(p.pokemon)){
            var z=attack.getMove().getZMove(p.pokemon,false);double zScore=pressure*.45;
            zScore-=control*2;
            if(z!=null){
                double ratio=Math.max(1,z.basePower/(double)Math.max(1,attack.getMove().getBasePower()));
                for(var enemy:enemies){
                    double estimated=pressure; // Native move selection already compares legality and actual target damage.
                    var planned=ACTIVE_MOVE.get();if(planned!=null&&planned.user==p&&planned.result!=null&&planned.targets.contains(enemy))estimated=Math.max(planned.result.damage,planned.result.fullDamage);
                    if(estimated<enemy.getHealth()&&estimated*ratio>=enemy.getHealth())zScore+=100;
                }
            }
            if(zScore>score)best=Choice.Z;
        }
        return best;
    }
    public static void plan(PixelmonWrapper p,MoveChoice choice){
        if(!p.bc.simulateMode && choice!=null && choice.isAttack())PENDING.put(p,choice);else if(!p.bc.simulateMode)PENDING.remove(p);
    }
    public static void apply(PixelmonWrapper p){
        if(p.bc==null || p.bc.simulateMode)return;
        MoveChoice move=PENDING.remove(p);if(move==null)return;
        // Native chooseMove passes evolving=true for every attack. Replace that unconditional choice.
        p.willEvolve=false;p.usingZ=false;
        if(NpcMedicine.plan(p,move))return;
        if(Tera.used(p.getParticipant()))return;
        // Save the limited resource when an ordinary attack already finishes first.
        boolean author=p.getParticipant() instanceof TrainerParticipant&&((TrainerParticipant)p.getParticipant()).trainer.getPersistentData().func_74767_n("BackportAuthorTrainer");
        if(!author&&NpcTactics.finishFirst(p,move,NpcTactics.threats(p)))return;
        if(move.result!=null && move.result.result==com.pixelmonmod.pixelmon.battles.controller.log.AttackResult.failed)return;
        Choice choice;ACTIVE_MOVE.set(move);try{choice=decide(p,move.attack);}finally{ACTIVE_MOVE.remove();}
        switch(choice){case TERA:Tera.select(p);break;case MEGA:case DYNAMAX:p.willEvolve=true;break;case Z:p.usingZ=true;break;default:break;}
    }
}
