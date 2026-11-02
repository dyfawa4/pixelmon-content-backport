package local.pixelmon.backport.client;
import local.pixelmon.backport.*;
import net.minecraft.util.ResourceLocation;
import net.minecraft.client.renderer.model.ModelResourceLocation;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;
/** Bind reviewed legacy IDs to owned models independently of the shared Pixelmon resource namespace order. */
@Mod.EventBusSubscriber(modid=Content.NS,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class UpstreamItemArtModels {
    @SubscribeEvent public static void register(ModelRegistryEvent event){
        for(var value:Content.read("backport/upstream_art_180.json").getAsJsonArray("items"))ModelLoader.addSpecialModel(new ResourceLocation(value.getAsJsonObject().get("model").getAsString()));
        ModelLoader.addSpecialModel(new ResourceLocation(Content.NS,"block/upstream942/max_mushroom"));
        ModelLoader.addSpecialModel(new ResourceLocation(Content.NS,"item/delta_orb"));
        ModelLoader.addSpecialModel(new ResourceLocation(Content.NS,"block/delta_orb"));
        for(int i=1;i<=9;i++)ModelLoader.addSpecialModel(new ResourceLocation(Content.NS,"item/delta_orb_"+i));
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void bake(ModelBakeEvent event){
        var registry=event.getModelRegistry();
        var deltaBlock=registry.get(new ResourceLocation(Content.NS,"block/delta_orb"));
        for(var state:com.pixelmonmod.pixelmon.api.registries.PixelmonBlocks.jade_orb.func_176194_O().func_177619_a())registry.put(net.minecraft.client.renderer.BlockModelShapes.func_209554_c(state),deltaBlock);
        var delta=registry.get(new ResourceLocation(Content.NS,"item/delta_orb"));if(delta==null)throw new IllegalStateException("Missing Delta Orb model");registry.put(new ModelResourceLocation("pixelmon:jade_orb","inventory"),delta);
        for(var value:Content.read("backport/upstream_art_180.json").getAsJsonArray("items")){
            var row=value.getAsJsonObject();var model=registry.get(new ResourceLocation(row.get("model").getAsString()));
            if(model==null)throw new IllegalStateException("Missing reviewed item model "+row.get("id"));
            registry.put(new ModelResourceLocation(row.get("id").getAsString(),"inventory"),model);
        }
        var mushroom=registry.get(new ResourceLocation(Content.NS,"block/upstream942/max_mushroom"));
        for(var key:new java.util.ArrayList<>(registry.keySet()))if(key instanceof ModelResourceLocation&&(key.toString().startsWith("pixelmon:max_mushroom#")||key.toString().startsWith("pixelmon:max_mushroom_block#")))registry.put(key,mushroom);
    }
}
