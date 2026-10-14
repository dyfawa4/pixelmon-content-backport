package local.pixelmon.backport.mixin;

import local.pixelmon.backport.client.DetectorConfigText;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Pseudo @Mixin(targets="com.mrcrayfish.configured.client.screen.ConfigScreen",remap=false)
public abstract class DetectorConfigTitleMixin {
    @ModifyArg(method={"<init>(Lnet/minecraft/client/gui/screen/Screen;Lnet/minecraft/util/text/ITextComponent;Lnet/minecraft/util/ResourceLocation;Lcom/mrcrayfish/configured/api/IConfigEntry;)V", "<init>(Lnet/minecraft/client/gui/screen/Screen;Lnet/minecraft/util/text/ITextComponent;Lcom/mrcrayfish/configured/api/IModConfig;Lnet/minecraft/util/ResourceLocation;)V"},at=@At(value="INVOKE",target="Lcom/mrcrayfish/configured/client/screen/ListMenuScreen;<init>(Lnet/minecraft/client/gui/screen/Screen;Lnet/minecraft/util/text/ITextComponent;Lnet/minecraft/util/ResourceLocation;I)V"),index=1,require=2,allow=2)
    private static ITextComponent backport$title(ITextComponent original){
        String text=original.getString();
        if(!text.equals("PixelmonDetector") && !text.startsWith("PixelmonDetector > ")
            && !text.equals("Pixelmon Detector") && !text.startsWith("Pixelmon Detector > "))return original;
        String root=DetectorConfigText.text("pixelmondetector.config.category.root");
        if(root==null)return original;
        String[] parts=text.split(" > ",-1);
        parts[0]=root;
        for(int i=1;i<parts.length;i++){
            String name=parts[i].equals("General")?"general":parts[i].equals("Draws")?"draws":null;
            if(name!=null)parts[i]=DetectorConfigText.text("pixelmondetector.config.category."+name);
        }
        return new StringTextComponent(String.join(" > ",parts));
    }
}
