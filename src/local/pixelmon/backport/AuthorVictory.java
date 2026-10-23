package local.pixelmon.backport;
import java.util.UUID;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;

public final class AuthorVictory {
    public static final UUID PROFILE=UUID.fromString("5c44f11d-53a8-4a13-aa60-f24fb70b1700");
    private AuthorVictory(){}
    public static void earned(ServerPlayerEntity player){
        var advancement=player.func_184102_h().func_191949_aK().func_192778_a(new ResourceLocation(Content.NS,"adventure/defeat_author"));
        var root=player.getPersistentData();var saved=root.func_74775_l("PlayerPersisted");
        // The caller has already claimed the unique trainer's real victory.
        // An old completed first-victory advancement proves one past win only.
        int previous=saved.func_74764_b("BackportAuthorVictories")?saved.func_74762_e("BackportAuthorVictories"):
            advancement!=null&&player.func_192039_O().func_192747_a(advancement).func_192105_a()?1:0;
        int count=Math.min(Integer.MAX_VALUE-1,Math.max(0,previous))+1;
        saved.func_74768_a("BackportAuthorVictories",count);root.func_218657_a("PlayerPersisted",saved);
        if(advancement!=null)player.func_192039_O().func_192750_a(advancement,"earned");
        if(count>=10){
            var challenge=player.func_184102_h().func_191949_aK().func_192778_a(new ResourceLocation(Content.NS,"adventure/defeat_author_10"));
            if(challenge!=null)player.func_192039_O().func_192750_a(challenge,"earned");
        }
    }
}
