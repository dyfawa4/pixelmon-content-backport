package qa;

import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import local.pixelmon.backport.*;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.pokemon.species.Species;
import com.pixelmonmod.pixelmon.api.registries.*;
import com.pixelmonmod.pixelmon.items.heldItems.MegaStoneItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

/** Audit only the immutable published 1.8.0 core in a disposable world. */
@Mod("backport_author180_audit") @Mod.EventBusSubscriber(modid="backport_author180_audit")
public final class Author180AuditProbe {
    static final String WORLD="backport-author180-audit";
    static boolean opened,hidden,ran;static volatile boolean done;static int age,warmup;
    static final JsonObject result=new JsonObject();static final JsonArray teams=new JsonArray();static final Map<String,Pokemon> catalogue=new TreeMap<>();
    static boolean enabled(){return WORLD.equals(System.getProperty("backport.manual.world"));}
    static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
    static void put(Pokemon p){catalogue.putIfAbsent(p.getSpecies().getDex()+"/"+p.getForm().getName(),PokemonFactory.copy(p));}
    static JsonArray ints(int[] values){return new Gson().toJsonTree(values).getAsJsonArray();}
    static int[] panel(Pokemon p){int[] values=new int[6];int i=0;for(var type:BattleStatModifiers.STATS)values[i++]=p.getStats().calculateStat(type,p.getNature(),p.getForm(),p.getPokemonLevel());return values;}
    static JsonObject row(Pokemon p,boolean neutral){
        var out=new JsonObject();out.addProperty("dex",p.getSpecies().getDex());out.addProperty("species",p.getSpecies().getName());out.addProperty("form",p.getForm().getName());
        int[] base=new int[6];for(int i=0;i<6;i++)base[i]=p.getForm().getBattleStats().getStat(BattleStatModifiers.STATS[i]);out.add("base",ints(base));out.addProperty("base_total",Arrays.stream(base).sum());
        out.add("ivs",ints(p.getIVs().getArray()));out.add("evs",ints(p.getEVs().getArray()));out.addProperty("ev_total",Arrays.stream(p.getEVs().getArray()).sum());out.addProperty("nature",p.getNature().name());
        out.addProperty("reference_neutral",neutral);out.addProperty("ability",p.getAbility()==null?"":p.getAbility().getClass().getSimpleName());out.addProperty("held_item",p.getHeldItem().func_190926_b()?"":p.getHeldItem().func_77973_b().getRegistryName().toString());
        JsonArray moves=new JsonArray();for(var attack:p.getMoveset())moves.add(attack.getMove().getAttackName());out.add("moves",moves);
        int level=p.getPokemonLevel();p.setLevelNum(70);p.getStats().recalculateStats();out.add("stats70",ints(panel(p)));p.setLevelNum(100);p.getStats().recalculateStats();out.add("stats100",ints(panel(p)));p.setLevelNum(level);p.getStats().recalculateStats();return out;
    }
    @SubscribeEvent public static void server(TickEvent.ServerTickEvent event){
        if(!enabled()||ran||event.phase!=TickEvent.Phase.END)return;var s=ServerLifecycleHooks.getCurrentServer();if(s==null||s.func_184103_al().func_181057_v().isEmpty()||++warmup<100)return;
        ran=true;try{audit();System.out.println("AUTHOR180_AUDIT_PASS teams="+teams.size()+" catalogue="+catalogue.size());Files.writeString(Path.of("author180-result.txt"),"AUTHOR180_AUDIT_PASS\n");}
        catch(Throwable e){e.printStackTrace();try{Files.writeString(Path.of("author180-result.txt"),"AUTHOR180_AUDIT_FAIL "+e);}catch(Exception ignored){}}finally{done=true;}
    }
    @SuppressWarnings("unchecked") static void audit()throws Exception{
        List<String> themes=new ArrayList<>(AuthorBossPolicy.TYPES.keySet());themes.addAll(List.of("ultra","paradox"));long seed=18020261026L;
        for(String theme:themes){JsonObject team=new JsonObject();team.addProperty("theme",theme);team.addProperty("seed",seed);JsonArray members=new JsonArray();for(Pokemon p:AuthorRoster.build(theme,new Random(seed))){
            check(Arrays.stream(p.getIVs().getArray()).allMatch(v->v==31),"Published IV31");check(Arrays.stream(p.getEVs().getArray()).allMatch(v->v==252),"Published six EV252");put(p);members.add(row(p,false));
        }team.add("members",members);teams.add(team);}
        var method=AuthorRoster.class.getDeclaredMethod("candidates",boolean.class);method.setAccessible(true);
        for(boolean legendary:new boolean[]{false,true})for(Species species:(List<Species>)method.invoke(null,legendary))put(PokemonFactory.create(species));
        var pool=AuthorCorePool.class.getDeclaredField("POOL");pool.setAccessible(true);for(AuthorCorePool.Core core:(List<AuthorCorePool.Core>)pool.get(null)){
            var p=PokemonFactory.create(PixelmonSpecies.fromNationalDex(core.dex));if(!core.form.isEmpty())p.setForm(core.form);put(p);
        }
        for(int i=0;i<6;i++)put(AuthorRoster.strong(i,new Random(seed+i)));
        for(Pokemon p:new ArrayList<>(catalogue.values()))for(var item:PixelmonItems.megaStones)if(item instanceof MegaStoneItem){
            if(com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper.canMegaEvolve(new net.minecraft.item.ItemStack(item),p.getSpecies(),p.getForm().getName())){
                var mega=PokemonFactory.copy(p);mega.setForm(((MegaStoneItem)item).getMegaForm());put(mega);
            }
        }
        JsonArray all=new JsonArray();for(Pokemon p:catalogue.values()){
            p.setLevelNum(100);p.initialize();p.setMintNature(Nature.HARDY);for(var type:BattleStatModifiers.STATS){p.getIVs().setStat(type,31);p.getIVs().setHyperTrained(type,false);}p.getEVs().fillFromArray(252,252,252,252,252,252);p.getStats().recalculateStats();all.add(row(p,true));
        }
        result.addProperty("version","1.8.0");result.addProperty("core_sha256","72ae745baacbed75f85eb1c6a386cbcbc453447e5453a7611d71bbeb14e8f260");result.addProperty("order","HP/Atk/Def/SpA/SpD/Spe");result.add("sample_teams",teams);result.add("candidate_forms_neutral_reference",all);
        Files.writeString(Path.of("author180-numbers.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result));
    }
    @SubscribeEvent public static void client(TickEvent.ClientTickEvent event){
        if(!enabled()||event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.func_71410_x();mc.field_71474_y.field_82881_y=false;
        if(!hidden){org.lwjgl.glfw.GLFW.glfwHideWindow(mc.func_228018_at_().func_198092_i());hidden=true;}
        if(!opened&&mc.field_71462_r instanceof MainMenuScreen){opened=true;mc.func_238191_a_(WORLD);}
        if(mc.field_71439_g!=null){com.pixelmonmod.pixelmon.client.ServerStorageDisplay.showStarterScreen=false;if(mc.field_71462_r instanceof com.pixelmonmod.pixelmon.client.gui.starter.ChooseStarterScreen)mc.func_147108_a(null);}
        if(done&&++age>35)mc.func_71400_g();
    }
}
