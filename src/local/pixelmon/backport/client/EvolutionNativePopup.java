package local.pixelmon.backport.client;
import com.pixelmonmod.pixelmon.client.gui.widgets.PokeCheckerButton;
import local.pixelmon.backport.mixin.EvolutionMiniButtonAccess;
/** Add one native package-private row, retaining the three original row objects. */
public final class EvolutionNativePopup {
    private EvolutionNativePopup(){}
    public static EvolutionMiniButtonAccess[] rows(PokeCheckerButton popup){
        try{
            var rows=new EvolutionMiniButtonAccess[4];String[] fields={"summary","moves","stats"};
            for(int i=0;i<3;i++){var field=PokeCheckerButton.class.getDeclaredField(fields[i]);field.setAccessible(true);rows[i]=(EvolutionMiniButtonAccess)field.get(popup);}
            var nativeRow=Class.forName("com.pixelmonmod.pixelmon.client.gui.widgets.PokeCheckerButton$MiniButton",true,PokeCheckerButton.class.getClassLoader());
            var constructor=nativeRow.getDeclaredConstructor(PokeCheckerButton.class,int.class,String.class,int.class,int.class);constructor.setAccessible(true);
            rows[3]=(EvolutionMiniButtonAccess)constructor.newInstance(popup,3,EvolutionGuideText.tr("tab"),10,72);return rows;
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("Native PokeChecker popup row layout changed",failure);}
    }
}
