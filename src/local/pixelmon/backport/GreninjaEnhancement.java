package local.pixelmon.backport;
/** Explicit optional companion: ordinary installs use the reduced signature effects. */
public final class GreninjaEnhancement {
 public static final String MOD_ID="ohmygod_greninja";
 private GreninjaEnhancement(){}
 public static boolean enabled(){return net.minecraftforge.fml.ModList.get()!=null&&net.minecraftforge.fml.ModList.get().isLoaded(MOD_ID);}
 public static String trainingDescription(String code){
  if("zh_cn".equals(code))return enabled()?"大幅提高自身特攻，使下一回合招式优先度提高1级，并获得一次额外行动。额外行动中不能再次使用本招式，仅推进自身的持续效果。":"大幅提高自身特攻，并使下一回合招式优先度提高1级。";
  if("en_us".equals(code))return enabled()?"Sharply raises the user's Sp. Atk, raises move priority by 1 next turn, and grants an extra action. Cannot be used during the extra action. Only the user's own effects advance.":"Sharply raises the user's Sp. Atk and raises move priority by 1 for the next turn.";
  return null;
 }
}
