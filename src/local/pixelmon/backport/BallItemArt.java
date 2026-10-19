package local.pixelmon.backport;
import java.util.*;
import net.minecraft.util.ResourceLocation;
/** Dedicated namespace prevents original mod pack order from masking the reference art. */
public final class BallItemArt {
    public static final Set<String> FILES=new LinkedHashSet<>();
    static {for(var v:Content.read("backport/pokeball-item-art-source.json").getAsJsonArray("ownedFiles"))FILES.add(v.getAsString());}
    private BallItemArt(){}
    public static ResourceLocation sprite(ResourceLocation original){
        if(original!=null && "pixelmon".equals(original.func_110624_b()) && original.func_110623_a().startsWith("items/pokeballs/")){
            String name=original.func_110623_a().substring("items/pokeballs/".length());
            if(FILES.contains(name))return new ResourceLocation(Content.NS,"items/pokeballs/"+name);
        }
        return original;
    }
}
