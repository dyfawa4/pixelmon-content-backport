package local.pixelmon.backport.client;
import com.pixelmonmod.pixelmon.entities.pixelmon.AbstractBaseEntity;

/** Pixelmon inherits AbstractHorseEntity, but does not register horse.jump_strength. */
public final class JadePixelmonCompat {
    private JadePixelmonCompat(){}
    public static boolean decorativeOrPokemon(Object accessor){
        try{return Class.forName("mcp.mobius.waila.api.IEntityAccessor").getMethod("getEntity").invoke(accessor) instanceof AbstractBaseEntity;}
        catch(ReflectiveOperationException failure){throw new IllegalStateException("Jade entity accessor unavailable",failure);}
    }
}
