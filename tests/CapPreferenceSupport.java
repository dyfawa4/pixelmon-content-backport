import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Isolate storage/text services while executing shipped preference and multiplier methods. */
public final class CapPreferenceSupport {
 static final String P="com.pixelmonmod.pixelmon.api.pokemon.";
 static void add(Map<String,String> sources,String name,String body){sources.put(name,"package "+name.substring(0,name.lastIndexOf('.'))+";"+body);}
 static String body(String source,String members){return source.replace("public class ","public class ").replaceFirst("\\{",java.util.regex.Matcher.quoteReplacement("{"+members));}
 static void prepare(Map<String,String> sources){
  add(sources,"net.minecraft.nbt.CompoundNBT","public class CompoundNBT {public final java.util.Map<String,String> values=new java.util.HashMap<>();public String func_74779_i(String k){return values.getOrDefault(k,\"\");}public void func_74778_a(String k,String v){values.put(k,v);}}");
  sources.compute(P+"Pokemon",(k,v)->body(v,"public java.util.UUID id=java.util.UUID.randomUUID();public boolean egg,stored;public int friendship=70;public net.minecraft.entity.player.ServerPlayerEntity owner;public net.minecraft.entity.player.ServerPlayerEntity getOwnerPlayer(){return owner;}public void setFriendship(int value){friendship=value;}public net.minecraft.nbt.CompoundNBT data=new net.minecraft.nbt.CompoundNBT();public java.util.UUID getUUID(){return id;}public boolean isEgg(){return egg;}public net.minecraft.nbt.CompoundNBT getPersistentData(){return data;}"));
  add(sources,"local.pixelmon.backport.AcquisitionAdvancements","public class AcquisitionAdvancements {public static boolean ownedStored(net.minecraft.entity.player.ServerPlayerEntity owner,"+P+"Pokemon p){return owner!=null&&p.owner==owner&&p.stored;}}");
  if(!sources.containsKey(P+"item.pokeball.PokeBall"))add(sources,P+"item.pokeball.PokeBall","public class PokeBall {public String kind;public PokeBall(String s){kind=s;}}");
  sources.compute(P+"item.pokeball.PokeBall",(k,v)->body(v,"public String getName(){return kind;}public String getLocalizedName(){return kind;}"));
  sources.putIfAbsent("com.pixelmonmod.api.registry.RegistryValue","package com.pixelmonmod.api.registry;public class RegistryValue<T> {public T value;public RegistryValue(T v){value=v;}}");
  sources.compute("com.pixelmonmod.api.registry.RegistryValue",(k,v)->body(v,"public T getValueUnsafe(){return value;}"));
  if(!sources.containsKey(P+"item.pokeball.PokeBallRegistry"))add(sources,P+"item.pokeball.PokeBallRegistry","public class PokeBallRegistry {}");
  sources.compute(P+"item.pokeball.PokeBallRegistry",(k,v)->body(v,"public static com.pixelmonmod.api.registry.RegistryValue<PokeBall> getPokeBall(String n){return new com.pixelmonmod.api.registry.RegistryValue<>(new PokeBall(n));}"));
  sources.compute("net.minecraft.util.text.TranslationTextComponent",(k,v)->body(v.replace("public TranslationTextComponent(String s)","public TranslationTextComponent(String s,Object... args)").replace("public TranslationTextComponent(String k)","public TranslationTextComponent(String k,Object... args)"),"public TranslationTextComponent(String text){this(text,new Object[0]);}"));
  add(sources,"net.minecraft.util.text.TextFormatting","public enum TextFormatting {GREEN}");
  add(sources,"net.minecraft.util.text.IFormattableTextComponent","public interface IFormattableTextComponent extends ITextComponent {}");
  add(sources,"net.minecraft.util.text.StringTextComponent","public class StringTextComponent implements IFormattableTextComponent {public String text;public TextFormatting color;public StringTextComponent(String value){text=value;}public IFormattableTextComponent func_240699_a_(TextFormatting value){color=value;return this;}}");
 }
 static byte[] preference()throws Exception{
  var node=Version13StaticVerification.node("local/pixelmon/backport/CapBallPreference");
  node.methods.removeIf(m->!Set.of("cap","selected","forbidden","preferred","multiplier","announce","stored","<clinit>").contains(m.name));
  var writer=new ClassWriter(0);node.accept(writer);return writer.toByteArray();
 }
 static byte[] burst()throws Exception{
  var node=Version13StaticVerification.node("local/pixelmon/backport/BurstCapture");node.methods.removeIf(m->!Set.of("forbidden","bonus").contains(m.name));
  var active=new MethodNode(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"active","(Lnet/minecraft/entity/player/ServerPlayerEntity;Lcom/pixelmonmod/pixelmon/api/pokemon/Pokemon;)Z",null,null);active.instructions.add(new InsnNode(Opcodes.ICONST_0));active.instructions.add(new InsnNode(Opcodes.IRETURN));active.maxStack=1;active.maxLocals=2;node.methods.add(active);
  var writer=new ClassWriter(0);node.accept(writer);return writer.toByteArray();
 }
}
