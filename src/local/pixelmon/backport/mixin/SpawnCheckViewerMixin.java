package local.pixelmon.backport.mixin;

import java.util.List;
import com.pixelmonmod.pixelmon.api.spawning.archetypes.algorithms.checkspawns.LegendaryCheckSpawns;
import local.pixelmon.backport.SpawnChecks;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.management.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(value=LegendaryCheckSpawns.class,remap=false)
public abstract class SpawnCheckViewerMixin {
    @Redirect(method="checkSpawns",at=@At(value="INVOKE",target="Lnet/minecraft/server/management/PlayerList;func_181057_v()Ljava/util/List;"))
    private List<ServerPlayerEntity> backport$ownArea(PlayerList players,com.pixelmonmod.pixelmon.api.spawning.AbstractSpawner spawner,net.minecraft.command.CommandSource source,List<String> filters){return SpawnChecks.players(players.func_181057_v(),source);}
}
