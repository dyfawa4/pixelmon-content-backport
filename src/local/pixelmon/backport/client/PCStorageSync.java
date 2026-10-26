package local.pixelmon.backport.client;

import com.pixelmonmod.pixelmon.api.storage.*;
import com.pixelmonmod.pixelmon.client.storage.ClientStorageManager;
import local.pixelmon.backport.mixin.PCStorageAccess;

/** Initialization carries metadata only. Keep the object observed by the PC screen. */
public final class PCStorageSync {
    private PCStorageSync() {}
    public static PCStorage initialize(PCStorage incoming) {
        PCStorage current=ClientStorageManager.openPC;
        if(current==null || !current.uuid.equals(incoming.uuid))current=ClientStorageManager.pcs.get(incoming.uuid);
        if(current==null)return incoming;
        int size=incoming.getBoxCount();
        if(current.getBoxCount()!=size){
            PCBox[] boxes=java.util.Arrays.copyOf(current.getBoxes(),size);
            for(int i=0;i<size;i++)if(boxes[i]==null)boxes[i]=new PCBox(current,i);
            ((PCStorageAccess)(Object)current).backport$boxes(boxes);
        }
        current.shouldSendUpdates=false;
        current.setLastBox(incoming.getLastBox());
        for(int i=0;i<size;i++){
            PCBox box=current.getBox(i), metadata=incoming.getBox(i);
            box.setName(metadata.getName());box.setWallpaper(metadata.getWallpaper());
            box.hasChangedClientSide=false;
        }
        return current;
    }
    /** Run only after the native handler installs the storage into the map. */
    public static void initialized(PCStorage storage){
        int size=storage.getBoxCount(),visible=storage.getLastBox();
        if(visible>=0&&visible<size)StoragePacketGuard.recover(storage.uuid,new StoragePosition(visible,0));
        for(int i=0;i<size;i++)if(i!=visible)StoragePacketGuard.recover(storage.uuid,new StoragePosition(i,0));
    }
}
