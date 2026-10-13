import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.minecraft.command.*;
import net.minecraft.entity.player.ServerPlayerEntity;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;

/** Distributable map controls only. No automatic QA scenarios or report collectors. */
@Mod("backport_client_test")
public final class MapSupport {
    @Mod.EventBusSubscriber(modid="backport_client_test")
    public static final class Events {
        @SubscribeEvent public static void commands(RegisterCommandsEvent e){
            LiteralArgumentBuilder<CommandSource> root=Commands.func_197057_a("backportqa");
            for(String action:new String[]{"reset","end","raids","raidteam","careteam","breedteam","evolveteam","starterteam","nextteam","hurt","faint","showfirst"})
                root.then(Commands.func_197057_a("map").then(Commands.func_197057_a(action).executes(ctx->{
                    ServerPlayerEntity p=ctx.getSource().func_197035_h();if(!TestMaps.isMap(p))return 0;
                    if(!TestMaps.prepared)TestMaps.setup(p);TestMaps.action(p,action);return 1;
                })));
            e.getDispatcher().register(root);
            LiteralArgumentBuilder<CommandSource> gym=Commands.func_197057_a("backportqa");
            for(String action:new String[]{"overview","leader","city","center","shop","hub","team","end"})
                gym.then(Commands.func_197057_a("gym").then(Commands.func_197057_a(action).executes(ctx->GymTestMaps.action(ctx.getSource().func_197035_h(),action))));
            e.getDispatcher().register(gym);
            LiteralArgumentBuilder<CommandSource> cap=Commands.func_197057_a("backportqa");
            for(String action:new String[]{"team","end","cap","double","persist","skills","tera","motion","normal","fire","water","electric","grass","ice","fighting","poison","ground","flying","psychic","bug","rock","ghost","dragon","dark","steel","fairy"})
                cap.then(Commands.func_197057_a("cap").then(Commands.func_197057_a(action).executes(ctx->CapTestMaps.action(ctx.getSource().func_197035_h(),action))));
            e.getDispatcher().register(cap);
            LiteralArgumentBuilder<CommandSource> feature=Commands.func_197057_a("backportqa");
            for(String action:new String[]{"riding","capture_next","fusion_next","zygarde","flute","menu","mega","eternamax","special_field","forms","urshifu","galar","end"})
                feature.then(Commands.func_197057_a("feature").then(Commands.func_197057_a(action).executes(ctx->FeatureTestMaps.action(ctx.getSource().func_197035_h(),action))));
            e.getDispatcher().register(feature);
            e.getDispatcher().register(Commands.func_197057_a("backportqa").then(Commands.func_197057_a("feature").then(Commands.func_197057_a("spawn").then(Commands.func_197056_a("pokemon",StringArgumentType.greedyString()).executes(ctx->FeatureStations.spawn(ctx.getSource().func_197035_h(),StringArgumentType.getString(ctx,"pokemon")))))));
        }
        @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){
            if(e.phase!=TickEvent.Phase.END || ServerLifecycleHooks.getCurrentServer()==null)return;
            for(ServerPlayerEntity p:ServerLifecycleHooks.getCurrentServer().func_184103_al().func_181057_v())
                if(CapTestMaps.isMap(p)){CapTestMaps.tick(p);break;}else if(FeatureTestMaps.isMap(p)){FeatureTestMaps.tick(p);break;}else if(GymTestMaps.isMap(p)){GymTestMaps.tick(p);break;}else if(TestMaps.isMap(p)){TestMaps.tick(p);break;}
        }
    }
}
