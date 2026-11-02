package local.pixelmon.backport;
import com.pixelmonmod.pixelmon.items.heldItems.*;
import com.pixelmonmod.pixelmon.enums.heldItems.EnumHeldItems;
import com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType;
import com.pixelmonmod.pixelmon.api.pokemon.Element;
import com.pixelmonmod.pixelmon.api.battles.Terrain;
import net.minecraft.item.*;
/** Independently registered items; native effect classes preserve exact conditions and dispatch. */
public final class EndlessRegisteredItems {
 public static final class AbsorbBulb extends RaiseStatItem implements EndlessHeldItems.Endless {
  public AbsorbBulb(){super(EnumHeldItems.absorbbulb,BattleStatsType.SPECIAL_ATTACK,Element.WATER);}
  @Override public boolean func_77636_d(ItemStack stack){return true;}
  @Override public void func_150895_a(ItemGroup group,net.minecraft.util.NonNullList<ItemStack> output){}
 }
 public static final class AirBalloon extends AirBalloonItem implements EndlessHeldItems.Endless {
  public AirBalloon(){super();}
  @Override public boolean func_77636_d(ItemStack stack){return true;}
  @Override public void func_150895_a(ItemGroup group,net.minecraft.util.NonNullList<ItemStack> output){}
 }
 public static final class BerryJuice extends BerryJuiceItem implements EndlessHeldItems.Endless {
  public BerryJuice(){super(new Item.Properties());}
  @Override public boolean func_77636_d(ItemStack stack){return true;}
  @Override public void func_150895_a(ItemGroup group,net.minecraft.util.NonNullList<ItemStack> output){}
 }
 public static final class BlunderPolicy extends BlunderPolicyItem implements EndlessHeldItems.Endless {
  public BlunderPolicy(){super();}
  @Override public boolean func_77636_d(ItemStack stack){return true;}
  @Override public void func_150895_a(ItemGroup group,net.minecraft.util.NonNullList<ItemStack> output){}
 }
 public static final class BoosterEnergy extends BoosterEnergyItem implements EndlessHeldItems.Endless {
  public BoosterEnergy(){super();}
  @Override public boolean func_77636_d(ItemStack stack){return true;}
  @Override public void func_150895_a(ItemGroup group,net.minecraft.util.NonNullList<ItemStack> output){}
 }
 public static final class CellBattery extends RaiseStatItem implements EndlessHeldItems.Endless {
  public CellBattery(){super(EnumHeldItems.cellbattery,BattleStatsType.ATTACK,Element.ELECTRIC);}
  @Override public boolean func_77636_d(ItemStack stack){return true;}
  @Override public void func_150895_a(ItemGroup group,net.minecraft.util.NonNullList<ItemStack> output){}
 }
 public static final class EjectButton extends EjectButtonItem implements EndlessHeldItems.Endless {
  public EjectButton(){super();}
  @Override public boolean func_77636_d(ItemStack stack){return true;}
  @Override public void func_150895_a(ItemGroup group,net.minecraft.util.NonNullList<ItemStack> output){}
 }
 public static final class ElectricSeed extends TerrainSeedItem implements EndlessHeldItems.Endless {
  public ElectricSeed(){super(Terrain.ELECTRIC,BattleStatsType.DEFENSE);}
  @Override public boolean func_77636_d(ItemStack stack){return true;}
  @Override public void func_150895_a(ItemGroup group,net.minecraft.util.NonNullList<ItemStack> output){}
 }
 public static final class FocusSash extends FocusSashItem implements EndlessHeldItems.Endless {
  public FocusSash(){super();}
  @Override public boolean func_77636_d(ItemStack stack){return true;}
  @Override public void func_150895_a(ItemGroup group,net.minecraft.util.NonNullList<ItemStack> output){}
 }
 public static final class GrassySeed extends TerrainSeedItem implements EndlessHeldItems.Endless {
  public GrassySeed(){super(Terrain.GRASSY,BattleStatsType.DEFENSE);}
  @Override public boolean func_77636_d(ItemStack stack){return true;}
  @Override public void func_150895_a(ItemGroup group,net.minecraft.util.NonNullList<ItemStack> output){}
 }
 public static final class LuminousMoss extends RaiseStatItem implements EndlessHeldItems.Endless {
  public LuminousMoss(){super(EnumHeldItems.luminousMoss,BattleStatsType.SPECIAL_DEFENSE,Element.WATER);}
  @Override public boolean func_77636_d(ItemStack stack){return true;}
  @Override public void func_150895_a(ItemGroup group,net.minecraft.util.NonNullList<ItemStack> output){}
 }
 public static final class MentalHerb extends MentalHerbItem implements EndlessHeldItems.Endless {
  public MentalHerb(){super();}
  @Override public boolean func_77636_d(ItemStack stack){return true;}
  @Override public void func_150895_a(ItemGroup group,net.minecraft.util.NonNullList<ItemStack> output){}
 }
 public static final class MirrorHerb extends MirrorHerbItem implements EndlessHeldItems.Endless {
  public MirrorHerb(){super();}
  @Override public boolean func_77636_d(ItemStack stack){return true;}
  @Override public void func_150895_a(ItemGroup group,net.minecraft.util.NonNullList<ItemStack> output){}
 }
 public static final class MistySeed extends TerrainSeedItem implements EndlessHeldItems.Endless {
  public MistySeed(){super(Terrain.MISTY,BattleStatsType.SPECIAL_DEFENSE);}
  @Override public boolean func_77636_d(ItemStack stack){return true;}
  @Override public void func_150895_a(ItemGroup group,net.minecraft.util.NonNullList<ItemStack> output){}
 }
 public static final class PowerHerb extends PowerHerbItem implements EndlessHeldItems.Endless {
  public PowerHerb(){super();}
  @Override public boolean func_77636_d(ItemStack stack){return true;}
  @Override public void func_150895_a(ItemGroup group,net.minecraft.util.NonNullList<ItemStack> output){}
 }
 public static final class PsychicSeed extends TerrainSeedItem implements EndlessHeldItems.Endless {
  public PsychicSeed(){super(Terrain.PSYCHIC,BattleStatsType.SPECIAL_DEFENSE);}
  @Override public boolean func_77636_d(ItemStack stack){return true;}
  @Override public void func_150895_a(ItemGroup group,net.minecraft.util.NonNullList<ItemStack> output){}
 }
 public static final class RedCard extends RedCardItem implements EndlessHeldItems.Endless {
  public RedCard(){super();}
  @Override public boolean func_77636_d(ItemStack stack){return true;}
  @Override public void func_150895_a(ItemGroup group,net.minecraft.util.NonNullList<ItemStack> output){}
 }
 public static final class RoomService extends RoomServiceItem implements EndlessHeldItems.Endless {
  public RoomService(){super();}
  @Override public boolean func_77636_d(ItemStack stack){return true;}
  @Override public void func_150895_a(ItemGroup group,net.minecraft.util.NonNullList<ItemStack> output){}
 }
 public static final class Snowball extends RaiseStatItem implements EndlessHeldItems.Endless {
  public Snowball(){super(EnumHeldItems.snowball,BattleStatsType.ATTACK,Element.ICE);}
  @Override public boolean func_77636_d(ItemStack stack){return true;}
  @Override public void func_150895_a(ItemGroup group,net.minecraft.util.NonNullList<ItemStack> output){}
 }
 public static final class ThroatSpray extends ThroatSprayItem implements EndlessHeldItems.Endless {
  public ThroatSpray(){super();}
  @Override public boolean func_77636_d(ItemStack stack){return true;}
  @Override public void func_150895_a(ItemGroup group,net.minecraft.util.NonNullList<ItemStack> output){}
 }
 public static final class WeaknessPolicy extends WeaknessPolicyItem implements EndlessHeldItems.Endless {
  public WeaknessPolicy(){super();}
  @Override public boolean func_77636_d(ItemStack stack){return true;}
  @Override public void func_150895_a(ItemGroup group,net.minecraft.util.NonNullList<ItemStack> output){}
 }
 public static final class WhiteHerb extends WhiteHerbItem implements EndlessHeldItems.Endless {
  public WhiteHerb(){super();}
  @Override public boolean func_77636_d(ItemStack stack){return true;}
  @Override public void func_150895_a(ItemGroup group,net.minecraft.util.NonNullList<ItemStack> output){}
 }
 public static final class EjectPack extends local.pixelmon.backport.EjectPack implements EndlessHeldItems.Endless {
  public EjectPack(){super();}
  @Override public boolean func_77636_d(ItemStack stack){return true;}
  @Override public void func_150895_a(ItemGroup group,net.minecraft.util.NonNullList<ItemStack> output){}
 }
 public static Item create(String name){switch(name){
 case "absorb_bulb":return new AbsorbBulb();
 case "air_balloon":return new AirBalloon();
 case "berry_juice":return new BerryJuice();
 case "blunder_policy":return new BlunderPolicy();
 case "booster_energy":return new BoosterEnergy();
 case "cell_battery":return new CellBattery();
 case "eject_button":return new EjectButton();
 case "electric_seed":return new ElectricSeed();
 case "focus_sash":return new FocusSash();
 case "grassy_seed":return new GrassySeed();
 case "luminous_moss":return new LuminousMoss();
 case "mental_herb":return new MentalHerb();
 case "mirror_herb":return new MirrorHerb();
 case "misty_seed":return new MistySeed();
 case "power_herb":return new PowerHerb();
 case "psychic_seed":return new PsychicSeed();
 case "red_card":return new RedCard();
 case "room_service":return new RoomService();
 case "snowball":return new Snowball();
 case "throat_spray":return new ThroatSpray();
 case "weakness_policy":return new WeaknessPolicy();
 case "white_herb":return new WhiteHerb();
 case "eject_pack":return new EjectPack();
 default:throw new IllegalArgumentException(name);}}
}
