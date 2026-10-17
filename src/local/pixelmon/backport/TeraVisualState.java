package local.pixelmon.backport;

import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;

/** Ephemeral entity metadata, sent by vanilla tracking to every nearby client.
 * No client-side battle snapshot, persisted NBT or scale is used for visuals. */
public interface TeraVisualState {
    int backport$teraVisualType();
    void backport$setTeraVisualType(int type);
    long backport$teraVisualStart();
    void backport$setTeraVisualStart(long tick);

    static int authoritativeType(PixelmonEntity entity){return Tera.visualType(entity);}
    static boolean visible(boolean active,boolean simulated,boolean ended,boolean onField,boolean alive){
        return active && !simulated && !ended && onField && alive;
    }
}
