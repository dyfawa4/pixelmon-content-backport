package local.pixelmon.backport.mixin;
import local.pixelmon.backport.*;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.entities.DenEntity;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import com.pixelmonmod.pixelmon.battles.raids.*;
import com.pixelmonmod.pixelmon.battles.controller.participants.RaidPixelmonParticipant;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=RaidData.class,remap=false)
public abstract class TeraRaidDataMixin {
    @Redirect(method="startRaid",at=@At(value="NEW",target="com/pixelmonmod/pixelmon/battles/controller/participants/RaidPixelmonParticipant",ordinal=1))
    private RaidPixelmonParticipant backport$teraFallback(RaidData raid,RaidSettings settings,Pokemon pokemon,DenEntity den){return den instanceof TeraDenEntity?new TeraRaidParticipant(raid,(TeraDenEntity)den,pokemon):new RaidPixelmonParticipant(raid,settings,pokemon);}
    @Redirect(method="startRaid",at=@At(value="NEW",target="com/pixelmonmod/pixelmon/battles/controller/participants/RaidPixelmonParticipant",ordinal=0))
    private RaidPixelmonParticipant backport$tera(RaidData raid,RaidSettings settings,PixelmonEntity pokemon,DenEntity den){return den instanceof TeraDenEntity?new TeraRaidParticipant(raid,(TeraDenEntity)den,pokemon):new RaidPixelmonParticipant(raid,settings,pokemon);}
}
