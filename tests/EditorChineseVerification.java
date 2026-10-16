import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarFile;
import javax.tools.ToolProvider;
import com.google.gson.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import local.pixelmon.backport.EditorNameAliases;

/** Execute native editor callbacks/checkFields with live-label registry fixtures. */
public final class EditorChineseVerification {
    static final String API="com.pixelmonmod.pixelmon.",BASE=API+"client.gui.pokemoneditor.IndividualEditorBaseScreen",BALL=API+"client.gui.pokemoneditor.PokemonEditorIndividualScreen";
    public static void main(String[] args)throws Exception{
        org.spongepowered.asm.launch.MixinBootstrap.init();
        org.spongepowered.asm.mixin.MixinEnvironment.getDefaultEnvironment().setSide(org.spongepowered.asm.mixin.MixinEnvironment.Side.CLIENT);
        org.spongepowered.asm.mixin.MixinEnvironment.getCurrentEnvironment().setSide(org.spongepowered.asm.mixin.MixinEnvironment.Side.CLIENT);
        org.spongepowered.asm.mixin.Mixins.addConfiguration("pixelmon_backport.mixins.json");
        var phase=org.spongepowered.asm.mixin.MixinEnvironment.class.getDeclaredMethod("gotoPhase",org.spongepowered.asm.mixin.MixinEnvironment.Phase.class);phase.setAccessible(true);phase.invoke(null,org.spongepowered.asm.mixin.MixinEnvironment.Phase.DEFAULT);
        var ctor=Class.forName("org.spongepowered.asm.mixin.transformer.MixinTransformer").getDeclaredConstructor();ctor.setAccessible(true);var transformer=(org.spongepowered.asm.mixin.transformer.IMixinTransformer)ctor.newInstance();
        try(var jar=new JarFile(args[0])){String[] targets={BASE,BALL,API+"client.gui.pokemoneditor.PokemonEditorAdvancedScreen",API+"client.gui.widgets.TabCompleteTranslateableTextField"};byte[][] code=new byte[4][];for(int i=0;i<4;i++)code[i]=transformer.transformClassBytes(targets[i],targets[i],read(jar,targets[i]));verify(jar,code[0],code[1],code[2],code[3]);}
    }
    static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);}
    static ClassNode node(byte[] b){var n=new ClassNode();new ClassReader(b).accept(n,0);return n;}
    static byte[] write(ClassNode n){var w=new ClassWriter(0);n.accept(w);return w.toByteArray();}
    static byte[] read(JarFile j,String n)throws Exception{try(var in=j.getInputStream(j.getJarEntry(n.replace('.','/')+".class"))){return in.readAllBytes();}}
    static void add(Map<String,String>s,String n,String b){s.put(n,"package "+n.substring(0,n.lastIndexOf('.'))+";"+b);}
    static void replace(Path p,ClassNode source,java.util.function.Predicate<MethodNode> take)throws Exception{var target=node(Files.readAllBytes(p));for(var m:source.methods)if(take.test(m)){target.methods.removeIf(v->v.name.equals(m.name)&&v.desc.equals(m.desc));target.methods.add(m);}Files.write(p,write(target));}
    static void hooks(byte[] code,String...expected){var n=node(code);for(String name:expected){boolean found=false;for(var m:n.methods)for(var i:m.instructions)if(i instanceof MethodInsnNode)found|=((MethodInsnNode)i).name.contains(name);check(found,"Missing editor injection "+name);}}
    public static void verify(JarFile jar,byte[] base,byte[] ball,byte[] advanced,byte[] widget)throws Exception{
        hooks(base,"backport$speciesName","backport$moveName","backport$moveCompletions","backport$paletteLabels");hooks(ball,"backport$ballName");
        hooks(advanced,"backport$heldItemName","backport$heldCompletions","backport$natureCompletions","backport$heldLabel","backport$unfinishedChineseItem");hooks(widget,"backport$bilingualPrefix");
        var config=Version13StaticVerification.resource("pixelmon_backport.mixins.json");for(String name:new String[]{"EditorBaseInputMixin","EditorBallInputMixin","EditorAdvancedInputMixin","EditorCompletionMixin"}){
            check(config.getAsJsonArray("client").toString().contains(name)&&!config.getAsJsonArray("mixins").toString().contains(name),"Editor hook must be client-only");
        }
        // Full native JSON name coverage, plus separate backport species/moves.
        JsonObject lang;try(var in=jar.getInputStream(jar.getJarEntry("assets/pixelmon/lang/zh_cn.json"))){lang=JsonParser.parseReader(new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}
        for(var row:Version13StaticVerification.resource("assets/pixelmon_backport/lang/zh_cn.json").entrySet())lang.add(row.getKey(),row.getValue());
        // Backport adds its Pixelmon-namespace translations at runtime too.
        Path nativeLang=Path.of("resources/assets/pixelmon/lang/zh_cn.json");if(Files.isRegularFile(nativeLang)){var extra=JsonParser.parseString(Files.readString(nativeLang)).getAsJsonObject();for(var e:extra.entrySet())lang.add(e.getKey(),e.getValue());}
        JsonObject tables=new JsonObject();for(String group:new String[]{"species","balls","moves"})tables.add(group,new JsonObject());
        var entries=jar.entries();while(entries.hasMoreElements()){
            var e=entries.nextElement();String group=e.getName().startsWith("data/pixelmon/species/")?"species":e.getName().startsWith("data/pixelmon/pokeballs/")?"balls":e.getName().startsWith("data/pixelmon/moves/")?"moves":null;
            if(group==null||!e.getName().endsWith(".json"))continue;
            try(var in=jar.getInputStream(e)){var data=JsonParser.parseReader(new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();String id=data.get(group.equals("moves")?"attackName":"name").getAsString();String key=group.equals("species")?(data.has("translationKey")?data.get("translationKey").getAsString():"pixelmon."+id.toLowerCase(Locale.ROOT)):group.equals("balls")?"item.pixelmon."+id:"attack."+id.toLowerCase(Locale.ROOT).replace(" ","_");tables.getAsJsonObject(group).addProperty(id,lang.has(key)?lang.get(key).getAsString():id);}
        }
        for(var row:Version13StaticVerification.resource("backport/species_names_zh.json").entrySet())lang.add(row.getKey(),row.getValue());
        var content=Version13StaticVerification.resource("backport/content.json");
        for(var patch:content.getAsJsonArray("patches")){var species=patch.getAsJsonObject().getAsJsonObject("species");String id=species.get("name").getAsString(),key=species.has("translationKey")?species.get("translationKey").getAsString():"pixelmon."+id.toLowerCase(Locale.ROOT);tables.getAsJsonObject("species").addProperty(id,lang.has(key)?lang.get(key).getAsString():id);}
        List<JsonElement> extraMoves=new ArrayList<>();for(var move:content.getAsJsonArray("moves"))extraMoves.add(move);
        for(var move:Version13StaticVerification.resource("backport/modern-moves.json").entrySet())extraMoves.add(move.getValue());
        for(var move:Version13StaticVerification.resource("backport/cap_pikachu.json").getAsJsonArray("moves"))extraMoves.add(move);
        for(var move:extraMoves){String id=move.getAsJsonObject().get("attackName").getAsString(),key="attack."+id.toLowerCase(Locale.ROOT).replace(" ","_");tables.getAsJsonObject("moves").addProperty(id,lang.has(key)?lang.get(key).getAsString():id);}
        tables.getAsJsonObject("species").addProperty("CapPikachu",lang.get("pixelmon.cappikachu").getAsString());
        JsonObject natures=new JsonObject();for(String name:new String[]{"HARDY","LONELY","BRAVE","ADAMANT","NAUGHTY","BOLD","DOCILE","RELAXED","IMPISH","LAX","TIMID","HASTY","SERIOUS","JOLLY","NAIVE","MODEST","MILD","QUIET","BASHFUL","RASH","CALM","GENTLE","SASSY","CAREFUL","QUIRKY"}){String key="enum.nature."+name.toLowerCase(Locale.ROOT);natures.addProperty(name,lang.has(key)?lang.get(key).getAsString():name);}tables.add("natures",natures);
        // Exact aliases, unknown/partial input, normalization and collision boundaries.
        recordCheck();
        for(var group:tables.entrySet()){
            var rows=group.getValue().getAsJsonObject().entrySet();int checked=0;
            for(var row:rows){String label=row.getValue().getAsString();long duplicates=rows.stream().filter(r->EditorNameAliases.key(r.getValue().getAsString()).equals(EditorNameAliases.key(label))).count();
                check(EditorNameAliases.resolve(row.getKey(),rows,Map.Entry::getKey,r->r.getValue().getAsString()).equals(row.getKey()),"English ID changed");
                if(duplicates==1){check(EditorNameAliases.resolve(label,rows,Map.Entry::getKey,r->r.getValue().getAsString()).equals(row.getKey()),"Localized lookup failed: "+row.getKey());checked++;}
            }System.out.println("PASS editor registry aliases "+group.getKey()+"="+rows.size()+", unambiguous="+checked);
        }
        Map<String,String>s=new LinkedHashMap<>();
        add(s,"fixture.Data","public class Data {public static com.google.gson.JsonObject tables;public static String label(String group,String id){return tables.getAsJsonObject(group).get(id).getAsString();}}");
        add(s,API+"api.util.ITranslatable","public interface ITranslatable {String getTranslationKey();String getLocalizedName();}");
        add(s,API+"api.pokemon.species.Species","public class Species implements "+API+"api.util.ITranslatable {public String name;public Species(String n){name=n;}public String getName(){return name;}public String getTranslationKey(){return name;}public String getLocalizedName(){return fixture.Data.label(\"species\",name);}public boolean is(Species... s){return java.util.Arrays.asList(s).contains(this);}}");
        add(s,"com.pixelmonmod.api.registry.RegistryValue","public class RegistryValue<T> {public T value;public RegistryValue(T v){value=v;}public T getValueUnsafe(){return value;}public T orElse(T fallback){return value==null?fallback:value;}}");
        add(s,API+"api.registries.PixelmonSpecies","public class PixelmonSpecies {public static java.util.List<"+API+"api.pokemon.species.Species> all=new java.util.ArrayList<>();public static java.util.List<"+API+"api.pokemon.species.Species> getAll(){return all;}public static java.util.Optional<com.pixelmonmod.api.registry.RegistryValue<"+API+"api.pokemon.species.Species>> get(String id){return all.stream().filter(p->p.getName().equalsIgnoreCase(id)).findFirst().map(com.pixelmonmod.api.registry.RegistryValue::new);}}");
        add(s,"net.minecraft.util.IItemProvider","public interface IItemProvider {}");
        add(s,"net.minecraft.util.ResourceLocation","public class ResourceLocation {public String id;public ResourceLocation(String id){if(!id.matches(\"[a-z0-9_:/.-]+\"))throw new IllegalArgumentException(id);this.id=id;}public String toString(){return id;}}");
        add(s,"net.minecraft.item.Item","public class Item implements net.minecraft.util.IItemProvider {public String id,label;public Item(String i,String l){id=i;label=l;}public net.minecraft.util.ResourceLocation getRegistryName(){return new net.minecraft.util.ResourceLocation(id);}}");
        add(s,API+"items.HeldItem","public class HeldItem extends net.minecraft.item.Item {public HeldItem(String id,String label){super(id,label);}}");
        add(s,"net.minecraft.util.text.ITextComponent","public interface ITextComponent {String getString();}");
        add(s,"net.minecraft.item.ItemStack","public class ItemStack {public static final ItemStack field_190927_a=new ItemStack(null);public Item item;public ItemStack(net.minecraft.util.IItemProvider i){item=(Item)i;}public net.minecraft.util.text.ITextComponent func_200301_q(){return ()->item.label;}public boolean func_190926_b(){return item==null;}}");
        add(s,API+"init.registry.ItemRegistration","public class ItemRegistration {public static java.util.List<net.minecraft.item.Item> all=new java.util.ArrayList<>();public static java.util.List<net.minecraft.item.Item> getAllItems(){return all;}}");
        add(s,API+"api.pokemon.item.pokeball.PokeBall","public class PokeBall implements "+API+"api.util.ITranslatable {public String name;public PokeBall(String n){name=n;}public String getName(){return name;}public String getTranslationKey(){return name;}public String getLocalizedName(){return fixture.Data.label(\"balls\",name);}public net.minecraft.item.ItemStack getBallItem(){return new net.minecraft.item.ItemStack(new net.minecraft.item.Item(\"pixelmon:\"+name,getLocalizedName()));}public boolean is(PokeBall... b){return java.util.Arrays.asList(b).contains(this);}}");
        add(s,API+"api.pokemon.item.pokeball.PokeBallRegistry","public class PokeBallRegistry {public static java.util.List<PokeBall> all=new java.util.ArrayList<>();public static java.util.List<PokeBall> getAll(){return all;}public static com.pixelmonmod.api.registry.RegistryValue<PokeBall> getPokeBall(String id){return new com.pixelmonmod.api.registry.RegistryValue<>(all.stream().filter(p->p.name.equalsIgnoreCase(id)).findFirst().orElse(null));}}");
        add(s,API+"battles.attacks.ImmutableAttack","public class ImmutableAttack implements "+API+"api.util.ITranslatable {public String name;public ImmutableAttack(String n){name=n;}public String getAttackName(){return name;}public String getTranslationKey(){return name;}public String getLocalizedName(){return fixture.Data.label(\"moves\",name);}}");
        add(s,API+"api.battles.attack.AttackRegistry","public class AttackRegistry {public static java.util.List<"+API+"battles.attacks.ImmutableAttack> all=new java.util.ArrayList<>();public static java.util.List<"+API+"battles.attacks.ImmutableAttack> getAllAttacks(){return all;}public static java.util.Optional<"+API+"battles.attacks.ImmutableAttack> getAttackBase(String id){var direct=all.stream().filter(m->m.name.equalsIgnoreCase(id)).findFirst();return direct.isPresent()?direct:all.stream().filter(m->m.getLocalizedName().equalsIgnoreCase(id)).findFirst();}}");
        add(s,API+"battles.attacks.Attack","public class Attack {public ImmutableAttack move;public int pp=20;public Attack(String name){move="+API+"api.battles.attack.AttackRegistry.getAttackBase(name).orElseThrow();}public ImmutableAttack getMove(){return move;}public static boolean hasAttack(String name){return "+API+"api.battles.attack.AttackRegistry.getAttackBase(name).isPresent();}}");
        add(s,API+"api.pokemon.stats.Moveset","public class Moveset extends java.util.ArrayList<"+API+"battles.attacks.Attack> {public "+API+"battles.attacks.Attack get(int i){return i>=size()?null:super.get(i);}public boolean add("+API+"battles.attacks.Attack a){return super.add(a);}}");
        add(s,API+"api.pokemon.Pokemon","public class Pokemon {public "+API+"api.pokemon.species.Species species;public "+API+"api.pokemon.item.pokeball.PokeBall ball;public "+API+"api.pokemon.stats.Moveset moves=new "+API+"api.pokemon.stats.Moveset();public "+API+"api.pokemon.stats.Moveset getMoveset(){return moves;}public void setBall("+API+"api.pokemon.item.pokeball.PokeBall b){ball=b;}}");
        add(s,"net.minecraft.client.gui.widget.TextFieldWidget","public class TextFieldWidget {public String text=\"\";public String func_146179_b(){return text;}public void func_146180_a(String s){text=s;}}");
        add(s,API+"client.gui.widgets.TabCompleteTextField","public class TabCompleteTextField extends net.minecraft.client.gui.widget.TextFieldWidget {}");
        add(s,API+"client.gui.widgets.TabCompleteTranslateableTextField","public class TabCompleteTranslateableTextField extends net.minecraft.client.gui.widget.TextFieldWidget {public "+API+"api.util.ITranslatable value;public void setValue("+API+"api.util.ITranslatable v){value=v;func_146180_a(v.getLocalizedName());}public "+API+"api.util.ITranslatable getTranslatableValue(){return value;}}");
        add(s,"net.minecraft.client.gui.screen.Screen","public class Screen {}");
        add(s,"net.minecraft.client.Minecraft","public class Minecraft {public static Minecraft INSTANCE=new Minecraft();public net.minecraft.client.gui.screen.Screen field_71462_r;public static Minecraft func_71410_x(){return INSTANCE;}}");
        add(s,BASE,"public class IndividualEditorBaseScreen extends net.minecraft.client.gui.screen.Screen {public "+API+"api.pokemon.Pokemon p=new "+API+"api.pokemon.Pokemon();public int changes;private "+API+"client.gui.widgets.TabCompleteTranslateableTextField tbName=new "+API+"client.gui.widgets.TabCompleteTranslateableTextField();private "+API+"client.gui.widgets.TabCompleteTextField[] tbMoves=new "+API+"client.gui.widgets.TabCompleteTextField[4];public IndividualEditorBaseScreen(){tbName.setValue("+API+"api.registries.PixelmonSpecies.all.get(0));for(int i=0;i<4;i++)tbMoves[i]=new "+API+"client.gui.widgets.TabCompleteTextField();}private void lambda$init$3(String s){throw new AssertionError();}protected boolean checkFields(){throw new AssertionError();}protected void changePokemon("+API+"api.pokemon.species.Species s){p.species=s;changes++;}public void name(String s){lambda$init$3(s);}public void move(int i,String s){tbMoves[i].func_146180_a(s);}public boolean save(){return checkFields();}}");
        add(s,BALL,"public class PokemonEditorIndividualScreen extends IndividualEditorBaseScreen {private "+API+"client.gui.widgets.TabCompleteTranslateableTextField pokeBallText=new "+API+"client.gui.widgets.TabCompleteTranslateableTextField();private net.minecraft.item.ItemStack pokeBall;public PokemonEditorIndividualScreen(){pokeBallText.setValue("+API+"api.pokemon.item.pokeball.PokeBallRegistry.all.get(0));}private void lambda$init$0(String s){throw new AssertionError();}private void updatePokeBall(){throw new AssertionError();}public void ball(String s){lambda$init$0(s);}}");
        StringBuilder enumValues=new StringBuilder();for(String name:natures.keySet()){if(enumValues.length()>0)enumValues.append(',');enumValues.append(name);}add(s,API+"api.pokemon.Nature","public enum Nature {"+enumValues+";public String getLocalizedName(){return fixture.Data.label(\"natures\",name());}public static Nature natureFromString(String name){throw new AssertionError();}}");
        add(s,"fixture.Completion","public class Completion extends local.pixelmon.backport.mixin.EditorCompletionMixin {}");
        add(s,"fixture.Advanced","public class Advanced extends local.pixelmon.backport.mixin.EditorAdvancedInputMixin {}");
        s.put("fixture.EditorChineseCases",Files.readString(Path.of("tests/resources/EditorChineseCases.java.txt")));
        Path dir=Files.createTempDirectory(Path.of(".cache"),"editor-chinese-");var options=new ArrayList<String>(List.of("--release","11","-proc:none","-encoding","UTF-8","-cp",System.getProperty("java.class.path"),"-d",dir.toString()));
        for(var e:s.entrySet()){var p=dir.resolve(e.getKey().replace('.','/')+".java");Files.createDirectories(p.getParent());Files.writeString(p,e.getValue());options.add(p.toString());}
        check(ToolProvider.getSystemJavaCompiler().run(null,null,null,options.toArray(new String[0]))==0,"Editor fixture compilation failed");
        replace(dir.resolve(API.replace('.','/')+"api/pokemon/Nature.class"),node(read(jar,API+"api.pokemon.Nature")),m->m.name.equals("natureFromString"));
        Path basePath=dir.resolve(BASE.replace('.','/')+".class"),ballPath=dir.resolve(BALL.replace('.','/')+".class");byte[] baseStub=Files.readAllBytes(basePath),ballStub=Files.readAllBytes(ballPath);
        for(boolean patched:new boolean[]{false,true}){
            Files.write(basePath,baseStub);Files.write(ballPath,ballStub);
            replace(basePath,node(patched?base:read(jar,BASE)),m->m.name.equals("lambda$init$3")||m.name.equals("checkFields")||m.name.contains("backport$speciesName")||m.name.contains("backport$moveName"));
            replace(ballPath,node(patched?ball:read(jar,BALL)),m->m.name.equals("lambda$init$0")||m.name.equals("updatePokeBall")||m.name.contains("backport$ballName"));
            Set<String> production=Set.of("local.pixelmon.backport.client.EditorNames","local.pixelmon.backport.mixin.EditorCompletionMixin","local.pixelmon.backport.mixin.EditorAdvancedInputMixin");
            try(var loader=new URLClassLoader(new URL[]{dir.toUri().toURL()},EditorChineseVerification.class.getClassLoader()){
                protected Class<?> loadClass(String n,boolean resolve)throws ClassNotFoundException{synchronized(getClassLoadingLock(n)){Class<?> c=findLoadedClass(n);String parent=n.contains("$")?n.substring(0,n.indexOf('$')):n;if(c==null&&s.containsKey(parent))c=findClass(n);if(c==null&&production.contains(n))try(var in=getParent().getResourceAsStream(n.replace('.','/')+".class")){byte[] b=in.readAllBytes();c=defineClass(n,b,0,b.length);}catch(java.io.IOException ex){throw new ClassNotFoundException(n,ex);}if(c==null)c=super.loadClass(n,false);if(resolve)resolveClass(c);return c;}}
            }){loader.loadClass("fixture.EditorChineseCases").getMethod("run",String.class,boolean.class).invoke(null,tables.toString(),patched);}
        }
        System.out.println("PASS original/patched editor species and ball responders, actual native move save with existing PP identity, item alias before ResourceLocation parsing, native 25-nature Chinese parsing, dynamic language change, bilingual Tab scope and invalid/ambiguous inputs; no game initialized.");
    }
    static void recordCheck(){
        List<String[]> entries=List.of(new String[]{"first","同名"},new String[]{"second","同名"},new String[]{"third","唯一"});
        check(EditorNameAliases.resolve("同名",entries,e->e[0],e->e[1]).equals("同名"),"Ambiguous name silently selected");
        check(EditorNameAliases.resolve("未登记",entries,e->e[0],e->e[1]).equals("未登记"),"Unknown name invented");
        check(EditorNameAliases.resolve("唯",entries,e->e[0],e->e[1]).equals("唯"),"Prefix auto-committed");
        check(EditorNameAliases.resolve("  THIRD ",entries,e->e[0],e->e[1]).equals("third"),"Case/whitespace English input broken");
        check(EditorNameAliases.resolve(null,entries,e->e[0],e->e[1])==null,"Null input changed");
    }
}
