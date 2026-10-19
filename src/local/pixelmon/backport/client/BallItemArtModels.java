package local.pixelmon.backport.client;
import local.pixelmon.backport.*;
import net.minecraft.util.ResourceLocation;
import net.minecraft.client.renderer.model.ModelResourceLocation;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;
@Mod.EventBusSubscriber(modid=Content.NS,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class BallItemArtModels {
    private static boolean part(String name){return name.endsWith("_base")||name.endsWith("_disc");}
    @SubscribeEvent public static void register(ModelRegistryEvent event){for(String name:BallItemArt.FILES)if(part(name))ModelLoader.addSpecialModel(new ResourceLocation(Content.NS,"item/ball_parts/"+name));}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void bake(ModelBakeEvent event){
        for(String name:BallItemArt.FILES)if(part(name)){
            var key=new ModelResourceLocation("pixelmon:"+name,"inventory");
            var model=event.getModelRegistry().get(new ResourceLocation(Content.NS,"item/ball_parts/"+name));
            if(event.getModelRegistry().containsKey(key)&&model!=null)event.getModelRegistry().put(key,model);
        }
        com.pixelmonmod.pixelmon.client.models.items.PokeBallItemModel.OverrideList.MODELS.clear();
    }
    @SubscribeEvent public static void stitch(TextureStitchEvent.Pre event){
        if(!event.getMap().func_229223_g_().equals(net.minecraft.client.renderer.texture.AtlasTexture.field_110575_b))return;
        for(String name:BallItemArt.FILES)event.addSprite(new ResourceLocation(Content.NS,"items/pokeballs/"+name));
    }
}
