package local.pixelmon.backport.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.BattleBond;
import com.pixelmonmod.pixelmon.battles.attacks.ImmutableAttack;
import local.pixelmon.backport.PermanentAsh;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.text.*;

/** Context-specific summary text; never changes the shared move definition or battle state. */
public final class AshMoveDisplay {
    private AshMoveDisplay(){}
    public static boolean eligible(Pokemon pokemon,ImmutableAttack move){
        return pokemon!=null && pokemon.getSpecies().getDex()==658 && PermanentAsh.isAsh(pokemon.getForm())
            && move!=null && "Water Shuriken".equals(move.getAttackName());
    }
    public static String description(String locale){
        if("zh_cn".equals(locale))return "优先攻击，固定3击，每击基础威力20。使出前强化自身：水系威力提升50%，持续5回合，包含当回合。重复只刷新，不叠加，并给对方附着蚀。";
        if("en_us".equals(locale))return "Strikes first, exactly 3 times at 20 base power each. Before attacking, strengthens itself: Water power +50% for 5 turns including this turn. Reuse refreshes without stacking. Also inflicts Erosion on the target.";
        return null;
    }
    public static String message(String key,String locale){
        if("pixelmon_backport.battle.ash_water_boost".equals(key)){
            if("zh_cn".equals(locale))return "%s强化了自身！水系攻击威力提升50%，持续五回合！";
            if("en_us".equals(locale))return "%s strengthened itself! Its Water attacks gain 50% power for five turns!";
        }
        if("pixelmon_backport.battle.ash_water_boost_end".equals(key)){
            if("zh_cn".equals(locale))return "%s的水系伤害加成结束了！";
            if("en_us".equals(locale))return "%s's Water damage boost has ended!";
        }
        return null;
    }
    public static ITextComponent battleMessage(ITextComponent original,String locale){
        if(!(original instanceof TranslationTextComponent))return original;
        TranslationTextComponent translated=(TranslationTextComponent)original;
        String pattern=message(translated.func_150268_i(),locale);
        if(pattern==null)return original;
        Object[] args=translated.func_150271_j().clone();
        for(int i=0;i<args.length;i++)if(args[i] instanceof ITextComponent)args[i]=((ITextComponent)args[i]).getString();
        // These two owned messages have one name placeholder; literal percentages are not format specifiers.
        String name=args.length==0?"":String.valueOf(args[0]);
        IFormattableTextComponent result=new StringTextComponent(pattern.replace("%s",name)).func_230530_a_(original.func_150256_b());
        for(ITextComponent sibling:original.func_150253_a())result.func_230529_a_(sibling.func_230532_e_());
        return result;
    }
    public static void drawDescription(MatrixStack matrix,String text,int x,int y,int width,int color){
        FontRenderer font=Minecraft.func_71410_x().field_71466_p;
        NpcDialogueLayout.Layout layout=NpcDialogueLayout.layout(font,text,width);
        matrix.func_227860_a_();
        try{
            matrix.func_227861_a_(x,y,0);matrix.func_227862_a_(layout.scale,layout.scale,1);
            for(int i=0;i<layout.lines.size();i++)font.func_238422_b_(matrix,layout.lines.get(i),0,i*layout.rowHeight,color);
        }finally{matrix.func_227865_b_();}
    }
}
