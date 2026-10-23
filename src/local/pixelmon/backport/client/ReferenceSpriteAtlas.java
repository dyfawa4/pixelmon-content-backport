package local.pixelmon.backport.client;

import local.pixelmon.backport.Content;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Native sprite items use the block atlas, while party portraits load files directly. */
@Mod.EventBusSubscriber(modid=Content.NS,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class ReferenceSpriteAtlas {
    private ReferenceSpriteAtlas(){}
    @SubscribeEvent public static void stitch(TextureStitchEvent.Pre event){
        if(!event.getMap().func_229223_g_().equals(AtlasTexture.field_110575_b))return;
        // Pixelmon only scans textures/pokemon; imported Mega portraits live under reference.
        for(ResourceLocation file:Minecraft.func_71410_x().func_195551_G().func_199003_a(
                "textures/reference/pokemon",name->name.endsWith("sprite.png"))){
            if(!Content.NS.equals(file.func_110624_b()))continue;
            String path=file.func_110623_a();
            event.addSprite(new ResourceLocation(Content.NS,path.substring("textures/".length(),path.length()-4)));
        }
    }
}
