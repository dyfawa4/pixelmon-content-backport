package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.entities.npcs.NPCEntity;
import com.pixelmonmod.pixelmon.entities.npcs.registry.BaseTrainer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Native -1 means resource-pack skin, not a list index. Never change the saved NPC appearance. */
@Mixin(targets="com.pixelmonmod.pixelmon.client.gui.npcEditor.TextureEditorNPC",remap=false)
public abstract class NpcTextureEditorMixin {
    @Shadow private NPCEntity npc;
    @Shadow private BaseTrainer model;
    @Inject(method="isCustomTexture",at=@At("HEAD"),cancellable=true)
    private void backport$custom(CallbackInfoReturnable<Boolean> ci){
        int index=npc.getTextureIndex();
        if(index==-1){ci.setReturnValue(true);return;}
        if(index<0||model==null||index>=model.textures.size())ci.setReturnValue(false);
    }
    @Redirect(method="displayTextureMenu",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/entities/npcs/NPCEntity;getTextureIndex()I"))
    private int backport$selection(NPCEntity entity){
        int index=entity.getTextureIndex();
        if(index>=0&&index<model.textures.size())return index;
        int custom=model.textures.indexOf("Custom_RP");
        return index==-1&&custom>=0?custom:0;
    }
}
