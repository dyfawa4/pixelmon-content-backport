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
    @SubscribeEvent public static void register(ModelRegistryEvent event){for(String name:BallItemArt.FILES)ModelLoader.addSpecialModel(new ResourceLocation(Content.NS,"item/ball_parts/"+name));}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void bake(ModelBakeEvent event){
        // Advancement icons also use legacy items and untagged shared items.
        // Recreate the native NBT wrappers with an owned fallback, preserving ball identity.
        for(String name:BallItemArt.FILES){
            var key=new ModelResourceLocation("pixelmon:"+name,"inventory");
            var model=event.getModelRegistry().get(new ResourceLocation(Content.NS,"item/ball_parts/"+name));
            if(event.getModelRegistry().containsKey(key)&&model!=null){
                if(name.equals("poke_ball"))model=new com.pixelmonmod.pixelmon.client.models.items.PokeBallItemModel(model);
                if(name.equals("poke_ball_lid"))model=new com.pixelmonmod.pixelmon.client.models.items.PokeBallLidItemModel(model);
                event.getModelRegistry().put(key,model);
            }
        }
        com.pixelmonmod.pixelmon.client.models.items.PokeBallItemModel.OverrideList.MODELS.clear();
    }
    @SubscribeEvent public static void stitch(TextureStitchEvent.Pre event){
        if(!event.getMap().func_229223_g_().equals(net.minecraft.client.renderer.texture.AtlasTexture.field_110575_b))return;
        for(String name:BallItemArt.FILES)event.addSprite(new ResourceLocation(Content.NS,"items/pokeballs/"+name));
    }
}
