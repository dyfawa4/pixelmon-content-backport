package local.pixelmon.backport;
import net.minecraft.client.audio.ISound;
import net.minecraft.util.ResourceLocation;

/** Music events only; Pokemon cries, battle effects and block sounds are excluded. */
public final class PokemonMusic {
    public interface Gain {void backport$music(boolean music);void backport$refreshMusic();}
    private PokemonMusic(){}
    private static boolean music(ResourceLocation id){
        if(id==null)return false;
        String ns=id.func_110624_b(),path=id.func_110623_a();
        return (ns.equals("pixelmon")||ns.equals(Content.NS)||ns.equals("pixeltweaks"))
            &&(path.startsWith("pixelmon.music.")||path.startsWith("music.")||path.startsWith("music/"));
    }
    public static boolean isMusic(ISound sound){
        if(sound instanceof IndependentBattleSound || music(sound.func_147650_b()))return true;
        return sound.func_184364_b()!=null && music(sound.func_184364_b().func_188719_a());
    }
}
