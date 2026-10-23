package local.pixelmon.backport.client;
import local.pixelmon.backport.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.model.*;
import net.minecraft.client.renderer.entity.layers.BipedArmorLayer;
import net.minecraft.util.ResourceLocation;
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid=Content.NS,value=net.minecraftforge.api.distmarker.Dist.CLIENT,bus=net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD)
public final class AuthorBossRenderer extends BipedRenderer<AuthorCombatBoss,PlayerModel<AuthorCombatBoss>> {
    public AuthorBossRenderer(EntityRendererManager manager){super(manager,new PlayerModel<>(0,false),.5f);func_177094_a(new BipedArmorLayer<>(this,new BipedModel<>(.5f),new BipedModel<>(1)));}
    @Override public ResourceLocation func_110775_a(AuthorCombatBoss boss){return new ResourceLocation("pixelmon","textures/steve/mystery_task_man.png");}
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event){net.minecraftforge.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(Backport.AUTHOR_COMBAT.get(),AuthorBossRenderer::new);}
}
