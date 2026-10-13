package local.pixelmon.backport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
/** A private key avoids an external language pack restoring the obsolete GUI title. */
public final class InfuserNames {
    private InfuserNames(){}
    public static String translate(String key,Object[] args){
        boolean machine=key.equals("container.infuser") || key.equals("jei.pixeltweaks.infuser.title");
        return I18n.func_135052_a(machine && "zh_cn".equalsIgnoreCase(Minecraft.func_71410_x().field_71474_y.field_74363_ab)
            ?"pixelmon_backport.infuser.name":key,args);
    }
}
