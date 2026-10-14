package local.pixelmon.backport.client;

import java.util.*;
import local.pixelmon.backport.NpcLocalization;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.gui.screen.Screen;

/** Translate complete dialogue pages before the shared helper wraps or formats them. */
public final class NpcDialogueDisplay {
    private NpcDialogueDisplay(){}
    public static boolean applies(Screen screen){return screen!=null && applies(screen.getClass().getName());}
    public static boolean applies(String type){return type.startsWith("com.pixelmonmod.pixelmon.client.gui.npc.") || type.startsWith("com.pixelmonmod.pixelmon.client.gui.custom.dialogue.");}
    public static String text(String value){
        String locale=Minecraft.func_71410_x().field_71474_y.field_74363_ab;
        String translated=NpcLocalization.translate(value,locale);
        if(value==null || !Objects.equals(value,translated))return translated;
        return I18n.func_188566_a(value)?I18n.func_135052_a(value):value;
    }
    public static List<String> pages(List<String> source){
        if(source==null)return null;
        List<String> result=new ArrayList<>(source.size());for(String page:source)result.add(text(page));return result;
    }
}
