package local.pixelmon.backport;
import net.minecraft.util.*;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.*;
public final class BackportSounds {
 public static final ResourceLocation GYM_ID=new ResourceLocation(Content.NS,"music.battle.gym_leader");
 public static final DeferredRegister<SoundEvent> SOUNDS=DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,Content.NS);
 public static final RegistryObject<SoundEvent> GYM_LEADER=SOUNDS.register(GYM_ID.func_110623_a(),()->new SoundEvent(GYM_ID));
 public static final ResourceLocation WILD_ID=new ResourceLocation(Content.NS,"music.battle.wild");
 public static final RegistryObject<SoundEvent> WILD=SOUNDS.register(WILD_ID.func_110623_a(),()->new SoundEvent(WILD_ID));
 public static final ResourceLocation WILD_TWO_ID=new ResourceLocation(Content.NS,"music.battle.wild_two");
 public static final RegistryObject<SoundEvent> WILD_TWO=SOUNDS.register(WILD_TWO_ID.func_110623_a(),()->new SoundEvent(WILD_TWO_ID));
 public static final ResourceLocation RAID_DISC_ID=new ResourceLocation(Content.NS,"music.disc.max_raid");
 public static final RegistryObject<SoundEvent> RAID_DISC=SOUNDS.register(RAID_DISC_ID.func_110623_a(),()->new SoundEvent(RAID_DISC_ID));
 public static final ResourceLocation TERA_ID=new ResourceLocation(Content.NS,"music.battle.tera_raid");
 public static final RegistryObject<SoundEvent> TERA_RAID=SOUNDS.register(TERA_ID.func_110623_a(),()->new SoundEvent(TERA_ID));
 /** Native track indices are -1 or non-negative. This packet-local marker distinguishes Tera raids. */
 public static final int TERA_TRACK=-170;
 public static final int WILD_ONE_TRACK=-171,WILD_TWO_TRACK=-172;
 public static final int MAX_RAID_TRACK=-173;
 private BackportSounds(){}
}
