package local.pixelmon.backport;
import com.pixelmonmod.pixelmon.entities.DenEntity;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.battles.raids.RaidData;
import net.minecraft.entity.EntityType;
import net.minecraft.world.World;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.*;
import net.minecraftforge.fml.network.NetworkHooks;

/** Separate saved entity, sharing only native lobby and detector ancestry. */
public final class TeraDenEntity extends DenEntity {
    private static final DataParameter<Integer> TYPE=EntityDataManager.func_187226_a(TeraDenEntity.class,DataSerializers.field_187192_b);
    private static final DataParameter<String> PALETTE=EntityDataManager.func_187226_a(TeraDenEntity.class,DataSerializers.field_187194_d);
    private int refreshed=-1;
    public TeraDenEntity(EntityType<DenEntity> type,World world){super(type,world);setDefaults();setSize(1.4f,2.1f);field_70158_ak=true;setBaseHidden(true);func_189654_d(true);getPersistentData().func_74768_a("BackportDenKind",1);}
    @Override public void func_213352_e(Vector3d travel){func_213317_d(Vector3d.field_186680_a);}
    public TeraDenEntity(World world){this(Backport.TERA_DEN.get(),world);}
    @Override public boolean isBaseHidden(){return true;}
    @Override public boolean func_70097_a(net.minecraft.util.DamageSource source,float amount){
        if(source.func_76364_f() instanceof PlayerEntity){
            PlayerEntity player=(PlayerEntity)source.func_76364_f();
            net.minecraft.item.Item tool=player.func_184614_ca().func_77973_b();
            if(tool==com.pixelmonmod.pixelmon.api.registries.PixelmonItems.den_spawner)return false;
            if(tool instanceof TeraDenItem){
                if(!player.func_184812_l_() || com.pixelmonmod.pixelmon.Pixelmon.EVENT_BUS.post(new com.pixelmonmod.pixelmon.api.events.raids.DenEvent.Interact(this,player,false)))return false;
                if(!field_70170_p.field_72995_K){
                    if(com.pixelmonmod.pixelmon.items.SpawnDenItem.isOPOnly && !com.pixelmonmod.pixelmon.api.util.helpers.PlayerHelper.hasPermission((net.minecraft.entity.player.ServerPlayerEntity)player,"pixelmon.denspawner.use"))return false;
                    field_70170_p.func_184133_a(null,func_233580_cy_(),net.minecraft.util.SoundEvents.field_187835_fT,net.minecraft.util.SoundCategory.BLOCKS,1f,.5f);
                    func_241204_bJ_();
                }
                return true;
            }
        }
        return super.func_70097_a(source,amount);
    }
    @Override protected void func_70088_a(){super.func_70088_a();field_70180_af.func_187214_a(TYPE,0);field_70180_af.func_187214_a(PALETTE,"none");}
    public int teraType(){return field_70180_af.func_187225_a(TYPE);}
    public String palette(){return field_70180_af.func_187225_a(PALETTE);}
    public void configure(Pokemon pokemon,int stars,int teraType,int day){
        if(stars<1 || stars>6)throw new IllegalArgumentException("Den stars");
        refreshed=day;setLastRollDay(day);type(teraType);field_70180_af.func_187227_b(PALETTE,pokemon.getPalette().getName());
        setData(new RaidData(func_145782_y(),stars,pokemon.getSpecies(),pokemon.getForm()));
    }
    public void type(int type){if(type<0 || type>=19)throw new IllegalArgumentException("Den type");field_70180_af.func_187227_b(TYPE,type);int c=TeraRaidClientColors.COLORS[type];setColorRGBA(0xff000000|c);setColorUIA(c);setColorUIB(c);setColorUIC(0x66000000|c);}
    @Override public void roll(int day){
        setLastRollDay(day);if(field_70170_p.field_72995_K || refreshed==day || getData().map(RaidData::hasPlayers).orElse(false))return;
        refreshed=day;int r=field_70146_Z.nextInt(100),stars=r<20?1:r<40?2:r<60?3:r<80?4:r<95?5:6;
        Pokemon p=TeraRaids.encounter(TeraRaids.pick(stars,field_70146_Z));type(field_70146_Z.nextInt(18));field_70180_af.func_187227_b(PALETTE,p.getPalette().getName());
        setData(new RaidData(func_145782_y(),stars,p.getSpecies(),p.getForm()));
    }
    @Override public void func_213281_b(CompoundNBT nbt){super.func_213281_b(nbt);nbt.func_74768_a("BackportTeraType",teraType());nbt.func_74778_a("BackportTeraPalette",palette());nbt.func_74768_a("BackportTeraDay",refreshed);if(getData().isPresent()){RaidData data=getServerData();CompoundNBT raid=new CompoundNBT();raid.func_74768_a("Species",data.getSpecies().getDex());raid.func_74778_a("Form",data.getForm().getName());raid.func_74768_a("Stars",data.getStars());nbt.func_218657_a("BackportTeraRaid",raid);}}
    @Override public void func_70037_a(CompoundNBT nbt){super.func_70037_a(nbt);setBaseHidden(true);getPersistentData().func_74768_a("BackportDenKind",1);type(Math.max(0,Math.min(18,nbt.func_74762_e("BackportTeraType"))));refreshed=nbt.func_74762_e("BackportTeraDay");String palette=nbt.func_74779_i("BackportTeraPalette");field_70180_af.func_187227_b(PALETTE,palette.isEmpty()?"none":palette);if(nbt.func_74764_b("BackportTeraRaid")){CompoundNBT saved=nbt.func_74775_l("BackportTeraRaid");com.pixelmonmod.pixelmon.api.pokemon.species.Species species=PixelmonSpecies.fromNationalDex(saved.func_74762_e("Species"));setData(new RaidData(func_145782_y(),Math.max(1,Math.min(6,saved.func_74762_e("Stars"))),species,species.getForm(saved.func_74779_i("Form"))));}}
    @Override public ActionResultType func_184199_a(PlayerEntity player,Vector3d hit,Hand hand){
        if(!field_70170_p.field_72995_K && hand==Hand.MAIN_HAND)player.func_146105_b(new TranslationTextComponent("pixelmon.backport.tera.raid.instructions"),false);
        if(!getData().isPresent()){if(!field_70170_p.field_72995_K)player.func_146105_b(new TranslationTextComponent("pixelmon.backport.tera.raid.empty"),true);return ActionResultType.SUCCESS;}
        return super.func_184199_a(player,hit,hand);
    }
    @Override public IPacket<?> func_213297_N(){return NetworkHooks.getEntitySpawningPacket(this);}
}
