package local.pixelmon.backport;
import java.util.*;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.pokemon.species.Species;
import com.pixelmonmod.pixelmon.api.pokemon.boss.BossTierRegistry;
import com.pixelmonmod.pixelmon.api.registries.*;
import com.pixelmonmod.pixelmon.entities.npcs.NPCTrainer;
import com.pixelmonmod.pixelmon.entities.npcs.registry.NPCRegistryTrainers;
import com.pixelmonmod.pixelmon.enums.*;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import net.minecraft.item.*;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.registries.ForgeRegistries;

/** Six authored members; native equal-level scaling, moves, training and bag AI remain shared. */
public final class AuthorBossTeams {
    public static final String MARKER="BackportAuthorTrainer";
    public static final Set<Integer> PARADOX=Set.of(984,985,986,987,988,989,990,991,992,993,994,995,1005,1006,1009,1010,1020,1021,1022,1023);
    public static final Set<Integer> ULTRA=Set.of(791,792,793,794,795,796,797,798,799,800,804,805,806);
    private AuthorBossTeams(){}
    public static boolean is(NPCTrainer npc){return npc!=null&&npc.getPersistentData().func_74767_n(MARKER);}
    public static ItemStack item(String id){Item value=ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));if(value==null||value==Items.field_190931_a)throw new IllegalStateException("Missing author Boss item "+id);return new ItemStack(value);}
    public static boolean legendary(Species s){return s.getDefaultForm().getTags().isLegendary()||s.getDefaultForm().getTags().isMythical();}
    public static List<Species> pool(String theme,boolean legends){
        List<Species> pool=new ArrayList<>();
        for(Species s:PixelmonSpecies.getAll()){
            int dex=s.getDex();if(dex<=0||dex==CapSpecies.DEX)continue;
            if(theme.equals("paradox")){if(PARADOX.contains(dex))pool.add(s);continue;}
            if(theme.equals("ultra")){if(ULTRA.contains(dex))pool.add(s);continue;}
            if(legends&&!legendary(s))continue;
            if(!s.getDefaultForm().getEvolutions().isEmpty())continue;
            List<String> types=AuthorBossPolicy.TYPES.getOrDefault(theme,List.of("NORMAL"));
            if(s.getDefaultForm().getTypes().stream().anyMatch(t->types.contains(t.name())))pool.add(s);
        }
        pool.sort(Comparator.comparingInt(Species::getDex));return pool;
    }
    private static void add(List<Pokemon> team,Species species){
        if(species==null||team.stream().anyMatch(p->p.getSpecies().getDex()==species.getDex()))return;
        Pokemon p=PokemonFactory.create(species);p.setLevelNum(70);p.initialize();p.rerollMoveset();team.add(p);
    }
    private static void take(List<Pokemon> team,List<Species> pool,Random random){
        List<Species> options=new ArrayList<>(pool);options.removeIf(s->team.stream().anyMatch(p->p.getSpecies().getDex()==s.getDex()));
        if(options.isEmpty())throw new IllegalStateException("Not enough distinct legal author Boss members");
        add(team,options.get(random.nextInt(options.size())));
    }
    public static List<Pokemon> roster(String theme,Random random){
        List<Pokemon> team=new ArrayList<>();
        if(theme.equals("plains")){
            add(team,PixelmonSpecies.fromNationalDex(493));team.get(0).setHeldItem(item("pixelmon:legend_plate"));
            List<Species> all=new ArrayList<>();for(Species s:PixelmonSpecies.getAll())if(legendary(s)&&s.getDex()!=CapSpecies.DEX&&s.getDefaultForm().getEvolutions().isEmpty())all.add(s);
            all.sort(Comparator.comparingInt(Species::getDex));take(team,all,random);
        }
        if(theme.equals("ocean")){
            add(team,PixelmonSpecies.fromNationalDex(382));team.get(0).setForm("primal");team.get(0).setHeldItem(item("pixelmon:blue_orb"));
        }
        if(theme.equals("badlands")){
            add(team,PixelmonSpecies.fromNationalDex(CapSpecies.DEX));team.get(0).setHeldItem(item("pixelmon:pikashunium_z"));
        }
        if(!theme.equals("ultra")&&!theme.equals("paradox")){
            List<Species> legends=pool(theme,true);
            while(team.stream().filter(p->legendary(p.getSpecies())&&p.getForm().getTypes().stream().anyMatch(t->AuthorBossPolicy.TYPES.get(theme).contains(t.name()))).count()<2)take(team,legends,random);
        }
        while(team.size()<6)take(team,pool(theme,false),random);
        for(Pokemon p:team){
            if(theme.equals("ultra")&&p.getSpecies().getDex()==800){
                List<String> forms=new ArrayList<>();for(String f:List.of("","dawn","dusk","ultra"))if(p.getSpecies().getForms().stream().anyMatch(s->s.getName().equals(f)))forms.add(f);
                p.setForm(forms.get(random.nextInt(forms.size())));
            }
            p.getPersistentData().func_74757_a(NpcMoves.ACE,true);NpcMoves.configure(p,new UUID(random.nextLong(),random.nextLong()));
            if(p.getSpecies().getDex()==CapSpecies.DEX&&p.getMoveset().stream().noneMatch(a->a.getMove().getAttackName().equals("Ten Million Volts"))){
                var move=com.pixelmonmod.pixelmon.api.battles.attack.AttackRegistry.getAttackBase("Ten Million Volts").orElseThrow();
                if(p.getMoveset().size()>=4)p.getMoveset().set(0,new Attack(move));else p.getMoveset().add(new Attack(move));
            }
            p.getPersistentData().func_74757_a(NpcGimmicks.PROFILE,true);
        }
        boolean mega=false;
        for(Pokemon p:team){
            if(p.getHeldItem().func_190926_b()&&!mega){
                List<Item> stones=new ArrayList<>();for(Item stone:ForgeRegistries.ITEMS)if(stone instanceof com.pixelmonmod.pixelmon.items.heldItems.MegaStoneItem&&PixelmonWrapper.canMegaEvolve(new ItemStack(stone),p.getSpecies(),p.getForm().getName()))stones.add(stone);
                stones.sort(Comparator.comparing(s->s.getRegistryName().toString()));if(!stones.isEmpty()){p.setHeldItem(new ItemStack(stones.get(random.nextInt(stones.size()))));mega=true;}
            }
            if(p.getHeldItem().func_190926_b())p.setHeldItem(new ItemStack(random.nextBoolean()?PixelmonItems.leftovers:PixelmonItems.life_orb));
            NpcTraining.prepare(p);p.heal();
        }
        return team;
    }
    public static NPCTrainer create(ServerWorld world,BlockPos center,String theme){
        NPCTrainer npc=new NPCTrainer(world);npc.getPersistentData().func_74757_a(MARKER,true);
        npc.getPersistentData().func_74778_a("AuthorBossTheme",theme);
        String primary=AuthorBossPolicy.TYPES.getOrDefault(theme,List.of(theme.equals("paradox")?"STEEL":"PSYCHIC")).get(0).toLowerCase(Locale.ROOT);
        npc.getPersistentData().func_74778_a(GymCities.TYPE,primary);npc.getPersistentData().func_74778_a("BackportGymRole","leader");
        npc.setBaseTrainer(NPCRegistryTrainers.Steve);npc.setTextureIndex(-1);npc.setCustomSteveTexture("mystery_task_man");
        npc.setName(AuthorBossPolicy.NAME);npc.setNickName(AuthorBossPolicy.NAME);npc.func_174805_g(true);npc.func_110163_bv();
        npc.isGymLeader=true;npc.winMoney=0;npc.setEngageDistance(0);npc.setEncounterMode(EnumEncounterMode.Once);
        npc.setOldGenMode(EnumOldGenMode.Both);npc.setMegaItem(EnumMegaItemsUnlocked.Both);
        npc.setBattleAIMode(com.pixelmonmod.pixelmon.api.battles.BattleAIMode.ADVANCED);
        npc.setAIMode(EnumTrainerAI.StandStill);npc.updateDrops(new ItemStack[0]);
        Random random=new Random(world.func_72905_C()^center.func_218275_a());int slot=0;
        for(Pokemon p:roster(theme,random))npc.getPokemonStorage().set(slot++,p);
        npc.setBossTier(BossTierRegistry.getBossTierOrNotBoss("equal"));npc.updateTrainerLevel();
        npc.func_70012_b(center.func_177958_n()+.5,center.func_177956_o()+1,center.func_177952_p()-2.5,0,0);
        AuthorBossArenas.tag(npc,center);return npc;
    }
}
