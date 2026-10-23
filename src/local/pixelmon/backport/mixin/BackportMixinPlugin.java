package local.pixelmon.backport.mixin;
import org.spongepowered.asm.mixin.extensibility.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.util.*;

public final class BackportMixinPlugin implements IMixinConfigPlugin {
    private static final String WRAPPER="com/pixelmonmod/pixelmon/battles/controller/participants/PixelmonWrapper";
    private static final String GLOBAL="com/pixelmonmod/pixelmon/battles/controller/GlobalStatusController";
    private static final String WEATHER="com/pixelmonmod/pixelmon/battles/status/Weather";
    private static final String HOOK="local/pixelmon/backport/UmbrellaHooks";
    public void onLoad(String p){} public String getRefMapperConfig(){return null;}
    public boolean shouldApplyMixin(String t,String m){
        if(m.endsWith("RaidCatchClockMixin") && getClass().getClassLoader().getResource("local/raidcatchfix/PixelmonRaidCatchFix.class")!=null)return false;
        if(t.startsWith("net.blay09.mods.trashslot."))return getClass().getClassLoader().getResource(t.replace('.','/')+".class")!=null;
        if(t.startsWith("snownee.jade."))return getClass().getClassLoader().getResource(t.replace('.','/')+".class")!=null;
        if(t.startsWith("com.strangeone101.pixeltweaks."))return getClass().getClassLoader().getResource(t.replace('.','/')+".class")!=null
            && getClass().getClassLoader().getResource("mezz/jei/api/recipe/category/IRecipeCategory.class")!=null;
        if(t.startsWith("vectorwing.farmersdelight.") || t.startsWith("pixelmondetector.") || t.startsWith("com.mrcrayfish.configured.") || t.startsWith("com.chaosthedude.explorerscompass.") || t.startsWith("com.chaosthedude.naturescompass.") || t.startsWith("noobanidus.mods.lootr.") || t.startsWith("pokelucky.") || t.startsWith("dev.ftb.mods.ftbquests."))return getClass().getClassLoader().getResource(t.replace('.','/')+".class")!=null;
        return true;
    }
    public void acceptTargets(Set<String> mine,Set<String> other){}
    public List<String> getMixins(){return null;}
    public void preApply(String t,ClassNode c,String m,IMixinInfo i){}
    public void postApply(String target,ClassNode c,String mixin,IMixinInfo info){
        if(mixin.endsWith("DayCareRayTraceMixin")){
            int patched=0;
            for(MethodNode method:c.methods)if(method.name.equals("func_217297_a")&&method.desc.equals("(Lnet/minecraft/util/math/RayTraceContext;Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/util/math/BlockRayTraceResult;")){
                int slot=method.maxLocals++;
                for(AbstractInsnNode instruction:method.instructions.toArray())if(instruction.getOpcode()==Opcodes.ARETURN){
                    InsnList hook=new InsnList();hook.add(new VarInsnNode(Opcodes.ASTORE,slot));hook.add(new VarInsnNode(Opcodes.ALOAD,0));hook.add(new VarInsnNode(Opcodes.ALOAD,1));hook.add(new VarInsnNode(Opcodes.ALOAD,2));hook.add(new VarInsnNode(Opcodes.ALOAD,slot));
                    hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"local/pixelmon/backport/DayCareSelection","backport$upper","(Lnet/minecraft/world/IBlockReader;Lnet/minecraft/util/math/RayTraceContext;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/BlockRayTraceResult;)Lnet/minecraft/util/math/BlockRayTraceResult;",false));method.instructions.insertBefore(instruction,hook);patched++;
                }
                method.maxStack=Math.max(method.maxStack,4);
            }
            if(patched!=1)throw new IllegalStateException("Daycare ray traversal hook count: "+patched);
            return;
        }
        if(!mixin.endsWith("UmbrellaConsumerMixin"))return;
        for(MethodNode m:c.methods){
            int slot=(m.access&Opcodes.ACC_STATIC)!=0?0:1,holder=-1,second=-1,weather=-1;
            for(Type type:Type.getArgumentTypes(m.desc)){
                if(type.getSort()==Type.OBJECT && type.getInternalName().equals(WRAPPER)){if(holder<0)holder=slot;else second=slot;}
                if(type.getSort()==Type.OBJECT && type.getInternalName().equals(WEATHER))weather=slot;
                slot+=type.getSize();
            }
            if(holder<0)continue;
            // Allies' Flower Gift and Protosynthesis are intentionally not
            // suppressed by the recipient's umbrella, matching upstream tests.
            if(target.endsWith("FlowerGift") && m.name.contains("Teammate"))continue;
            if((target.endsWith("AlwaysHit") || target.endsWith("Freeze")) && second>=0)holder=second;
            if(m.name.equals("onWeatherChange") && weather>=0){
                InsnList inject=new InsnList();inject.add(new VarInsnNode(Opcodes.ALOAD,holder));inject.add(new VarInsnNode(Opcodes.ALOAD,weather));
                inject.add(new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"backport$filter","(L"+WRAPPER+";L"+WEATHER+";)L"+WEATHER+";",false));inject.add(new VarInsnNode(Opcodes.ASTORE,weather));m.instructions.insert(inject);
            }
            for(AbstractInsnNode n:m.instructions.toArray())if(n instanceof MethodInsnNode){
                MethodInsnNode call=(MethodInsnNode)n;if(!GLOBAL.equals(call.owner))continue;
                String hook=call.name.equals("getWeather")?"backport$weather":call.name.equals("hasStatus")?"backport$hasStatus":call.name.equals("getGlobalStatus") && call.desc.startsWith("(L")?"backport$status":null;
                if(hook==null)continue;
                if(target.contains(".battles.attacks."))hook=hook.equals("backport$weather")?"backport$moveWeather":hook.equals("backport$hasStatus")?"backport$moveHasStatus":hook.equals("backport$status")?"backport$moveStatus":hook;
                if(target.endsWith("AlwaysHit") && call.name.equals("getWeather"))hook="backport$targetWeather";
                m.instructions.insertBefore(call,new VarInsnNode(Opcodes.ALOAD,holder));
                call.desc="(L"+GLOBAL+";"+call.desc.substring(1,call.desc.indexOf(')'))+"L"+WRAPPER+";"+call.desc.substring(call.desc.indexOf(')'));
                call.owner=HOOK;call.name=hook;call.setOpcode(Opcodes.INVOKESTATIC);call.itf=false;
            }
            m.maxStack+=2;
        }
    }
}
