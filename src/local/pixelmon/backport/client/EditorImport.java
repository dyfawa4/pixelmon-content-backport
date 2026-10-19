package local.pixelmon.backport.client;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import java.util.List;

/** Locate only the individual being edited, never a default/first party slot. */
public final class EditorImport {
    private EditorImport(){}
    public static int slot(Pokemon current,List<Pokemon> edited){
        if(current==null||edited==null)return -1;
        for(int i=0;i<Math.min(6,edited.size());i++){
            Pokemon entry=edited.get(i);
            if(entry!=null&&(entry==current||entry.getUUID().equals(current.getUUID())))return i;
        }
        return -1;
    }
}
