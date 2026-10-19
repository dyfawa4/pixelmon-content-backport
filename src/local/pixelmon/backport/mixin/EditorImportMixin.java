package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.util.helpers.NetworkHelper;
import com.pixelmonmod.pixelmon.client.ServerStorageDisplay;
import com.pixelmonmod.pixelmon.client.gui.pokemoneditor.IndividualEditorBaseScreen;
import com.pixelmonmod.pixelmon.client.gui.pokemoneditor.PokemonEditorIndividualScreen;
import com.pixelmonmod.pixelmon.comm.packetHandlers.pokemoneditor.UpdatePlayerPokemonPacket;
import local.pixelmon.backport.client.EditorImport;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Imported network Pokemon need not have a client-side storage attachment. */
@Mixin(value=PokemonEditorIndividualScreen.class,remap=false)
public abstract class EditorImportMixin extends IndividualEditorBaseScreen {
    @Unique private int backport$importSlot=-1;
    private EditorImportMixin(Pokemon pokemon,net.minecraft.util.text.ITextComponent title){super(pokemon,title);}
    @Inject(method="setPokemon([Lcom/pixelmonmod/pixelmon/api/pokemon/Pokemon;)V",at=@At("HEAD"),cancellable=true)
    private void backport$importToEditedSlot(Pokemon[] incoming,CallbackInfo ci){
        ci.cancel();
        if(incoming==null||incoming.length<1||incoming[0]==null||p==null)return;
        int resolved=EditorImport.slot(p,ServerStorageDisplay.editedPokemon);
        if(resolved>=0)backport$importSlot=resolved;
        if(backport$importSlot<0||backport$importSlot>=6){
            var player=net.minecraft.client.Minecraft.func_71410_x().field_71439_g;
            if(player!=null)player.func_146105_b(new net.minecraft.util.text.TranslationTextComponent("pixelmon.backport.editor.import_slot_missing"),false);
            return;
        }
        // Keep the explicit original slot across repeated imports. The native
        // server packet still validates editor permission and edited-player UUID.
        Pokemon previous=p;p=incoming[0];
        var attachment=previous.getStorageAndPosition();
        if(attachment!=null&&attachment.func_76341_a()!=null&&attachment.func_76340_b()!=null)
            p.setStorage(attachment.func_76341_a(),attachment.func_76340_b());
        NetworkHelper.sendToServer(new UpdatePlayerPokemonPacket(p,backport$importSlot));
    }
}
