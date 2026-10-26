package local.pixelmon.backport;

import java.lang.reflect.Method;
import java.util.List;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.*;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;

/** Optional Curios 4 API bridge. No Curios classes are linked when the mod is absent. */
public final class GogglesCurios {
    private static Object helper;private static Method find,stack;private static boolean failed;
    private GogglesCurios(){}
    public static void enqueue(InterModEnqueueEvent event){
        if(!ModList.get().isLoaded("curios"))return;
        try{
            var type=Class.forName("top.theillusivec4.curios.api.SlotTypeMessage$Builder");var builder=type.getConstructor(String.class).newInstance("head");
            type.getMethod("size",int.class).invoke(builder,1);Object message=type.getMethod("build").invoke(builder);
            InterModComms.sendTo("curios","register_type",()->message);
        }catch(ReflectiveOperationException e){throw new IllegalStateException("Cannot register Curios head slot",e);}
    }
    public static boolean goggles(ItemStack stack){return stack!=null&&!stack.func_190926_b()&&stack.func_77973_b() instanceof com.pixelmonmod.pixelmon.items.heldItems.SafetyGogglesItem;}
    public static ItemStack head(LivingEntity entity){
        if(entity==null||failed||!ModList.get().isLoaded("curios"))return ItemStack.field_190927_a;
        try{
            if(find==null){
                helper=Class.forName("top.theillusivec4.curios.api.CuriosApi").getMethod("getCuriosHelper").invoke(null);
                find=Class.forName("top.theillusivec4.curios.api.type.util.ICuriosHelper").getMethod("findCurios",LivingEntity.class,String[].class);
                stack=Class.forName("top.theillusivec4.curios.api.SlotResult").getMethod("getStack");
            }
            for(Object row:(List<?>)find.invoke(helper,entity,new String[]{"head"})){var item=(ItemStack)stack.invoke(row);if(goggles(item))return item;}
        }catch(ReflectiveOperationException e){failed=true;org.apache.logging.log4j.LogManager.getLogger("pixelmon_backport").error("Curios head goggles unavailable",e);}
        return ItemStack.field_190927_a;
    }
    public static ItemStack fogEquipment(ItemStack helmet,LivingEntity player){if(goggles(helmet))return helmet;var head=head(player);return goggles(head)?head:helmet;}
}
