package local.pixelmon.backport.client;

import java.util.*;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.IResource;
import net.minecraft.util.ResourceLocation;

/** Procedural facets retain the source skin's alpha, including transparent fins
 * and wings. Loaded only for visible Tera skins; cache eviction is between frames. */
final class TeraFacetTextures {
    private static final Map<ResourceLocation,ResourceLocation> CACHE=new LinkedHashMap<>(16,.75f,true);
    private static int sequence;
    private TeraFacetTextures(){}
    static ResourceLocation get(ResourceLocation original){
        ResourceLocation result=CACHE.get(original);if(result!=null)return result;
        Minecraft mc=Minecraft.func_71410_x();
        try(IResource source=mc.func_195551_G().func_199002_a(original);
            NativeImage skin=NativeImage.func_195713_a(source.func_199027_b())){
            NativeImage image=new NativeImage(skin.func_195702_a(),skin.func_195714_b(),true);
            for(int y=0;y<image.func_195714_b();y++)for(int x=0;x<image.func_195702_a();x++){
                int skinPixel=skin.func_195709_a(x,y);
                // Facet lighting is calculated from the animated triangles.
                // UV-space patterns stretch into lines on necks and wings.
                image.func_195700_a(x,y,(skinPixel&0xff000000)|0xffffff);
            }
            result=new ResourceLocation(local.pixelmon.backport.Content.NS,"tera/facet_cache/"+(sequence++));
            mc.func_110434_K().func_229263_a_(result,new DynamicTexture(image));
        }catch(Exception error){
            // Dynamic/online skins may not have a resource file. Reusing the
            // original texture preserves transparency and still receives tint.
            result=original;
        }
        CACHE.put(original,result);return result;
    }
    static void trim(){
        Iterator<Map.Entry<ResourceLocation,ResourceLocation>> entries=CACHE.entrySet().iterator();
        while(CACHE.size()>64 && entries.hasNext()){Map.Entry<ResourceLocation,ResourceLocation> e=entries.next();release(e);entries.remove();}
    }
    static void clear(){for(Map.Entry<ResourceLocation,ResourceLocation> e:CACHE.entrySet())release(e);CACHE.clear();}
    private static void release(Map.Entry<ResourceLocation,ResourceLocation> entry){
        if(!entry.getKey().equals(entry.getValue()))Minecraft.func_71410_x().func_110434_K().func_147645_c(entry.getValue());
    }
}
