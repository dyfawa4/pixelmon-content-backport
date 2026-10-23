package local.pixelmon.backport.mixin;
import local.pixelmon.backport.BackportSounds;
import net.minecraft.item.MusicDiscItem;
import net.minecraft.util.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(MusicDiscItem.class)
public abstract class BossDiscSoundMixin {
 @Inject(method="func_185075_h",at=@At("HEAD"),cancellable=true,require=1)
 private void backport$bossRecord(CallbackInfoReturnable<SoundEvent> cir){
  if(new ResourceLocation("pixelmon:boss_music_disc").equals(((MusicDiscItem)(Object)this).getRegistryName()))cir.setReturnValue(BackportSounds.GYM_LEADER.get());
  if(new ResourceLocation("pixelmon:wild_music_disc").equals(((MusicDiscItem)(Object)this).getRegistryName()))cir.setReturnValue(BackportSounds.WILD.get());
  if(new ResourceLocation("pixelmon:raid_music_disc").equals(((MusicDiscItem)(Object)this).getRegistryName()))cir.setReturnValue(BackportSounds.RAID_DISC.get());
 }
 @Inject(method="func_185074_a",at=@At("HEAD"),cancellable=true,require=1)
 private static void backport$recordLookup(SoundEvent sound,CallbackInfoReturnable<MusicDiscItem> cir){
  if(sound==null)return;
  if(BackportSounds.TERA_ID.equals(sound.getRegistryName())){cir.setReturnValue((MusicDiscItem)local.pixelmon.backport.Backport.item("tera_raid_music_disc"));return;}
  if(BackportSounds.WILD_TWO_ID.equals(sound.getRegistryName())){cir.setReturnValue((MusicDiscItem)local.pixelmon.backport.Backport.item("wild_music_disc_two"));return;}
  String record=BackportSounds.GYM_ID.equals(sound.getRegistryName())?"boss_music_disc":BackportSounds.WILD_ID.equals(sound.getRegistryName())?"wild_music_disc":BackportSounds.RAID_DISC_ID.equals(sound.getRegistryName())?"raid_music_disc":null;
  if(record!=null){var item=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new ResourceLocation("pixelmon",record));if(item instanceof MusicDiscItem)cir.setReturnValue((MusicDiscItem)item);}
 }
}
